package dev.agiro.criteriafilter.repository;

import java.util.List;

/**
 * Result of a filter query, uniform across backends.
 *
 * @param content  the page of matched entities
 * @param totalHits total number of matches across all pages
 * @param hasMore  whether another page exists after this one
 */
public record FilterResult<T>(List<T> content, long totalHits, boolean hasMore) {

    public FilterResult {
        content = content == null ? List.of() : List.copyOf(content);
    }
}
