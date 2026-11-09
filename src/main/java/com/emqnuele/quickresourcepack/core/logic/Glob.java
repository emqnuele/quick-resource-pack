package com.emqnuele.quickresourcepack.core.logic;

import java.util.Locale;

/** Case-insensitive glob with {@code *} (any run) and {@code ?} (one char); nothing else is special. */
public final class Glob {
    private Glob() {
    }

    public static boolean matches(String pattern, String text) {
        if (pattern == null || text == null) {
            return false;
        }
        String p = pattern.trim().toLowerCase(Locale.ROOT);
        String t = text.trim().toLowerCase(Locale.ROOT);
        int pi = 0;
        int ti = 0;
        int star = -1;
        int mark = 0;
        while (ti < t.length()) {
            if (pi < p.length() && (p.charAt(pi) == '?' || p.charAt(pi) == t.charAt(ti))) {
                pi++;
                ti++;
            } else if (pi < p.length() && p.charAt(pi) == '*') {
                star = pi++;
                mark = ti;
            } else if (star >= 0) {
                pi = star + 1;
                ti = ++mark;
            } else {
                return false;
            }
        }
        while (pi < p.length() && p.charAt(pi) == '*') {
            pi++;
        }
        return pi == p.length();
    }
}
