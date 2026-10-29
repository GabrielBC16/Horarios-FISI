package fisi.edu.pe.SPA.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "plan_curricular", schema = "academico", uniqueConstraints = {@UniqueConstraint(columnNames = {"area_escuela_id", "codigo"})})
public class PlanCurricular extends UuidEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_escuela_id", nullable = false)
    private Escuela areaEscuela;

    @Column(name = "codigo", nullable = false, length = 40)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 180)
    private String nombre;

    @Column(name = "anio", nullable = true)
    private Short anio;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    public PlanCurricular() {}

    public Escuela getAreaEscuela() { return areaEscuela; }
    public void setAreaEscuela(Escuela areaEscuela) { this.areaEscuela = areaEscuela; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Short getAnio() { return anio; }
    public void setAnio(Short anio) { this.anio = anio; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

}
