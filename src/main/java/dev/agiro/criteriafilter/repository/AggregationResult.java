package dev.agiro.criteriafilter.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of an aggregation query, uniform across backends.
 *
 * <p>Each row maps every {@code groupBy} field name to its group value, then
 * every aggregation alias to its result, in request order. Without
 * {@code groupBy} there is exactly one row. Values may be {@code null} (e.g.
 * the {@code SUM} of an empty group, or a {@code null} group key).
 *
 * <pre>{@code
 * {"rows": [
 *   {"category": "BOOK", "totalPrice": 80.00, "n": 2},
 *   {"category": "TOY",  "totalPrice": 15.50, "n": 1}
 * ]}
 * }</pre>
 */
public record AggregationResult(List<Map<String, Object>> rows) {

    public AggregationResult {
        List<Map<String, Object>> copy = new ArrayList<>(rows == null ? 0 : rows.size());
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                copy.add(Collections.unmodifiableMap(new LinkedHashMap<>(row)));
            }
        }
        rows = Collections.unmodifiableList(copy);
    }

    public static AggregationResult empty() {
        return new AggregationResult(List.of());
    }
}
