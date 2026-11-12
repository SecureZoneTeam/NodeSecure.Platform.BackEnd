package com.safezone.platform.subscriptions.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String planName;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer maxWarehouses;

    @Column(nullable = false)
    private Integer maxSensors;

    @Column(nullable = false)
    private String status;

    public Subscription(String planName, Double price, Integer maxWarehouses, Integer maxSensors, String status) {
        this.planName = planName;
        this.price = price;
        this.maxWarehouses = maxWarehouses;
        this.maxSensors = maxSensors;
        this.status = status;
    }
}
