package fisi.edu.pe.SPA.domain.entity;

import jakarta.persistence.*;
import java.util.UUID;

@MappedSuperclass
public abstract class UuidEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    public UUID getId() { return id; }
}
