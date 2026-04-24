package com.kritik.POS.order.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.util.Objects;

@Entity
@Data
public class OrderTax {
    @Id
    @GeneratedValue
    private Long id;
    private String taxName;
    private Double taxAmount;
    @ManyToOne
    @JoinColumn(nullable = false,updatable = false)
    @ToString.Exclude
    private Order order;

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        OrderTax orderTax = (OrderTax) o;
        return id != null && Objects.equals(id, orderTax.id);
    }

    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
