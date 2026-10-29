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
@Table(name = "intercambio_excel", schema = "academico")
public class IntercambioExcel extends UuidEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planificacion_id", nullable = false)
    private Planificacion planificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 15)
    private TipoIntercambio tipo;

    @Column(name = "numero_ronda", nullable = false)
    private Integer numeroRonda;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "intercambio_origen_id", nullable = true)
    private IntercambioExcel intercambioOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_origen_id", nullable = false)
    private Area areaOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_destino_id", nullable = false)
    private Area areaDestino;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por", nullable = false)
    private Usuario registradoPor;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "archivo_uri", nullable = false, columnDefinition = "text")
    private String archivoUri;

    @Column(name = "version_base", nullable = false)
    private Long versionBase;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_base", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> snapshotBase;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoIntercambio estado = EstadoIntercambio.PENDIENTE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalle_validacion", nullable = true, columnDefinition = "jsonb")
    private Map<String, Object> detalleValidacion;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "registrado_en", nullable = false, insertable = false, updatable = false)
    private Instant registradoEn;

    public IntercambioExcel() {}

    public Planificacion getPlanificacion() { return planificacion; }
    public void setPlanificacion(Planificacion planificacion) { this.planificacion = planificacion; }

    public TipoIntercambio getTipo() { return tipo; }
    public void setTipo(TipoIntercambio tipo) { this.tipo = tipo; }

    public Integer getNumeroRonda() { return numeroRonda; }
    public void setNumeroRonda(Integer numeroRonda) { this.numeroRonda = numeroRonda; }

    public IntercambioExcel getIntercambioOrigen() { return intercambioOrigen; }
    public void setIntercambioOrigen(IntercambioExcel intercambioOrigen) { this.intercambioOrigen = intercambioOrigen; }

    public Area getAreaOrigen() { return areaOrigen; }
    public void setAreaOrigen(Area areaOrigen) { this.areaOrigen = areaOrigen; }

    public Area getAreaDestino() { return areaDestino; }
    public void setAreaDestino(Area areaDestino) { this.areaDestino = areaDestino; }

    public Usuario getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(Usuario registradoPor) { this.registradoPor = registradoPor; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getArchivoUri() { return archivoUri; }
    public void setArchivoUri(String archivoUri) { this.archivoUri = archivoUri; }

    public Long getVersionBase() { return versionBase; }
    public void setVersionBase(Long versionBase) { this.versionBase = versionBase; }

    public Map<String, Object> getSnapshotBase() { return snapshotBase; }
    public void setSnapshotBase(Map<String, Object> snapshotBase) { this.snapshotBase = snapshotBase; }

    public EstadoIntercambio getEstado() { return estado; }
    public void setEstado(EstadoIntercambio estado) { this.estado = estado; }

    public Map<String, Object> getDetalleValidacion() { return detalleValidacion; }
    public void setDetalleValidacion(Map<String, Object> detalleValidacion) { this.detalleValidacion = detalleValidacion; }

    public Instant getRegistradoEn() { return registradoEn; }

}
