package com.safezone.platform.reporting.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_reports")
@Getter
@Setter
@NoArgsConstructor
public class IncidentReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String warehouseName;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String status;

    private String description;

    public IncidentReport(String warehouseName, String eventType, LocalDateTime timestamp, String status, String description) {
        this.warehouseName = warehouseName;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.status = status;
        this.description = description;
    }
}
