package dev.agiro.criteriafilter.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilterRequestJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private FilterNode parse(String filterJson) throws Exception {
        return mapper.readValue("{\"filter\":" + filterJson + "}", FilterRequest.class).filter();
    }

    @Test
    void parsesSingleCondition() throws Exception {
        FilterNode node = parse("{\"field\":\"price\",\"operator\":\"GT\",\"value\":10.5}");
        assertThat(node).isEqualTo(new FilterCondition("price", Operator.GT, 10.5, null));
    }

    @Test
    void parsesExplicitCombinatorSyntax() throws Exception {
        FilterNode node = parse("""
                {"combinator":"OR","filters":[
                  {"field":"name","operator":"EQ","value":"a"},
                  {"field":"name","operator":"IS_NULL"}
                ]}""");
        assertThat(node).isEqualTo(FilterGroup.or(
                new FilterCondition("name", Operator.EQ, "a", null),
                new FilterCondition("name", Operator.IS_NULL, null, null)));
    }

    @Test
    void groupWithoutCombinatorDefaultsToAnd() throws Exception {
        FilterNode node = parse("{\"filters\":[{\"field\":\"name\",\"operator\":\"EQ\",\"value\":\"a\"}]}");
        assertThat(((FilterGroup) node).combinator()).isEqualTo(LogicalOperator.AND);
    }

    @Test
    void preservesJsonOperandTypes() throws Exception {
        FilterCondition condition = (FilterCondition) parse(
                "{\"field\":\"x\",\"operator\":\"IN\",\"values\":[1,2.5,true,\"s\",{\"k\":\"v\"},[1]]}");
        assertThat(condition.values()).hasSize(6);
        assertThat(condition.values().get(0)).isEqualTo(1);
        assertThat(condition.values().get(2)).isEqualTo(true);
    }

    @Test
    void roundTripsNestedTree() throws Exception {
        FilterNode tree = FilterGroup.and(
                new FilterCondition("price", Operator.BETWEEN, null, List.of(1, 2)),
                FilterGroup.or(
                        new FilterCondition("name", Operator.LIKE, "Ünïcödé ☕", null),
                        FilterGroup.and()));
        String json = mapper.writeValueAsString(new FilterRequest(tree));
        assertThat(mapper.readValue(json, FilterRequest.class).filter()).isEqualTo(tree);
    }

    /** Regression: {"and":[..],"or":[..]} used to silently drop the "or" branch. */
    @Test
    void rejectsAmbiguousGroupSyntax() {
        assertThatThrownBy(() -> parse("""
                {"and":[{"field":"name","operator":"EQ","value":"a"}],
                 "or":[{"field":"name","operator":"EQ","value":"b"}]}"""))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining("exactly one of");
        assertThatThrownBy(() -> parse("{\"combinator\":\"OR\",\"filters\":[],\"and\":[]}"))
                .isInstanceOf(JsonMappingException.class);
    }

    @Test
    void rejectsUnknownOrLowercaseOperators() {
        assertThatThrownBy(() -> parse("{\"field\":\"name\",\"operator\":\"eq\",\"value\":\"a\"}"))
                .isInstanceOf(JsonMappingException.class);
        assertThatThrownBy(() -> parse("{\"field\":\"name\",\"operator\":\"EQ OR 1=1\",\"value\":\"a\"}"))
                .isInstanceOf(JsonMappingException.class);
    }

    @Test
    void rejectsUnknownCombinator() {
        assertThatThrownBy(() -> parse("{\"combinator\":\"XOR\",\"filters\":[]}"))
                .isInstanceOf(JsonMappingException.class);
    }

    /** Documents Jackson's default: duplicate keys are accepted with last-value-wins semantics. */
    @Test
    void duplicateKeysUseLastValue() throws Exception {
        FilterNode node = parse("{\"field\":\"name\",\"field\":\"price\",\"operator\":\"EQ\",\"value\":1}");
        assertThat(((FilterCondition) node).field()).isEqualTo("price");
    }

    @Test
    void prototypePollutionKeysAreInertInJava() throws Exception {
        String json = "{\"filter\":{\"__proto__\":{\"admin\":true},"
                + "\"field\":\"name\",\"operator\":\"EQ\",\"value\":\"a\"}}";
        assertThatThrownBy(() -> mapper.readValue(json, FilterRequest.class))
                .isInstanceOf(UnrecognizedPropertyException.class);

        // Spring Boot's ObjectMapper ignores unknown properties: the key is simply dropped.
        ObjectMapper lenient = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        assertThat(lenient.readValue(json, FilterRequest.class).filter())
                .isEqualTo(new FilterCondition("name", Operator.EQ, "a", null));
    }

    @Test
    void nullChildrenAreRejected() {
        assertThatThrownBy(() -> parse("{\"and\":[null]}")).isInstanceOf(JsonMappingException.class);
    }
}
