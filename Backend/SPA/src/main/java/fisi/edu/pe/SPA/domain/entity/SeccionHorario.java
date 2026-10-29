package fisi.edu.pe.SPA.domain.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "seccion_horario", schema = "academico", uniqueConstraints = {@UniqueConstraint(columnNames = {"seccion_id", "dia", "hora_inicio", "hora_fin"})})
public class SeccionHorario extends VersionedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

    @Column(name = "dia", nullable = false)
    private Short dia;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    public SeccionHorario() {}

    public Seccion getSeccion() { return seccion; }
    public void setSeccion(Seccion seccion) { this.seccion = seccion; }

    public Short getDia() { return dia; }
    public void setDia(Short dia) { this.dia = dia; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

}
