package dev.agiro.criteriafilter.sample;

import dev.agiro.criteriafilter.annotation.CriteriaFilter;
import dev.agiro.criteriafilter.annotation.FilterField;
import dev.agiro.criteriafilter.model.Backend;
import dev.agiro.criteriafilter.model.Operator;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@CriteriaFilter(backend = Backend.JPA)
public class Product {

    @Id
    @FilterField
    private Long id;

    @FilterField
    private String name;

    @FilterField
    private BigDecimal price;

    @FilterField(name = "category")
    @Enumerated(EnumType.STRING)
    private Category category;

    @FilterField
    private Instant createdAt;

    @FilterField(operators = {Operator.EQ})
    private boolean active;

    // Not filterable.
    private String internalNote;

    protected Product() {
    }

    public Product(Long id, String name, BigDecimal price, Category category, Instant createdAt, boolean active) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.createdAt = createdAt;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Category getCategory() {
        return category;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public enum Category {
        BOOK, TOY, FOOD
    }
}
