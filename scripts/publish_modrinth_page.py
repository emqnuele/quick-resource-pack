#!/usr/bin/env python3
"""Publish the Modrinth project page (gallery, description and listing) from docs/modrinth."""

from __future__ import annotations

import argparse
import json
import mimetypes
import os
import re
import sys
from pathlib import Path
from urllib import error, parse, request

API_BASE = "https://api.modrinth.com/v2"
USER_AGENT = "quick-resource-pack-page-publisher/1.0 (+https://github.com/emqnuele/quick-resource-pack)"


class PublishError(RuntimeError):
    pass


def call(method: str, path: str, token: str | None, *, data: bytes | None = None,
         content_type: str | None = None) -> dict | list:
    headers = {"Accept": "application/json", "User-Agent": USER_AGENT}
    if token:
        headers["Authorization"] = token
    if content_type:
        headers["Content-Type"] = content_type
    req = request.Request(f"{API_BASE}{path}", data=data, headers=headers, method=method)
    try:
        with request.urlopen(req, timeout=120) as response:
            payload = response.read().decode("utf-8")
    except error.HTTPError as exc:
        raise PublishError(f"{method} {path} failed ({exc.code}): {exc.read().decode('utf-8', 'replace')}") from exc
    except error.URLError as exc:
        raise PublishError(f"{method} {path} failed: {exc.reason}") from exc
    return json.loads(payload) if payload else {}


def gallery_by_title(project: dict) -> dict[str, dict]:
    return {item.get("title") or "": item for item in project.get("gallery", [])}


def upload_gallery(project_id: str, project: dict, page: dict, media: Path, token: str, dry_run: bool) -> None:
    existing = gallery_by_title(project)
    for ordering, item in enumerate(page["gallery"]):
        if item["title"] in existing:
            print(f"gallery: keep '{item['title']}' (already uploaded)")
            continue
        path = media / item["file"]
        if not path.is_file():
            raise PublishError(f"missing gallery file: {path}")
        query = parse.urlencode({
            "ext": path.suffix.lstrip("."),
            "featured": "true" if item.get("featured") else "false",
            "title": item["title"],
            "description": item.get("description", ""),
            "ordering": ordering,
        })
        if dry_run:
            print(f"dry-run: would upload {path.name} as '{item['title']}'")
            continue
        mime = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
        call("POST", f"/project/{project_id}/gallery?{query}", token, data=path.read_bytes(), content_type=mime)
        print(f"gallery: uploaded {path.name} as '{item['title']}'")


def render_body(markdown: str, page: dict, project: dict, dry_run: bool) -> str:
    by_title = gallery_by_title(project)
    by_file = {item["file"]: item["title"] for item in page["gallery"]}

    def replace(match: re.Match) -> str:
        name = match.group(1)
        title = by_file.get(name)
        if title is None:
            raise PublishError(f"description uses media/{name}, which is not in the gallery list")
        uploaded = by_title.get(title)
        if uploaded is None:
            if dry_run:
                return f"<gallery:{title}>"
            raise PublishError(f"gallery item '{title}' was not found after upload")
        # raw keeps the original file; the resized copy can drop animation
        return uploaded.get("raw_url") or uploaded["url"]

    return re.sub(r"media/([A-Za-z0-9_.-]+)", replace, markdown)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", default="quick-resource-pack")
    parser.add_argument("--docs", default="docs/modrinth")
    parser.add_argument("--token", default=os.environ.get("MODRINTH_TOKEN", ""))
    parser.add_argument("--dry-run", action="store_true", help="print what would change without changing it")
    args = parser.parse_args()

    if not args.dry_run and not args.token:
        print("error: set MODRINTH_TOKEN or pass --token", file=sys.stderr)
        return 2

    docs = Path(args.docs)
    page = json.loads((docs / "page.json").read_text(encoding="utf-8"))
    markdown = (docs / "description.md").read_text(encoding="utf-8")
    token = args.token or None

    try:
        project = call("GET", f"/project/{args.project}", token)
        project_id = project["id"]
        upload_gallery(project_id, project, page, docs / "media", args.token, args.dry_run)
        if not args.dry_run:
            project = call("GET", f"/project/{project_id}", token)
        body = render_body(markdown, page, project, args.dry_run)
        changes = {
            "title": page["title"],
            "description": page["summary"],
            "body": body,
            "categories": page["categories"],
            "issues_url": page["issues_url"],
            "source_url": page["source_url"],
        }
        if args.dry_run:
            for key, value in changes.items():
                shown = value if key != "body" else f"{len(value)} characters"
                print(f"dry-run: would set {key} = {shown}")
            return 0
        call("PATCH", f"/project/{project_id}", token, data=json.dumps(changes).encode("utf-8"),
             content_type="application/json")
        print(f"page updated: https://modrinth.com/mod/{project.get('slug', args.project)}")
    except PublishError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
