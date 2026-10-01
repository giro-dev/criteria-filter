package dev.agiro.criteriafilter.repository;

import dev.agiro.criteriafilter.exception.InvalidPageRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestTest {

    @Test
    void computesOffset() {
        assertThat(new PageRequest(3, 20).offset()).isEqualTo(60);
    }

    @Test
    void rejectsNegativePageAndNonPositiveSize() {
        assertThatThrownBy(() -> new PageRequest(-1, 10))
                .isInstanceOf(InvalidPageRequestException.class)
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageRequest(0, 0)).isInstanceOf(InvalidPageRequestException.class);
    }

    /** Regression: page * size used to overflow into a negative offset. */
    @Test
    void rejectsOffsetOverflow() {
        assertThatThrownBy(() -> new PageRequest(Integer.MAX_VALUE, 100))
                .isInstanceOf(InvalidPageRequestException.class);
        assertThatThrownBy(() -> new PageRequest(1 << 16, 1 << 16))
                .isInstanceOf(InvalidPageRequestException.class);
    }

    /** Regression: size + 1 (the hasMore probe) used to overflow. */
    @Test
    void rejectsMaxIntSize() {
        assertThatThrownBy(() -> new PageRequest(0, Integer.MAX_VALUE))
                .isInstanceOf(InvalidPageRequestException.class);
    }

    @Test
    void acceptsLargestRepresentableOffset() {
        assertThat(new PageRequest(1, Integer.MAX_VALUE - 1).offset()).isEqualTo(Integer.MAX_VALUE - 1);
    }
}
