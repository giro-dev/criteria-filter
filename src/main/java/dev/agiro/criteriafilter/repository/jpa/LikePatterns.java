package dev.agiro.criteriafilter.repository.jpa;

import java.util.Locale;

/**
 * Builds case-insensitive "contains" patterns for SQL {@code LIKE}, escaping
 * user-supplied wildcards so they match literally.
 */
final class LikePatterns {

    static final char ESCAPE = '\\';

    private LikePatterns() {
    }

    static String containsIgnoreCase(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder pattern = new StringBuilder(lower.length() + 2).append('%');
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c == ESCAPE || c == '%' || c == '_') {
                pattern.append(ESCAPE);
            }
            pattern.append(c);
        }
        return pattern.append('%').toString();
    }
}
