package dev.agiro.criteriafilter.repository.jpa;

import dev.agiro.criteriafilter.exception.FilterTranslationException;
import dev.agiro.criteriafilter.sample.Product.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueCoercionTest {

    @Test
    void nullStaysNull() {
        assertThat(ValueCoercion.coerce(null, Integer.class)).isNull();
    }

    @Test
    void coercesStringsAndUnicodeUnchanged() {
        assertThat(ValueCoercion.coerce("Ünïcödé ☕ 😀", String.class)).isEqualTo("Ünïcödé ☕ 😀");
        assertThat(ValueCoercion.coerce(42, String.class)).isEqualTo("42");
        assertThat(ValueCoercion.coerce("", String.class)).isEqualTo("");
    }

    @Test
    void coercesEnumsCaseSensitively() {
        assertThat(ValueCoercion.coerce("BOOK", Category.class)).isEqualTo(Category.BOOK);
        assertThatThrownBy(() -> ValueCoercion.coerce("book", Category.class))
                .isInstanceOf(FilterTranslationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "TRUE", " True "})
    void coercesTrueStrings(String value) {
        assertThat(ValueCoercion.coerce(value, Boolean.class)).isEqualTo(true);
        assertThat(ValueCoercion.coerce(value, boolean.class)).isEqualTo(true);
    }

    @Test
    void coercesFalseString() {
        assertThat(ValueCoercion.coerce("false", Boolean.class)).isEqualTo(false);
    }

    /** Regression: arbitrary strings used to be silently coerced to {@code false}. */
    @ParameterizedTest
    @ValueSource(strings = {"yes", "1", "0", "not-a-boolean", ""})
    void rejectsNonBooleanStrings(String value) {
        assertThatThrownBy(() -> ValueCoercion.coerce(value, Boolean.class))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void rejectsNumericBoolean() {
        assertThatThrownBy(() -> ValueCoercion.coerce(1, Boolean.class))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void coercesIntegralBoundaries() {
        assertThat(ValueCoercion.coerce("2147483647", Integer.class)).isEqualTo(Integer.MAX_VALUE);
        assertThat(ValueCoercion.coerce("-2147483648", Integer.class)).isEqualTo(Integer.MIN_VALUE);
        assertThat(ValueCoercion.coerce(Long.MAX_VALUE, Long.class)).isEqualTo(Long.MAX_VALUE);
        assertThat(ValueCoercion.coerce(" 7 ", Short.class)).isEqualTo((short) 7);
        assertThat(ValueCoercion.coerce("127", Byte.class)).isEqualTo((byte) 127);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2147483648", "-2147483649", "1.5", "1e3", "abc", ""})
    void rejectsOverflowingOrMalformedIntegers(String value) {
        assertThatThrownBy(() -> ValueCoercion.coerce(value, Integer.class))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void rejectsLongOverflow() {
        assertThatThrownBy(() -> ValueCoercion.coerce("9223372036854775808", Long.class))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void coercesArbitraryPrecisionNumbers() {
        assertThat(ValueCoercion.coerce("123456789012345678901234567890.123", BigDecimal.class))
                .isEqualTo(new BigDecimal("123456789012345678901234567890.123"));
        assertThat(ValueCoercion.coerce(new BigInteger("99999999999999999999"), BigInteger.class))
                .isEqualTo(new BigInteger("99999999999999999999"));
        assertThat(ValueCoercion.coerce(0.1d, BigDecimal.class)).isEqualTo(new BigDecimal("0.1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "-Infinity"})
    void rejectsNonFiniteBigDecimal(String value) {
        assertThatThrownBy(() -> ValueCoercion.coerce(value, BigDecimal.class))
                .isInstanceOf(FilterTranslationException.class);
    }

    /** Regression: NaN / Infinity used to reach the database as comparison operands. */
    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "-Infinity", "1e400"})
    void rejectsNonFiniteFloatingPoint(String value) {
        assertThatThrownBy(() -> ValueCoercion.coerce(value, Double.class))
                .isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> ValueCoercion.coerce(value, float.class))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void acceptsFiniteFloatingPointBoundaries() {
        assertThat(ValueCoercion.coerce(String.valueOf(Double.MAX_VALUE), Double.class)).isEqualTo(Double.MAX_VALUE);
        assertThat(ValueCoercion.coerce("-0.0", Double.class)).isEqualTo(-0.0d);
        assertThat(ValueCoercion.coerce(1, Double.class)).isEqualTo(1.0d);
    }

    /** Regression: "1e999999999" overflowed inside the database driver (HTTP 500). */
    @ParameterizedTest
    @ValueSource(strings = {"1e999999999", "1e-999999999", "1e1001"})
    void rejectsExtremeArbitraryPrecisionNumbers(String value) {
        assertThatThrownBy(() -> ValueCoercion.coerce(value, BigDecimal.class))
                .isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> ValueCoercion.coerce(value, BigInteger.class))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void bigIntegerRejectsFractions() {
        assertThat(ValueCoercion.coerce("1e3", BigInteger.class)).isEqualTo(BigInteger.valueOf(1000));
        assertThatThrownBy(() -> ValueCoercion.coerce("1.5", BigInteger.class))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void coercesUuid() {
        UUID uuid = UUID.randomUUID();
        assertThat(ValueCoercion.coerce(uuid.toString(), UUID.class)).isEqualTo(uuid);
        assertThatThrownBy(() -> ValueCoercion.coerce("not-a-uuid", UUID.class))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void coercesTemporalTypesWithTimezones() {
        assertThat(ValueCoercion.coerce("2024-01-01T10:00:00+02:00", Instant.class))
                .isEqualTo(Instant.parse("2024-01-01T08:00:00Z"));
        assertThat(ValueCoercion.coerce("2024-02-29", LocalDate.class)).isEqualTo(LocalDate.of(2024, 2, 29));
        assertThat(ValueCoercion.coerce("2024-01-01T00:00:00", LocalDateTime.class))
                .isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0));
        assertThat(((OffsetDateTime) ValueCoercion.coerce("2024-01-01T10:00:00-05:00", OffsetDateTime.class))
                .toInstant()).isEqualTo(Instant.parse("2024-01-01T15:00:00Z"));
        assertThat(((ZonedDateTime) ValueCoercion.coerce("2024-03-31T03:30:00+02:00[Europe/Madrid]",
                ZonedDateTime.class)).toInstant()).isEqualTo(Instant.parse("2024-03-31T01:30:00Z"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2023-02-29", "2024-13-01", "2024-01-01T00:00:00", "1704067200000", ""})
    void rejectsInvalidLocalDates(String value) {
        assertThatThrownBy(() -> ValueCoercion.coerce(value, LocalDate.class))
                .isInstanceOf(FilterTranslationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024-01-01T00:00:00Z", "2024-01-01", "2024-01-01T25:00:00"})
    void localDateTimeRejectsZonedOrPartialValues(String value) {
        assertThatThrownBy(() -> ValueCoercion.coerce(value, LocalDateTime.class))
                .isInstanceOf(FilterTranslationException.class);
    }

    /** Regression: objects/arrays used to be stringified ("{a=1}") and silently compared. */
    @Test
    void rejectsStructuredValuesForScalarTypes() {
        assertThatThrownBy(() -> ValueCoercion.coerce(Map.of("a", 1), String.class))
                .isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> ValueCoercion.coerce(List.of("Sony"), String.class))
                .isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> ValueCoercion.coerce(List.of(1), Integer.class))
                .isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> ValueCoercion.coerce(Map.of(), Boolean.class))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void passesStructuredValuesThroughForStructuredTypes() {
        Map<String, Object> value = Map.of("theme", "dark");
        assertThat(ValueCoercion.coerce(value, Map.class)).isSameAs(value);
    }
}
