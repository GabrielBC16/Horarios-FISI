package fisi.edu.pe.SPA.domain.entity;

import jakarta.persistence.*;
import fisi.edu.pe.SPA.domain.enums.*;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "cambio_planificacion", schema = "academico")
@Immutable
public class CambioPlanificacion extends UuidEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planificacion_id", nullable = false)
    private Planificacion planificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "entidad", nullable = false, length = 30)
    private EntidadAuditada entidad;

    @Column(name = "registro_id", nullable = false)
    private UUID registroId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operacion", nullable = false, length = 10)
    private OperacionCambio operacion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_anteriores", nullable = true, columnDefinition = "jsonb")
    private Map<String, Object> datosAnteriores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_nuevos", nullable = true, columnDefinition = "jsonb")
    private Map<String, Object> datosNuevos;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_ejecutor_id", nullable = false)
    private Usuario usuarioEjecutor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_autora_id", nullable = false)
    private Area areaAutora;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "intercambio_excel_id", nullable = true)
    private IntercambioExcel intercambioExcel;

    @Column(name = "motivo", nullable = true, columnDefinition = "text")
    private String motivo;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "fecha", nullable = false, insertable = false, updatable = false)
    private Instant fecha;

    public CambioPlanificacion() {}

    public Planificacion getPlanificacion() { return planificacion; }

    public EntidadAuditada getEntidad() { return entidad; }

    public UUID getRegistroId() { return registroId; }

    public OperacionCambio getOperacion() { return operacion; }

    public Map<String, Object> getDatosAnteriores() { return datosAnteriores; }

    public Map<String, Object> getDatosNuevos() { return datosNuevos; }

    public Usuario getUsuarioEjecutor() { return usuarioEjecutor; }

    public Area getAreaAutora() { return areaAutora; }

    public IntercambioExcel getIntercambioExcel() { return intercambioExcel; }

    public String getMotivo() { return motivo; }

    public Instant getFecha() { return fecha; }

}
