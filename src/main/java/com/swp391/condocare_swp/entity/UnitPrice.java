package com.swp391.condocare_swp.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "unit_price", schema = "swp391")
public class UnitPrice {
    @Id
    @Column(name = "ID", nullable = false, length = 10)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Lob
    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "amount", nullable = false)
    private Float amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "update_by", nullable = false)
    private User updateBy;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "update_at", nullable = false)
    private Instant updateAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Float getAmount() {
        return amount;
    }

    public void setAmount(Float amount) {
        this.amount = amount;
    }

    public User getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(User updateBy) {
        this.updateBy = updateBy;
    }

    public Instant getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(Instant updateAt) {
        this.updateAt = updateAt;
    }

}