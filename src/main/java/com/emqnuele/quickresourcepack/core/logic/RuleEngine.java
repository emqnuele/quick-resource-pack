package com.emqnuele.quickresourcepack.core.logic;

import com.emqnuele.quickresourcepack.core.config.Rule;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Decides which slots rules want on for a given place. Pure: the game layer
 * feeds it the context and applies the difference.
 */
public final class RuleEngine {
    private RuleEngine() {
    }

    /**
     * Where the player is. Every field may be null (e.g. no world loaded).
     *
     * @param server    multiplayer address as typed in the server list
     * @param world     singleplayer world name
     * @param dimension dimension id such as {@code minecraft:the_nether}
     */
    public record Context(String server, String world, String dimension) {
        public static final Context NONE = new Context(null, null, null);
    }

    /** Slot ids wanted on, in rule order, without duplicates. */
    public static Set<String> wantedSlots(List<Rule> rules, Context context) {
        Set<String> wanted = new LinkedHashSet<>();
        if (rules == null || context == null) {
            return wanted;
        }
        for (Rule rule : rules) {
            if (rule != null && rule.enabled && rule.isComplete() && matches(rule, context)) {
                wanted.add(rule.slotId);
            }
        }
        return wanted;
    }

    public static boolean matches(Rule rule, Context context) {
        return switch (rule.trigger) {
            case SERVER -> context.server() != null && matchesServer(rule.pattern, context.server());
            case WORLD -> context.world() != null && Glob.matches(rule.pattern, context.world());
            case DIMENSION -> context.dimension() != null
                    && normalizeDimension(rule.pattern).equals(normalizeDimension(context.dimension()));
        };
    }

    /** A pattern without a port matches the host on any port: "mc.example.net" covers ":25566" too. */
    static boolean matchesServer(String pattern, String address) {
        if (Glob.matches(pattern, address)) {
            return true;
        }
        if (pattern.contains(":")) {
            return false;
        }
        return Glob.matches(pattern, stripPort(address));
    }

    static String stripPort(String address) {
        String trimmed = address.trim();
        // bracketed ipv6 keeps its colons; only a trailing :port is dropped
        if (trimmed.startsWith("[")) {
            int close = trimmed.indexOf(']');
            return close > 0 ? trimmed.substring(1, close) : trimmed;
        }
        int colon = trimmed.lastIndexOf(':');
        if (colon > 0 && trimmed.indexOf(':') == colon) {
            return trimmed.substring(0, colon);
        }
        return trimmed;
    }

    static String normalizeDimension(String id) {
        String value = id.trim().toLowerCase(Locale.ROOT);
        return value.contains(":") ? value : "minecraft:" + value;
    }
}
