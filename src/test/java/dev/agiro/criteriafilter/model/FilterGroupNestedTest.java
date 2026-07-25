package dev.agiro.criteriafilter.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for nested AND/OR filter groups using both syntaxes.
 */
class FilterGroupNestedTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializesIhoStyleAndGroup() throws Exception {
        String json = """
                {
                  "and": [
                    {"field": "status", "operator": "EQ", "value": "ACTIVE"},
                    {"field": "category", "operator": "EQ", "value": "BOOK"}
                  ]
                }
                """;

        FilterNode node = mapper.readValue(json, FilterNode.class);

        assertThat(node).isInstanceOf(FilterGroup.class);
        FilterGroup group = (FilterGroup) node;
        assertThat(group.combinator()).isEqualTo(LogicalOperator.AND);
        assertThat(group.filters()).hasSize(2);
        assertThat(group.filters().get(0)).isInstanceOf(FilterCondition.class);
    }

    @Test
    void deserializesIhoStyleOrGroup() throws Exception {
        String json = """
                {
                  "or": [
                    {"field": "price", "operator": "LT", "value": 10},
                    {"field": "price", "operator": "GT", "value": 100}
                  ]
                }
                """;

        FilterNode node = mapper.readValue(json, FilterNode.class);

        assertThat(node).isInstanceOf(FilterGroup.class);
        FilterGroup group = (FilterGroup) node;
        assertThat(group.combinator()).isEqualTo(LogicalOperator.OR);
        assertThat(group.filters()).hasSize(2);
    }

    @Test
    void deserializesNestedAndOrGroups() throws Exception {
        String json = """
                {
                  "and": [
                    {"field": "active", "operator": "EQ", "value": true},
                    {
                      "or": [
                        {"field": "category", "operator": "EQ", "value": "BOOK"},
                        {"field": "category", "operator": "EQ", "value": "TOY"}
                      ]
                    }
                  ]
                }
                """;

        FilterNode node = mapper.readValue(json, FilterNode.class);

        assertThat(node).isInstanceOf(FilterGroup.class);
        FilterGroup andGroup = (FilterGroup) node;
        assertThat(andGroup.combinator()).isEqualTo(LogicalOperator.AND);
        assertThat(andGroup.filters()).hasSize(2);

        // First child is a condition
        assertThat(andGroup.filters().get(0)).isInstanceOf(FilterCondition.class);

        // Second child is nested OR group
        assertThat(andGroup.filters().get(1)).isInstanceOf(FilterGroup.class);
        FilterGroup orGroup = (FilterGroup) andGroup.filters().get(1);
        assertThat(orGroup.combinator()).isEqualTo(LogicalOperator.OR);
        assertThat(orGroup.filters()).hasSize(2);
    }

    @Test
    void deserializesDeeplyNestedGroups() throws Exception {
        String json = """
                {
                  "and": [
                    {
                      "or": [
                        {"field": "a", "operator": "EQ", "value": 1},
                        {
                          "and": [
                            {"field": "b", "operator": "EQ", "value": 2},
                            {"field": "c", "operator": "EQ", "value": 3}
                          ]
                        }
                      ]
                    },
                    {"field": "d", "operator": "EQ", "value": 4}
                  ]
                }
                """;

        FilterNode node = mapper.readValue(json, FilterNode.class);

        assertThat(node).isInstanceOf(FilterGroup.class);
        FilterGroup root = (FilterGroup) node;
        assertThat(root.combinator()).isEqualTo(LogicalOperator.AND);

        // Navigate to deeply nested AND
        FilterGroup level1Or = (FilterGroup) root.filters().get(0);
        assertThat(level1Or.combinator()).isEqualTo(LogicalOperator.OR);

        FilterGroup level2And = (FilterGroup) level1Or.filters().get(1);
        assertThat(level2And.combinator()).isEqualTo(LogicalOperator.AND);
        assertThat(level2And.filters()).hasSize(2);
    }

    @Test
    void serializesToIhoStyleJson() throws Exception {
        FilterGroup group = FilterGroup.and(
                new FilterCondition("status", Operator.EQ, "ACTIVE", null),
                FilterGroup.or(
                        new FilterCondition("category", Operator.EQ, "BOOK", null),
                        new FilterCondition("category", Operator.EQ, "TOY", null)
                )
        );

        String json = mapper.writeValueAsString(group);

        // Should serialize with "and"/"or" keys, not "combinator"/"filters"
        assertThat(json).contains("\"and\"");
        assertThat(json).contains("\"or\"");
        assertThat(json).doesNotContain("\"combinator\"");
        assertThat(json).doesNotContain("\"filters\"");
    }

    @Test
    void roundTripsNestedStructure() throws Exception {
        FilterGroup original = FilterGroup.and(
                new FilterCondition("active", Operator.EQ, true, null),
                FilterGroup.or(
                        new FilterCondition("price", Operator.LT, 50, null),
                        FilterGroup.and(
                                new FilterCondition("category", Operator.EQ, "PREMIUM", null),
                                new FilterCondition("stock", Operator.GT, 0, null)
                        )
                )
        );

        String json = mapper.writeValueAsString(original);
        FilterNode deserialized = mapper.readValue(json, FilterNode.class);

        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    void supportsLegacyCombinatorSyntax() throws Exception {
        String json = """
                {
                  "combinator": "AND",
                  "filters": [
                    {"field": "status", "operator": "EQ", "value": "ACTIVE"}
                  ]
                }
                """;

        FilterNode node = mapper.readValue(json, FilterNode.class);

        assertThat(node).isInstanceOf(FilterGroup.class);
        FilterGroup group = (FilterGroup) node;
        assertThat(group.combinator()).isEqualTo(LogicalOperator.AND);
        assertThat(group.filters()).hasSize(1);
    }

    @Test
    void factoryMethodsCreateCorrectGroups() {
        FilterCondition c1 = new FilterCondition("a", Operator.EQ, 1, null);
        FilterCondition c2 = new FilterCondition("b", Operator.EQ, 2, null);

        FilterGroup andGroup = FilterGroup.and(c1, c2);
        assertThat(andGroup.combinator()).isEqualTo(LogicalOperator.AND);
        assertThat(andGroup.filters()).containsExactly(c1, c2);

        FilterGroup orGroup = FilterGroup.or(c1, c2);
        assertThat(orGroup.combinator()).isEqualTo(LogicalOperator.OR);
        assertThat(orGroup.filters()).containsExactly(c1, c2);
    }

    @Test
    void deserializesFilterRequestWithNestedFilter() throws Exception {
        String json = """
                {
                  "filter": {
                    "and": [
                      {"field": "status", "operator": "EQ", "value": "ACTIVE"},
                      {
                        "or": [
                          {"field": "category", "operator": "EQ", "value": "BOOK"},
                          {"field": "price", "operator": "LT", "value": 20}
                        ]
                      }
                    ]
                  }
                }
                """;

        FilterRequest request = mapper.readValue(json, FilterRequest.class);

        assertThat(request.filter()).isInstanceOf(FilterGroup.class);
        FilterGroup root = (FilterGroup) request.filter();
        assertThat(root.combinator()).isEqualTo(LogicalOperator.AND);
        assertThat(root.filters()).hasSize(2);
        assertThat(root.filters().get(1)).isInstanceOf(FilterGroup.class);
    }
}
