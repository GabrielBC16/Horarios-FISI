package fisi.edu.pe.SPA.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/** Valores administrados por los defaults y triggers del esquema academico. */
@MappedSuperclass
public abstract class VersionedEntity extends UuidEntity {
    @Version
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "version_registro", nullable = false, insertable = false, updatable = false)
    private Long versionRegistro;

    @Generated(event = EventType.INSERT)
    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private Instant creadoEn;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "actualizado_en", nullable = false, insertable = false, updatable = false)
    private Instant actualizadoEn;

    public Long getVersionRegistro() { return versionRegistro; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
