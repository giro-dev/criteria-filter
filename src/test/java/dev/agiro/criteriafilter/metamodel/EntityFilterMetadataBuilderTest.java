package dev.agiro.criteriafilter.metamodel;

import dev.agiro.criteriafilter.annotation.CriteriaFilter;
import dev.agiro.criteriafilter.annotation.FilterField;
import dev.agiro.criteriafilter.model.Backend;
import dev.agiro.criteriafilter.model.Operator;
import dev.agiro.criteriafilter.sample.Product;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityFilterMetadataBuilderTest {

    private final EntityFilterMetadataBuilder builder =
            new EntityFilterMetadataBuilder(new DatePatternResolver(List.of(), "yyyy-MM-dd'T'HH:mm:ss"));

    @Test
    void infersOperatorsAndExcludesUnannotatedFields() {
        EntityFilterMetadata metadata = builder.build(Product.class);

        assertThat(metadata.backend()).isEqualTo(Backend.JPA);
        assertThat(metadata.fields().keySet())
                .containsExactlyInAnyOrder("id", "name", "price", "category", "createdAt", "active");
        // internalNote has no @FilterField
        assertThat(metadata.find("internalNote")).isEmpty();

        // String defaults include LIKE, Number defaults include BETWEEN.
        assertThat(metadata.require("name").operators()).contains(Operator.LIKE, Operator.IN);
        assertThat(metadata.require("price").operators()).contains(Operator.BETWEEN, Operator.GT);

        // Explicit operators override inference.
        assertThat(metadata.require("active").operators()).containsExactly(Operator.EQ);
    }

    @Test
    void resolvesDatePatternPerBackend() {
        EntityFilterMetadata metadata = builder.build(Product.class);
        FieldMetadata createdAt = metadata.require("createdAt");

        assertThat(createdAt.datePatterns().get(Backend.JPA)).isEqualTo("yyyy-MM-dd'T'HH:mm:ss'Z'");
        assertThat(createdAt.datePatterns().get(Backend.OPENSEARCH)).isEqualTo("epoch_millis");
    }

    @Test
    void failsFastWhenAnnotatedFieldMissingOnEntity() {
        assertThatThrownBy(() -> builder.build(BrokenDto.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not exist on entity");
    }

    @CriteriaFilter(entity = Product.class)
    static class BrokenDto {
        @FilterField
        private String nonExistentField;
    }
}
