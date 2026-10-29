package fisi.edu.pe.SPA.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario", schema = "academico", uniqueConstraints = {@UniqueConstraint(columnNames = {"identificador"})})
public class Usuario extends UuidEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(name = "identificador", nullable = false, length = 180)
    private String identificador;

    @Column(name = "nombre", nullable = false, length = 180)
    private String nombre;

    @Column(name = "rol", nullable = false, length = 50)
    private String rol;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    public Usuario() {}

    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }

    public String getIdentificador() { return identificador; }
    public void setIdentificador(String identificador) { this.identificador = identificador; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

}
