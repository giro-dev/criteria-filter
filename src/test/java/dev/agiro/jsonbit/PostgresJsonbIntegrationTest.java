package dev.agiro.jsonbit;

import dev.agiro.criteriafilter.exception.FilterTranslationException;
import dev.agiro.criteriafilter.model.FilterCondition;
import dev.agiro.criteriafilter.model.FilterGroup;
import dev.agiro.criteriafilter.model.FilterNode;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.model.Operator;
import dev.agiro.criteriafilter.repository.CriteriaRepositoryRegistry;
import dev.agiro.criteriafilter.repository.PageRequest;
import dev.agiro.criteriafilter.validation.FilterValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises {@code PostgresJsonbOperatorHandler} against a real PostgreSQL instance.
 * Skipped when Docker is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = JsonbTestApplication.class, properties = {
        "criteria-filter.base-packages=dev.agiro.jsonbit",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PostgresJsonbIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    private CriteriaRepositoryRegistry registry;

    @Autowired
    private FilterValidator validator;

    @Autowired
    private DocumentJpaRepository documents;

    @BeforeEach
    void seed() {
        documents.deleteAll();
        documents.saveAll(List.of(
                new Document(1L, Map.of(
                        "theme", "dark",
                        "address", Map.of("city", "Madrid", "zip", "28001"),
                        "labels", List.of("vip", "beta"),
                        "note", "100% sure"), List.of("vip", "beta", 7)),
                new Document(2L, Map.of(
                        "theme", "light",
                        "address", Map.of("city", "Paris"),
                        "labels", List.of("beta"),
                        "note", "back\\slash"), List.of("beta")),
                new Document(3L, Map.of("theme", "dark", "o'brien", "quote"), List.of()),
                new Document(4L, null, null)
        ));
    }

    private List<Long> ids(FilterNode filter) {
        FilterRequest request = new FilterRequest(filter);
        validator.validate(request, Document.class);
        return registry.resolve(Document.class).filter(request, new PageRequest(0, 100))
                .content().stream().map(Document::getId).toList();
    }

    private static FilterCondition cond(String field, Operator operator, Object value) {
        return new FilterCondition(field, operator, value, null);
    }

    private static FilterCondition multi(String field, Operator operator, Object... values) {
        return new FilterCondition(field, operator, null, List.of(values));
    }

    /** Regression: JSONB operators were applied to text, failing with "operator does not exist". */
    @Test
    void containsAcceptsJsonStringsAndObjects() {
        assertThat(ids(cond("data", Operator.JSON_CONTAINS, "{\"theme\":\"dark\"}"))).containsExactlyInAnyOrder(1L, 3L);
        assertThat(ids(cond("data", Operator.JSON_CONTAINS, Map.of("address", Map.of("city", "Madrid")))))
                .containsExactly(1L);
        assertThat(ids(cond("data", Operator.JSON_CONTAINS, "{\"labels\":[\"vip\"]}"))).containsExactly(1L);
    }

    @Test
    void containedBy() {
        assertThat(ids(cond("data", Operator.JSON_CONTAINED_BY,
                "{\"theme\":\"dark\",\"o'brien\":\"quote\",\"extra\":1}"))).containsExactly(3L);
    }

    @Test
    void keyExistence() {
        assertThat(ids(cond("data", Operator.JSON_EXISTS, "labels"))).containsExactlyInAnyOrder(1L, 2L);
        assertThat(ids(cond("data", Operator.JSON_EXISTS, "o'brien"))).containsExactly(3L);
        assertThat(ids(multi("data", Operator.JSON_EXISTS_ANY, "o'brien", "note"))).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(ids(multi("data", Operator.JSON_EXISTS_ALL, "theme", "note"))).containsExactlyInAnyOrder(1L, 2L);
        assertThat(ids(multi("data", Operator.JSON_EXISTS_ALL, "a\"b", "c\\d"))).isEmpty();
    }

    @Test
    void pathEqualityAndLike() {
        assertThat(ids(multi("data", Operator.JSON_PATH_EQ, "theme", "dark"))).containsExactlyInAnyOrder(1L, 3L);
        assertThat(ids(multi("data", Operator.JSON_PATH_EQ, "address.city", "Paris"))).containsExactly(2L);
        assertThat(ids(multi("data", Operator.JSON_PATH_LIKE, "address.city", "MAD"))).containsExactly(1L);
    }

    @Test
    void pathLikeTreatsWildcardsLiterally() {
        assertThat(ids(multi("data", Operator.JSON_PATH_LIKE, "note", "%"))).containsExactly(1L);
        assertThat(ids(multi("data", Operator.JSON_PATH_LIKE, "note", "k\\s"))).containsExactly(2L);
    }

    /** Regression: the documented [path, value] form was rejected by arity validation. */
    @Test
    void arrayContainsAtRootAndNestedPath() {
        assertThat(ids(cond("tags", Operator.JSON_ARRAY_CONTAINS, "vip"))).containsExactly(1L);
        assertThat(ids(cond("tags", Operator.JSON_ARRAY_CONTAINS, 7))).containsExactly(1L);
        assertThat(ids(multi("data", Operator.JSON_ARRAY_CONTAINS, "labels", "beta"))).containsExactlyInAnyOrder(1L, 2L);
        assertThat(ids(multi("tags", Operator.JSON_ARRAY_CONTAINS_ALL, "vip", "beta"))).containsExactly(1L);
        assertThat(ids(multi("tags", Operator.JSON_ARRAY_CONTAINS_ANY, "vip", "beta"))).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void combinesWithLogicalGroupsAndNullChecks() {
        assertThat(ids(FilterGroup.and(
                multi("data", Operator.JSON_PATH_EQ, "theme", "dark"),
                multi("data", Operator.JSON_ARRAY_CONTAINS, "labels", "vip")))).containsExactly(1L);
        assertThat(ids(cond("data", Operator.IS_NULL, null))).containsExactly(4L);
    }

    /** Regression: malformed JSON reached PostgreSQL and failed with HTTP 500. */
    @Test
    void malformedJsonIsRejected() {
        assertThatThrownBy(() -> ids(cond("data", Operator.JSON_CONTAINS, "{not json")))
                .isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> ids(cond("data", Operator.JSON_CONTAINS, "[1,")))
                .isInstanceOf(FilterTranslationException.class);
    }

    /** Regression: trailing content after a JSON document was silently dropped ("{}" matched every row). */
    @Test
    void trailingContentAfterJsonIsRejected() {
        assertThatThrownBy(() -> ids(cond("data", Operator.JSON_CONTAINS, "{}') OR ('1'='1")))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void injectionAttemptsAreTreatedAsData() {
        assertThat(ids(cond("data", Operator.JSON_CONTAINS, "x' OR '1'='1"))).isEmpty();
        assertThat(ids(cond("data", Operator.JSON_EXISTS, "x') OR 1=1 --"))).isEmpty();
        assertThat(ids(multi("data", Operator.JSON_PATH_EQ, "theme') OR 1=1 --", "dark"))).isEmpty();
        assertThat(ids(multi("data", Operator.JSON_PATH_EQ, "theme", "dark' OR '1'='1"))).isEmpty();
        assertThat(ids(multi("data", Operator.JSON_EXISTS_ANY, "\"}') OR 1=1 --"))).isEmpty();
        assertThat(ids(cond("tags", Operator.JSON_ARRAY_CONTAINS, "\"]') OR 1=1 --"))).isEmpty();
        assertThat(ids(FilterGroup.and())).hasSize(4);
    }

    @Test
    void scalarJsonDocumentsAreSerialized() {
        assertThat(ids(cond("data", Operator.JSON_CONTAINS, "back\\slash"))).isEmpty();
        assertThat(ids(cond("data", Operator.JSON_CONTAINS, 42))).isEmpty();
    }
}
