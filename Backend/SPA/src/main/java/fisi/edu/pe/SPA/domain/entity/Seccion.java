package fisi.edu.pe.SPA.domain.entity;

import jakarta.persistence.*;
import fisi.edu.pe.SPA.domain.enums.*;

@Entity
@Table(name = "seccion", schema = "academico", uniqueConstraints = {@UniqueConstraint(columnNames = {"oferta_curso_id", "codigo"})})
public class Seccion extends VersionedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oferta_curso_id", nullable = false)
    private OfertaCurso ofertaCurso;

    @Column(name = "codigo", nullable = false, length = 30)
    private String codigo;

    @Column(name = "profesor", nullable = true, length = 200)
    private String profesor;

    @Column(name = "capacidad", nullable = false)
    private Integer capacidad = 35;

    @Column(name = "cupos_plan_2018", nullable = false)
    private Integer cuposPlan2018 = 0;

    @Column(name = "cupos_plan_2023", nullable = false)
    private Integer cuposPlan2023 = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoSeccion estado = EstadoSeccion.ACTIVA;

    public Seccion() {}

    public OfertaCurso getOfertaCurso() { return ofertaCurso; }
    public void setOfertaCurso(OfertaCurso ofertaCurso) { this.ofertaCurso = ofertaCurso; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getProfesor() { return profesor; }
    public void setProfesor(String profesor) { this.profesor = profesor; }

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public Integer getCuposPlan2018() { return cuposPlan2018; }
    public void setCuposPlan2018(Integer cuposPlan2018) { this.cuposPlan2018 = cuposPlan2018; }

    public Integer getCuposPlan2023() { return cuposPlan2023; }
    public void setCuposPlan2023(Integer cuposPlan2023) { this.cuposPlan2023 = cuposPlan2023; }

    public EstadoSeccion getEstado() { return estado; }
    public void setEstado(EstadoSeccion estado) { this.estado = estado; }

}
