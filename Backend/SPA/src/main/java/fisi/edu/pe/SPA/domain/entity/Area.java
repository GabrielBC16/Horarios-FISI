package fisi.edu.pe.SPA.domain.entity;

import jakarta.persistence.*;
import fisi.edu.pe.SPA.domain.enums.TipoArea;

@Entity
@Table(name = "area", schema = "academico", uniqueConstraints = {@UniqueConstraint(columnNames = {"codigo"})})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo", discriminatorType = DiscriminatorType.STRING, length = 40)
public abstract class Area extends UuidEntity {
    @Column(name = "codigo", nullable = false, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 180)
    private String nombre;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    public Area() {}

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    @Transient
    public abstract TipoArea getTipo();

}
