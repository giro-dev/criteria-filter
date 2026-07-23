package dev.agiro.criteriafilter.repository;

/**
 * Backend-agnostic pagination request. Deliberately not
 * {@code org.springframework.data.domain.Pageable}: OpenSearch pagination
 * (search_after / scroll) does not map cleanly onto JPA offset/limit.
 */
public record PageRequest(int page, int size) {

    public PageRequest {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be >= 1");
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
