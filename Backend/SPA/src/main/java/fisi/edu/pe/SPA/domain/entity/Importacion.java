package fisi.edu.pe.SPA.domain.entity;

import jakarta.persistence.*;
import fisi.edu.pe.SPA.domain.enums.*;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "importacion", schema = "academico")
public class Importacion extends UuidEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planificacion_id", nullable = false)
    private Planificacion planificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "importado_por", nullable = false)
    private Usuario importadoPor;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "archivo_uri", nullable = false, columnDefinition = "text")
    private String archivoUri;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoImportacion estado = EstadoImportacion.PENDIENTE;

    @Column(name = "total_registros", nullable = false)
    private Integer totalRegistros = 0;

    @Column(name = "registros_validos", nullable = false)
    private Integer registrosValidos = 0;

    @Column(name = "registros_invalidos", nullable = false)
    private Integer registrosInvalidos = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalle_validacion", nullable = true, columnDefinition = "jsonb")
    private Map<String, Object> detalleValidacion;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "importado_en", nullable = false, insertable = false, updatable = false)
    private Instant importadoEn;

    public Importacion() {}

    public Planificacion getPlanificacion() { return planificacion; }
    public void setPlanificacion(Planificacion planificacion) { this.planificacion = planificacion; }

    public Usuario getImportadoPor() { return importadoPor; }
    public void setImportadoPor(Usuario importadoPor) { this.importadoPor = importadoPor; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getArchivoUri() { return archivoUri; }
    public void setArchivoUri(String archivoUri) { this.archivoUri = archivoUri; }

    public EstadoImportacion getEstado() { return estado; }
    public void setEstado(EstadoImportacion estado) { this.estado = estado; }

    public Integer getTotalRegistros() { return totalRegistros; }
    public void setTotalRegistros(Integer totalRegistros) { this.totalRegistros = totalRegistros; }

    public Integer getRegistrosValidos() { return registrosValidos; }
    public void setRegistrosValidos(Integer registrosValidos) { this.registrosValidos = registrosValidos; }

    public Integer getRegistrosInvalidos() { return registrosInvalidos; }
    public void setRegistrosInvalidos(Integer registrosInvalidos) { this.registrosInvalidos = registrosInvalidos; }

    public Map<String, Object> getDetalleValidacion() { return detalleValidacion; }
    public void setDetalleValidacion(Map<String, Object> detalleValidacion) { this.detalleValidacion = detalleValidacion; }

    public Instant getImportadoEn() { return importadoEn; }

}
