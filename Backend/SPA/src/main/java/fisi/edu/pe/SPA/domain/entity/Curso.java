package fisi.edu.pe.SPA.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "curso", schema = "academico", uniqueConstraints = {@UniqueConstraint(columnNames = {"plan_id", "codigo"})})
public class Curso extends UuidEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanCurricular plan;

    @Column(name = "codigo", nullable = false, length = 40)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "ciclo", nullable = false)
    private Short ciclo;

    @Column(name = "capacidad_seccion_default", nullable = false)
    private Integer capacidadSeccionDefault;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    public Curso() {}

    public PlanCurricular getPlan() { return plan; }
    public void setPlan(PlanCurricular plan) { this.plan = plan; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Short getCiclo() { return ciclo; }
    public void setCiclo(Short ciclo) { this.ciclo = ciclo; }

    public Integer getCapacidadSeccionDefault() { return capacidadSeccionDefault; }
    public void setCapacidadSeccionDefault(Integer capacidadSeccionDefault) { this.capacidadSeccionDefault = capacidadSeccionDefault; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

}
