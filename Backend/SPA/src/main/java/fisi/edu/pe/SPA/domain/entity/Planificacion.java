package fisi.edu.pe.SPA.domain.entity;

import jakarta.persistence.*;
import fisi.edu.pe.SPA.domain.enums.*;

@Entity
@Table(name = "planificacion", schema = "academico", uniqueConstraints = {@UniqueConstraint(columnNames = {"area_escuela_id", "periodo_academico_id"})})
public class Planificacion extends VersionedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_escuela_id", nullable = false)
    private Escuela areaEscuela;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "periodo_academico_id", nullable = false)
    private PeriodoAcademico periodoAcademico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por", nullable = false)
    private Usuario creadoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoPlanificacion estado = EstadoPlanificacion.BORRADOR;

    public Planificacion() {}

    public Escuela getAreaEscuela() { return areaEscuela; }
    public void setAreaEscuela(Escuela areaEscuela) { this.areaEscuela = areaEscuela; }

    public PeriodoAcademico getPeriodoAcademico() { return periodoAcademico; }
    public void setPeriodoAcademico(PeriodoAcademico periodoAcademico) { this.periodoAcademico = periodoAcademico; }

    public Usuario getCreadoPor() { return creadoPor; }
    public void setCreadoPor(Usuario creadoPor) { this.creadoPor = creadoPor; }

    public EstadoPlanificacion getEstado() { return estado; }
    public void setEstado(EstadoPlanificacion estado) { this.estado = estado; }

}
