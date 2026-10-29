package fisi.edu.pe.SPA.domain.entity;

import jakarta.persistence.*;
import fisi.edu.pe.SPA.domain.enums.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "oferta_curso", schema = "academico", uniqueConstraints = {@UniqueConstraint(columnNames = {"planificacion_id", "curso_id"})})
public class OfertaCurso extends VersionedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planificacion_id", nullable = false)
    private Planificacion planificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "importacion_id", nullable = true)
    private Importacion importacion;

    @Column(name = "estudiantes_estimados", nullable = false)
    private Integer estudiantesEstimados;

    @Column(name = "capacidad_aplicada", nullable = false)
    private Integer capacidadAplicada;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "secciones_calculadas", nullable = false, insertable = false, updatable = false)
    private Integer seccionesCalculadas;

    @Column(name = "secciones_propuestas", nullable = false)
    private Integer seccionesPropuestas;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacion", nullable = false, length = 20)
    private SituacionOferta situacion = SituacionOferta.PENDIENTE;

    @Column(name = "motivo_decision", nullable = true, columnDefinition = "text")
    private String motivoDecision;

    public OfertaCurso() {}

    public Planificacion getPlanificacion() { return planificacion; }
    public void setPlanificacion(Planificacion planificacion) { this.planificacion = planificacion; }

    public Curso getCurso() { return curso; }
    public void setCurso(Curso curso) { this.curso = curso; }

    public Importacion getImportacion() { return importacion; }
    public void setImportacion(Importacion importacion) { this.importacion = importacion; }

    public Integer getEstudiantesEstimados() { return estudiantesEstimados; }
    public void setEstudiantesEstimados(Integer estudiantesEstimados) { this.estudiantesEstimados = estudiantesEstimados; }

    public Integer getCapacidadAplicada() { return capacidadAplicada; }
    public void setCapacidadAplicada(Integer capacidadAplicada) { this.capacidadAplicada = capacidadAplicada; }

    public Integer getSeccionesCalculadas() { return seccionesCalculadas; }

    public Integer getSeccionesPropuestas() { return seccionesPropuestas; }
    public void setSeccionesPropuestas(Integer seccionesPropuestas) { this.seccionesPropuestas = seccionesPropuestas; }

    public SituacionOferta getSituacion() { return situacion; }
    public void setSituacion(SituacionOferta situacion) { this.situacion = situacion; }

    public String getMotivoDecision() { return motivoDecision; }
    public void setMotivoDecision(String motivoDecision) { this.motivoDecision = motivoDecision; }

}
