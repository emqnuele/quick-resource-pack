#!/usr/bin/env python3

from __future__ import annotations

import argparse
import csv
import io
import json
import mimetypes
import os
import re
import sys
import time
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Sequence, Tuple
from urllib import error, parse, request

API_BASE = "https://api.modrinth.com/v2"
DEFAULT_USER_AGENT = "quick-resource-pack-release-uploader/1.0 (+https://github.com/emqnuele/quick-resource-pack)"
VALID_DEPENDENCY_TYPES = {"required", "optional", "incompatible", "embedded"}


class UploadError(RuntimeError):
    pass


@dataclass(frozen=True)
class ManifestEntry:
    file_relative: str
    file_path: Path
    version_number: str
    game_versions: List[str]
    loaders: List[str]


@dataclass(frozen=True)
class ReleaseNote:
    title: str
    changelog: str


def parse_list_value(raw: str) -> List[str]:
    return [item.strip() for item in re.split(r"[;,]", raw) if item.strip()]


def read_manifest(manifest_path: Path) -> List[ManifestEntry]:
    if not manifest_path.is_file():
        raise UploadError(f"manifest not found: {manifest_path}")

    with manifest_path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        fieldnames = set(reader.fieldnames or [])
        required_fields = {"file", "version_number", "game_versions", "loaders"}
        missing = sorted(required_fields - fieldnames)
        if missing:
            raise UploadError(f"manifest missing columns: {', '.join(missing)}")

        entries: List[ManifestEntry] = []
        for line_number, row in enumerate(reader, start=2):
            file_relative = (row.get("file") or "").strip()
            version_number = (row.get("version_number") or "").strip()
            game_versions = parse_list_value((row.get("game_versions") or "").strip())
            loaders = parse_list_value((row.get("loaders") or "").strip())

            if not file_relative or not version_number or not game_versions or not loaders:
                raise UploadError(f"invalid manifest row at line {line_number}")

            file_path = (manifest_path.parent / file_relative).resolve()
            entries.append(
                ManifestEntry(
                    file_relative=file_relative,
                    file_path=file_path,
                    version_number=version_number,
                    game_versions=game_versions,
                    loaders=loaders,
                )
            )

    if not entries:
        raise UploadError("manifest has no entries")

    return entries


def extract_changelog(block: str) -> str:
    changelog_header = re.search(r"^###\s+Changelog\s*$", block, flags=re.MULTILINE)
    if changelog_header:
        text = block[changelog_header.end():]
    else:
        text = block

    return text.strip()


def parse_notes_new_format(text: str) -> Dict[str, ReleaseNote]:
    notes: Dict[str, ReleaseNote] = {}
    matches = list(
        re.finditer(
            r"^###\s+(?P<title>.+\(MC\s+(?P<mc>\d+\.\d+\.\d+)\))\s*$",
            text,
            flags=re.MULTILINE,
        )
    )

    for index, match in enumerate(matches):
        start = match.end()
        end = matches[index + 1].start() if index + 1 < len(matches) else len(text)
        block = text[start:end]
        mc_version = match.group("mc")
        title = match.group("title").strip()
        changelog = extract_changelog(block)
        if changelog:
            notes[mc_version] = ReleaseNote(title=title, changelog=changelog)

    return notes


def parse_notes_legacy_format(text: str) -> Dict[str, ReleaseNote]:
    notes: Dict[str, ReleaseNote] = {}
    section_matches = list(
        re.finditer(r"^##\s+MC\s+(?P<mc>\d+\.\d+\.\d+)\s*$", text, flags=re.MULTILINE)
    )

    for index, match in enumerate(section_matches):
        start = match.end()
        end = section_matches[index + 1].start() if index + 1 < len(section_matches) else len(text)
        block = text[start:end]
        mc_version = match.group("mc")

        title = f"Quick Resource Pack (MC {mc_version})"
        title_header = re.search(r"^###\s+Version Title\s*$", block, flags=re.MULTILINE)
        if title_header:
            remaining = block[title_header.end():]
            for line in remaining.splitlines():
                candidate = line.strip()
                if candidate:
                    title = candidate
                    break

        changelog = extract_changelog(block)
        if changelog:
            notes[mc_version] = ReleaseNote(title=title, changelog=changelog)

    return notes


def read_release_notes(notes_path: Path) -> Dict[str, ReleaseNote]:
    if not notes_path.is_file():
        raise UploadError(f"release notes not found: {notes_path}")

    text = notes_path.read_text(encoding="utf-8")
    notes = parse_notes_new_format(text)
    if not notes:
        notes = parse_notes_legacy_format(text)

    if not notes:
        raise UploadError(
            "unable to parse release notes; expected sections like '### Quick Resource Pack ... (MC x.y.z)'"
        )

    return notes


def default_title(version_number: str, game_version: str) -> str:
    suffix = f"-mc{game_version}"
    base = version_number[:-len(suffix)] if version_number.endswith(suffix) else version_number
    return f"Quick Resource Pack {base} (MC {game_version})"


def api_json_request(
    method: str,
    path: str,
    *,
    user_agent: str,
    token: str | None = None,
    data: bytes | None = None,
    content_type: str | None = None,
    timeout: int = 120,
) -> dict | list:
    headers = {
        "Accept": "application/json",
        "User-Agent": user_agent,
    }
    if token:
        headers["Authorization"] = token
    if content_type:
        headers["Content-Type"] = content_type

    req = request.Request(
        url=f"{API_BASE}{path}",
        data=data,
        headers=headers,
        method=method,
    )

    try:
        with request.urlopen(req, timeout=timeout) as response:
            payload = response.read().decode("utf-8")
    except error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise UploadError(f"{method} {path} failed ({exc.code}): {body}") from exc
    except error.URLError as exc:
        raise UploadError(f"{method} {path} failed: {exc.reason}") from exc

    if not payload:
        return {}

    try:
        return json.loads(payload)
    except json.JSONDecodeError as exc:
        raise UploadError(f"invalid json response for {method} {path}: {payload}") from exc


def encode_multipart(fields: Dict[str, str], files: Sequence[Tuple[str, str, bytes, str]]) -> Tuple[bytes, str]:
    boundary = f"----modrinth-{uuid.uuid4().hex}"
    buffer = io.BytesIO()

    for name, value in fields.items():
        buffer.write(f"--{boundary}\r\n".encode("utf-8"))
        buffer.write(f'Content-Disposition: form-data; name="{name}"\r\n\r\n'.encode("utf-8"))
        buffer.write(value.encode("utf-8"))
        buffer.write(b"\r\n")

    for field_name, file_name, file_content, content_type in files:
        buffer.write(f"--{boundary}\r\n".encode("utf-8"))
        buffer.write(
            f'Content-Disposition: form-data; name="{field_name}"; filename="{file_name}"\r\n'.encode("utf-8")
        )
        buffer.write(f"Content-Type: {content_type}\r\n\r\n".encode("utf-8"))
        buffer.write(file_content)
        buffer.write(b"\r\n")

    buffer.write(f"--{boundary}--\r\n".encode("utf-8"))
    return buffer.getvalue(), f"multipart/form-data; boundary={boundary}"


def resolve_project_id(project_ref: str, *, user_agent: str, token: str | None) -> str:
    payload = api_json_request(
        "GET",
        f"/project/{parse.quote(project_ref, safe='')}",
        user_agent=user_agent,
        token=token,
    )
    project_id = payload.get("id") if isinstance(payload, dict) else None
    if not project_id:
        raise UploadError(f"unable to resolve project id for '{project_ref}'")
    return project_id


def parse_dependency_specs(specs: Iterable[str]) -> List[Tuple[str, str]]:
    parsed: List[Tuple[str, str]] = []
    for raw in specs:
        spec = raw.strip()
        if not spec:
            continue

        if ":" in spec:
            project_ref, dep_type = spec.split(":", 1)
        else:
            project_ref, dep_type = spec, "required"

        project_ref = project_ref.strip()
        dep_type = dep_type.strip().lower()
        if not project_ref:
            raise UploadError(f"invalid dependency spec: '{raw}'")
        if dep_type not in VALID_DEPENDENCY_TYPES:
            valid = ", ".join(sorted(VALID_DEPENDENCY_TYPES))
            raise UploadError(f"invalid dependency type '{dep_type}' in '{raw}' (valid: {valid})")

        parsed.append((project_ref, dep_type))

    return parsed


def build_dependencies(
    specs: Iterable[Tuple[str, str]],
    *,
    user_agent: str,
    token: str | None,
) -> List[dict]:
    cache: Dict[str, str] = {}
    dependencies: List[dict] = []

    for project_ref, dep_type in specs:
        if project_ref not in cache:
            cache[project_ref] = resolve_project_id(project_ref, user_agent=user_agent, token=token)
        dependencies.append(
            {
                "project_id": cache[project_ref],
                "dependency_type": dep_type,
            }
        )

    return dependencies


def fetch_existing_versions(project_ref: str, *, user_agent: str, token: str | None) -> set[str]:
    payload = api_json_request(
        "GET",
        f"/project/{parse.quote(project_ref, safe='')}/version",
        user_agent=user_agent,
        token=token,
    )

    existing: set[str] = set()
    if isinstance(payload, list):
        for item in payload:
            if isinstance(item, dict):
                version_number = item.get("version_number")
                if isinstance(version_number, str) and version_number:
                    existing.add(version_number)
    return existing


def select_entries(entries: Sequence[ManifestEntry], only_targets: Sequence[str]) -> List[ManifestEntry]:
    if not only_targets:
        return list(entries)

    wanted = set(only_targets)
    selected = [entry for entry in entries if any(game in wanted for game in entry.game_versions)]
    if not selected:
        raise UploadError(f"no manifest entries matched --only values: {', '.join(only_targets)}")
    return selected


def upload_entry(
    *,
    entry: ManifestEntry,
    note: ReleaseNote,
    project_id: str,
    dependencies: List[dict],
    version_type: str,
    featured: bool,
    user_agent: str,
    token: str,
    dry_run: bool,
) -> str:
    if not entry.file_path.is_file():
        raise UploadError(f"artifact not found: {entry.file_path}")

    payload = {
        "name": note.title,
        "version_number": entry.version_number,
        "changelog": note.changelog,
        "dependencies": dependencies,
        "game_versions": entry.game_versions,
        "version_type": version_type,
        "loaders": entry.loaders,
        "featured": featured,
        "project_id": project_id,
        "file_parts": ["file"],
    }

    if dry_run:
        print(f"dry-run: would upload {entry.file_relative} as {entry.version_number}")
        return "dry-run"

    file_bytes = entry.file_path.read_bytes()
    mime = mimetypes.guess_type(entry.file_path.name)[0] or "application/octet-stream"
    body, content_type = encode_multipart(
        fields={"data": json.dumps(payload, ensure_ascii=False)},
        files=[("file", entry.file_path.name, file_bytes, mime)],
    )

    response = api_json_request(
        "POST",
        "/version",
        user_agent=user_agent,
        token=token,
        data=body,
        content_type=content_type,
    )

    if not isinstance(response, dict) or "id" not in response:
        raise UploadError(f"unexpected upload response for {entry.version_number}: {response}")

    return str(response["id"])


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Upload patch-specific Modrinth versions from release manifest and markdown notes."
    )
    parser.add_argument("--project", required=True, help="Modrinth project slug or id (example: quick-resource-pack)")
    parser.add_argument(
        "--token",
        default=os.environ.get("MODRINTH_TOKEN", ""),
        help="Modrinth API token (default: MODRINTH_TOKEN env var)",
    )
    parser.add_argument(
        "--manifest",
        default="release/1.21.x/modrinth-upload.csv",
        help="Path to manifest CSV generated by build-release-1.21.sh",
    )
    parser.add_argument(
        "--notes",
        default="MODRINTH_RELEASE_1.1.0.md",
        help="Path to markdown file with version titles/changelogs",
    )
    parser.add_argument(
        "--only",
        action="append",
        default=[],
        help="Upload only a specific minecraft patch (repeatable, example: --only 1.21.11)",
    )
    parser.add_argument(
        "--version-type",
        choices=["release", "beta", "alpha"],
        default="release",
        help="Modrinth version type",
    )
    parser.add_argument(
        "--featured",
        action=argparse.BooleanOptionalAction,
        default=False,
        help="Mark uploaded versions as featured",
    )
    parser.add_argument(
        "--skip-existing",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="Skip uploads when version_number already exists on Modrinth",
    )
    parser.add_argument(
        "--sleep-seconds",
        type=float,
        default=0.5,
        help="Delay between uploads to avoid rate limits",
    )
    parser.add_argument(
        "--dependency",
        action="append",
        default=[],
        help="Dependency spec in format project[:type] (type: required/optional/incompatible/embedded)",
    )
    parser.add_argument(
        "--no-default-deps",
        action="store_true",
        help="Do not include default dependencies (fabric-api, cloth-config, modmenu)",
    )
    parser.add_argument("--dry-run", action="store_true", help="Print actions without uploading")
    parser.add_argument("--user-agent", default=DEFAULT_USER_AGENT, help="Custom HTTP user-agent")
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    if not args.dry_run and not args.token:
        raise UploadError("missing api token; use --token or set MODRINTH_TOKEN")

    manifest_path = Path(args.manifest).resolve()
    notes_path = Path(args.notes).resolve()

    entries = read_manifest(manifest_path)
    selected_entries = select_entries(entries, args.only)
    notes = read_release_notes(notes_path)

    default_dep_specs = ["fabric-api:required", "cloth-config:required", "modmenu:required"]
    dep_specs_raw = [] if args.no_default_deps else list(default_dep_specs)
    dep_specs_raw.extend(args.dependency)
    dependency_specs = parse_dependency_specs(dep_specs_raw)

    project_id = resolve_project_id(args.project, user_agent=args.user_agent, token=args.token or None)
    dependencies = build_dependencies(dependency_specs, user_agent=args.user_agent, token=args.token or None)

    existing_versions: set[str] = set()
    if args.skip_existing:
        existing_versions = fetch_existing_versions(args.project, user_agent=args.user_agent, token=args.token or None)

    uploaded = 0
    skipped = 0

    print(f"project: {args.project} ({project_id})")
    print(f"manifest: {manifest_path}")
    print(f"notes: {notes_path}")
    print()

    for entry in selected_entries:
        primary_game_version = entry.game_versions[0]
        note = notes.get(primary_game_version)
        if note is None:
            note = ReleaseNote(
                title=default_title(entry.version_number, primary_game_version),
                changelog=f"Target Minecraft: {primary_game_version}",
            )

        if entry.version_number in existing_versions and args.skip_existing:
            print(f"skip {entry.version_number}: already exists")
            skipped += 1
            continue

        print(f"upload {entry.version_number} -> {entry.file_relative}")
        version_id = upload_entry(
            entry=entry,
            note=note,
            project_id=project_id,
            dependencies=dependencies,
            version_type=args.version_type,
            featured=args.featured,
            user_agent=args.user_agent,
            token=args.token,
            dry_run=args.dry_run,
        )

        if args.dry_run:
            skipped += 1
        else:
            uploaded += 1
            print(f"  created version id: {version_id}")

        if args.sleep_seconds > 0:
            time.sleep(args.sleep_seconds)

    print()
    print(f"done. uploaded={uploaded}, skipped={skipped}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except UploadError as exc:
        print(f"error: {exc}", file=sys.stderr)
        raise SystemExit(1)
