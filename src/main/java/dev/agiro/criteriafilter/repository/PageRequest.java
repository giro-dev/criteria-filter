package dev.agiro.criteriafilter.repository;

import dev.agiro.criteriafilter.exception.InvalidPageRequestException;

/**
 * Backend-agnostic pagination request. Deliberately not
 * {@code org.springframework.data.domain.Pageable}: OpenSearch pagination
 * (search_after / scroll) does not map cleanly onto JPA offset/limit.
 */
public record PageRequest(int page, int size) {

    public PageRequest {
        if (page < 0) {
            throw new InvalidPageRequestException("page must be >= 0");
        }
        if (size < 1) {
            throw new InvalidPageRequestException("size must be >= 1");
        }
        // Repositories fetch size + 1 rows starting at page * size; both must fit in an int.
        if (size == Integer.MAX_VALUE || (long) page * size > Integer.MAX_VALUE) {
            throw new InvalidPageRequestException("page * size exceeds the maximum supported offset");
        }
    }

    public int offset() {
        return page * size;
    }

    /** Convenience conversion to Spring Data {@code Pageable}, valid for the JPA backend. */
    public org.springframework.data.domain.Pageable toPageable() {
        return org.springframework.data.domain.PageRequest.of(page, size);
    }
}
