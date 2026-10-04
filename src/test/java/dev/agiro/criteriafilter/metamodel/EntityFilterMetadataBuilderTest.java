package dev.agiro.criteriafilter.metamodel;

import dev.agiro.criteriafilter.annotation.CriteriaFilter;
import dev.agiro.criteriafilter.annotation.FilterField;
import dev.agiro.criteriafilter.model.Backend;
import dev.agiro.criteriafilter.model.FieldSelection;
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

    @Test
    void autoWithAnnotatedFieldsOnlyExposesAnnotatedOnes() {
        assertThat(builder.build(AutoAnnotated.class).fields().keySet()).containsExactly("name");
    }

    @Test
    void autoWithoutAnnotatedFieldsExposesAllNonStaticFields() {
        assertThat(builder.build(AutoPlain.class).fields().keySet()).containsExactlyInAnyOrder("name", "stock");
    }

    @Test
    void explicitAnnotatedWithoutFilterFieldsExposesNothing() {
        assertThat(builder.build(AnnotatedPlain.class).fields()).isEmpty();
    }

    @Test
    void explicitAllFieldsKeepsUnannotatedFieldsFilterable() {
        EntityFilterMetadata metadata = builder.build(AllFieldsMixed.class);

        assertThat(metadata.fields().keySet()).containsExactlyInAnyOrder("title", "category", "stock");
        assertThat(metadata.require("title").javaFieldName()).isEqualTo("name");
        assertThat(metadata.require("title").operators()).containsExactly(Operator.EQ);
        assertThat(metadata.require("category").operators()).contains(Operator.LIKE);
    }

    @Test
    void excludedFieldIsSkippedInEveryMode() {
        assertThat(builder.build(AutoAnnotatedExcluded.class).fields().keySet()).containsExactly("name");
        assertThat(builder.build(AnnotatedExcluded.class).fields().keySet()).containsExactly("name");
        assertThat(builder.build(AllFieldsExcluded.class).fields().keySet()).containsExactly("name");
    }

    @Test
    void autoTreatsExcludedAnnotationAsAnnotatedMode() {
        // Pre-existing AUTO behaviour: @FilterField(excluded = true) alone switches to ANNOTATED.
        assertThat(builder.build(AutoPlainExcluded.class).fields()).isEmpty();
    }

    @CriteriaFilter(entity = Product.class)
    static class BrokenDto {
        @FilterField
        private String nonExistentField;
    }

    @CriteriaFilter
    static class AutoAnnotated {
        @FilterField
        private String name;
        private String secret;
    }

    @CriteriaFilter
    static class AutoPlain {
        static final String CONSTANT = "x";
        private String name;
        private int stock;
    }

    @CriteriaFilter(selection = FieldSelection.ANNOTATED)
    static class AnnotatedPlain {
        private String name;
        private int stock;
    }

    @CriteriaFilter(selection = FieldSelection.ALL_FIELDS)
    static class AllFieldsMixed {
        static final String CONSTANT = "x";
        @FilterField(name = "title", operators = {Operator.EQ})
        private String name;
        private String category;
        private int stock;
    }

    @CriteriaFilter
    static class AutoAnnotatedExcluded {
        @FilterField
        private String name;
        @FilterField(excluded = true)
        private String secret;
    }

    @CriteriaFilter
    static class AutoPlainExcluded {
        private String name;
        @FilterField(excluded = true)
        private String secret;
    }

    @CriteriaFilter(selection = FieldSelection.ANNOTATED)
    static class AnnotatedExcluded {
        @FilterField
        private String name;
        @FilterField(excluded = true)
        private String secret;
    }

    @CriteriaFilter(selection = FieldSelection.ALL_FIELDS)
    static class AllFieldsExcluded {
        private String name;
        @FilterField(excluded = true)
        private String secret;
    }
}
