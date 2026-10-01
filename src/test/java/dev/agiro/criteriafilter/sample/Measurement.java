package dev.agiro.criteriafilter.sample;

import dev.agiro.criteriafilter.annotation.CriteriaFilter;
import dev.agiro.criteriafilter.annotation.FilterField;
import dev.agiro.criteriafilter.model.Backend;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Sample entity covering the remaining coercible data types and an association.
 */
@Entity
@CriteriaFilter(backend = Backend.JPA)
public class Measurement {

    @Id
    @FilterField
    private Long id;

    @FilterField
    private String label;

    @FilterField
    private Double reading;

    @FilterField
    @Column(name = "item_count")
    private Integer count;

    @FilterField
    private Boolean flagged;

    @FilterField
    @Column(name = "measured_day")
    private LocalDate day;

    @FilterField
    private OffsetDateTime takenAt;

    @FilterField
    private UUID ref;

    @FilterField
    @ManyToOne
    private Product product;

    protected Measurement() {
    }

    public Measurement(Long id, String label, Double reading, Integer count, Boolean flagged,
                       LocalDate day, OffsetDateTime takenAt, UUID ref, Product product) {
        this.id = id;
        this.label = label;
        this.reading = reading;
        this.count = count;
        this.flagged = flagged;
        this.day = day;
        this.takenAt = takenAt;
        this.ref = ref;
        this.product = product;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
