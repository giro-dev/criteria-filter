package dev.agiro.criteriafilter.repository.jpa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LikePatternsTest {

    @Test
    void wrapsValueInContainsWildcards() {
        assertThat(LikePatterns.containsIgnoreCase("Java")).isEqualTo("%java%");
    }

    @Test
    void escapesUserSuppliedWildcards() {
        assertThat(LikePatterns.containsIgnoreCase("100%")).isEqualTo("%100\\%%");
        assertThat(LikePatterns.containsIgnoreCase("a_b")).isEqualTo("%a\\_b%");
        assertThat(LikePatterns.containsIgnoreCase("c:\\dir")).isEqualTo("%c:\\\\dir%");
    }

    @Test
    void emptyValueMatchesEverything() {
        assertThat(LikePatterns.containsIgnoreCase("")).isEqualTo("%%");
    }

    @Test
    void lowercasesIndependentlyOfDefaultLocale() {
        // Locale.ROOT: 'I' must not become the dotless Turkish 'ı'.
        assertThat(LikePatterns.containsIgnoreCase("TITLE")).isEqualTo("%title%");
        assertThat(LikePatterns.containsIgnoreCase("ÉÑÜ")).isEqualTo("%éñü%");
    }
}
