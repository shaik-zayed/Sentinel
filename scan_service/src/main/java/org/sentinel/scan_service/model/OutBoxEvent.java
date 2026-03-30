package org.sentinel.scan_service.model;

import jakarta.persistence.*;

@Entity
@Table(name = "outbox_event")
public class OutBoxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long outBoxItemId;

    private String aggregatorType;
}
