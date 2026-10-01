package dev.agiro.jsonbit;

import dev.agiro.criteriafilter.annotation.CriteriaFilter;
import dev.agiro.criteriafilter.annotation.FilterField;
import dev.agiro.criteriafilter.model.Backend;
import dev.agiro.criteriafilter.model.Operator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@CriteriaFilter(backend = Backend.JPA)
public class Document {

    @Id
    @FilterField
    private Long id;

    @FilterField(json = true, operators = {
            Operator.IS_NULL, Operator.IS_NOT_NULL,
            Operator.JSON_CONTAINS, Operator.JSON_CONTAINED_BY,
            Operator.JSON_EXISTS, Operator.JSON_EXISTS_ANY, Operator.JSON_EXISTS_ALL,
            Operator.JSON_PATH_EQ, Operator.JSON_PATH_LIKE,
            Operator.JSON_ARRAY_CONTAINS, Operator.JSON_ARRAY_CONTAINS_ALL, Operator.JSON_ARRAY_CONTAINS_ANY
    })
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> data;

    @FilterField(json = true, operators = {
            Operator.JSON_ARRAY_CONTAINS, Operator.JSON_ARRAY_CONTAINS_ALL, Operator.JSON_ARRAY_CONTAINS_ANY
    })
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Object> tags;

    protected Document() {
    }

    public Document(Long id, Map<String, Object> data, List<Object> tags) {
        this.id = id;
        this.data = data;
        this.tags = tags;
    }

    public Long getId() {
        return id;
    }
}
