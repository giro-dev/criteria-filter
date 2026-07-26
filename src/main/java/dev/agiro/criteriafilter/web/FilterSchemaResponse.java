package dev.agiro.criteriafilter.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.agiro.criteriafilter.metamodel.EntityFilterMetadata;
import dev.agiro.criteriafilter.metamodel.FieldMetadata;
import dev.agiro.criteriafilter.model.Operator;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Response DTO describing the available filter options for an entity.
 * Exposed via {@code GET /search/schema} endpoint.
 */
@Schema(description = "Filter schema describing available fields and operators for search queries")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FilterSchemaResponse(
        @Schema(description = "Entity type name", example = "Product")
        String entity,

        @Schema(description = "List of filterable fields with their supported operators")
        List<FieldSchema> fields,

        @Schema(description = "All available operators with descriptions")
        List<OperatorInfo> operators,

        @Schema(description = "Example filter request")
        ExampleFilter example
) {

    /**
     * Creates a schema response from entity metadata.
     */
    public static FilterSchemaResponse from(EntityFilterMetadata metadata) {
        List<FieldSchema> fields = metadata.allFields().stream()
                .map(FieldSchema::from)
                .sorted(Comparator.comparing(FieldSchema::name))
                .toList();

        // Collect all operators used by any field
        Set<Operator> usedOperators = metadata.allFields().stream()
                .flatMap(f -> f.operators().stream())
                .collect(Collectors.toSet());

        List<OperatorInfo> operators = usedOperators.stream()
                .map(OperatorInfo::from)
                .sorted(Comparator.comparing(OperatorInfo::name))
                .toList();

        ExampleFilter example = ExampleFilter.generate(fields);

        return new FilterSchemaResponse(
                metadata.entityType().getSimpleName(),
                fields,
                operators,
                example
        );
    }

    @Schema(description = "Metadata for a filterable field")
    public record FieldSchema(
            @Schema(description = "Field name to use in filter requests", example = "price")
            String name,

            @Schema(description = "Java type of the field", example = "BigDecimal")
            String type,

            @Schema(description = "List of operators supported for this field")
            List<String> operators,

            @Schema(description = "Whether this is a nested/related field")
            boolean nested
    ) {
        public static FieldSchema from(FieldMetadata field) {
            return new FieldSchema(
                    field.logicalName(),
                    field.type().getSimpleName(),
                    field.operators().stream()
                            .map(Operator::name)
                            .sorted()
                            .toList(),
                    field.nested()
            );
        }
    }

    @Schema(description = "Information about an operator")
    public record OperatorInfo(
            @Schema(description = "Operator name", example = "EQ")
            String name,

            @Schema(description = "Number of values required", example = "SINGLE")
            String arity,

            @Schema(description = "Human-readable description")
            String description,

            @Schema(description = "Example usage")
            String example
    ) {
        public static OperatorInfo from(Operator op) {
            return new OperatorInfo(
                    op.name(),
                    op.arity().name(),
                    descriptionFor(op),
                    exampleFor(op)
            );
        }

        private static String descriptionFor(Operator op) {
            return switch (op) {
                case EQ -> "Equals";
                case NE -> "Not equals";
                case GT -> "Greater than";
                case GTE -> "Greater than or equal";
                case LT -> "Less than";
                case LTE -> "Less than or equal";
                case LIKE -> "Contains substring (case-insensitive)";
                case IN -> "Value is in list";
                case BETWEEN -> "Value is between two bounds (inclusive)";
                case IS_NULL -> "Value is null";
                case IS_NOT_NULL -> "Value is not null";
                case JSON_CONTAINS -> "JSONB contains the given JSON";
                case JSON_CONTAINED_BY -> "JSONB is contained by the given JSON";
                case JSON_EXISTS -> "JSONB has the specified key";
                case JSON_EXISTS_ANY -> "JSONB has any of the specified keys";
                case JSON_EXISTS_ALL -> "JSONB has all of the specified keys";
                case JSON_PATH_EQ -> "JSONB path value equals";
                case JSON_PATH_LIKE -> "JSONB path value contains substring";
                case JSON_ARRAY_CONTAINS -> "JSONB array contains value";
                case JSON_ARRAY_CONTAINS_ALL -> "JSONB array contains all values";
                case JSON_ARRAY_CONTAINS_ANY -> "JSONB array contains any value";
            };
        }

        private static String exampleFor(Operator op) {
            return switch (op) {
                case EQ -> "{\"field\": \"status\", \"operator\": \"EQ\", \"value\": \"ACTIVE\"}";
                case NE -> "{\"field\": \"status\", \"operator\": \"NE\", \"value\": \"DELETED\"}";
                case GT -> "{\"field\": \"price\", \"operator\": \"GT\", \"value\": 100}";
                case GTE -> "{\"field\": \"price\", \"operator\": \"GTE\", \"value\": 100}";
                case LT -> "{\"field\": \"price\", \"operator\": \"LT\", \"value\": 50}";
                case LTE -> "{\"field\": \"price\", \"operator\": \"LTE\", \"value\": 50}";
                case LIKE -> "{\"field\": \"name\", \"operator\": \"LIKE\", \"value\": \"java\"}";
                case IN -> "{\"field\": \"category\", \"operator\": \"IN\", \"values\": [\"BOOK\", \"TOY\"]}";
                case BETWEEN -> "{\"field\": \"price\", \"operator\": \"BETWEEN\", \"values\": [10, 100]}";
                case IS_NULL -> "{\"field\": \"deletedAt\", \"operator\": \"IS_NULL\"}";
                case IS_NOT_NULL -> "{\"field\": \"email\", \"operator\": \"IS_NOT_NULL\"}";
                case JSON_CONTAINS -> "{\"field\": \"metadata\", \"operator\": \"JSON_CONTAINS\", \"value\": \"{\\\"status\\\": \\\"active\\\"}\"}";
                case JSON_CONTAINED_BY -> "{\"field\": \"config\", \"operator\": \"JSON_CONTAINED_BY\", \"value\": \"{\\\"all\\\": true}\"}";
                case JSON_EXISTS -> "{\"field\": \"metadata\", \"operator\": \"JSON_EXISTS\", \"value\": \"email\"}";
                case JSON_EXISTS_ANY -> "{\"field\": \"metadata\", \"operator\": \"JSON_EXISTS_ANY\", \"values\": [\"email\", \"phone\"]}";
                case JSON_EXISTS_ALL -> "{\"field\": \"metadata\", \"operator\": \"JSON_EXISTS_ALL\", \"values\": [\"name\", \"email\"]}";
                case JSON_PATH_EQ -> "{\"field\": \"config\", \"operator\": \"JSON_PATH_EQ\", \"values\": [\"settings.theme\", \"dark\"]}";
                case JSON_PATH_LIKE -> "{\"field\": \"config\", \"operator\": \"JSON_PATH_LIKE\", \"values\": [\"settings.name\", \"test\"]}";
                case JSON_ARRAY_CONTAINS -> "{\"field\": \"tags\", \"operator\": \"JSON_ARRAY_CONTAINS\", \"value\": \"premium\"}";
                case JSON_ARRAY_CONTAINS_ALL -> "{\"field\": \"tags\", \"operator\": \"JSON_ARRAY_CONTAINS_ALL\", \"values\": [\"premium\", \"featured\"]}";
                case JSON_ARRAY_CONTAINS_ANY -> "{\"field\": \"tags\", \"operator\": \"JSON_ARRAY_CONTAINS_ANY\", \"values\": [\"sale\", \"new\"]}";
            };
        }
    }

    @Schema(description = "Example filter request structure")
    public record ExampleFilter(
            @Schema(description = "Simple AND filter example")
            Object simpleAnd,

            @Schema(description = "Nested AND/OR filter example")
            Object nestedAndOr
    ) {
        public static ExampleFilter generate(List<FieldSchema> fields) {
            if (fields.isEmpty()) {
                return new ExampleFilter(null, null);
            }

            // Pick first two fields for examples
            FieldSchema f1 = fields.get(0);
            FieldSchema f2 = fields.size() > 1 ? fields.get(1) : f1;

            String op1 = f1.operators().contains("EQ") ? "EQ" : f1.operators().get(0);
            String op2 = f2.operators().contains("LIKE") ? "LIKE" :
                         f2.operators().contains("EQ") ? "EQ" : f2.operators().get(0);

            java.util.LinkedHashMap<String, Object> f1Entry = new java.util.LinkedHashMap<>();
            f1Entry.put("field", f1.name());
            f1Entry.put("operator", op1);
            f1Entry.put("value", exampleValueFor(f1.type()));

            java.util.LinkedHashMap<String, Object> f2Entry = new java.util.LinkedHashMap<>();
            f2Entry.put("field", f2.name());
            f2Entry.put("operator", op2);
            f2Entry.put("value", exampleValueFor(f2.type()));

            java.util.LinkedHashMap<String, Object> simpleAndFilter = new java.util.LinkedHashMap<>();
            simpleAndFilter.put("and", Arrays.asList(f1Entry, f2Entry));

            java.util.LinkedHashMap<String, Object> simpleAnd = new java.util.LinkedHashMap<>();
            simpleAnd.put("filter", simpleAndFilter);

            java.util.LinkedHashMap<String, Object> nestedF1Entry = new java.util.LinkedHashMap<>();
            nestedF1Entry.put("field", f1.name());
            nestedF1Entry.put("operator", op1);
            nestedF1Entry.put("value", exampleValueFor(f1.type()));

            java.util.LinkedHashMap<String, Object> orEntry1 = new java.util.LinkedHashMap<>();
            orEntry1.put("field", f2.name());
            orEntry1.put("operator", op2);
            orEntry1.put("value", exampleValueFor(f2.type()));

            java.util.LinkedHashMap<String, Object> orEntry2 = new java.util.LinkedHashMap<>();
            orEntry2.put("field", f2.name());
            orEntry2.put("operator", "IS_NOT_NULL");

            java.util.LinkedHashMap<String, Object> orGroup = new java.util.LinkedHashMap<>();
            orGroup.put("or", Arrays.asList(orEntry1, orEntry2));

            java.util.LinkedHashMap<String, Object> nestedAndOrFilter = new java.util.LinkedHashMap<>();
            nestedAndOrFilter.put("and", Arrays.asList(nestedF1Entry, orGroup));

            java.util.LinkedHashMap<String, Object> nestedAndOr = new java.util.LinkedHashMap<>();
            nestedAndOr.put("filter", nestedAndOrFilter);

            return new ExampleFilter(simpleAnd, nestedAndOr);
        }

        private static Object exampleValueFor(String type) {
            return switch (type) {
                case "String" -> "example";
                case "Integer", "Long", "int", "long" -> 42;
                case "Double", "Float", "double", "float", "BigDecimal" -> 99.99;
                case "Boolean", "boolean" -> true;
                case "Instant", "LocalDateTime", "ZonedDateTime" -> "2024-01-15T10:30:00Z";
                case "LocalDate" -> "2024-01-15";
                default -> "value";
            };
        }
    }
}
