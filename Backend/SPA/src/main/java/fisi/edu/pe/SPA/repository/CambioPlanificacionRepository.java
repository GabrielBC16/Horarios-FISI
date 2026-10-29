package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.CambioPlanificacion;
import fisi.edu.pe.SPA.domain.enums.EntidadAuditada;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CambioPlanificacionRepository extends JpaRepository<CambioPlanificacion, UUID> {
    List<CambioPlanificacion> findByPlanificacion_IdOrderByFechaAsc(UUID planificacionId);
    List<CambioPlanificacion> findByEntidadAndRegistroIdOrderByFechaAsc(EntidadAuditada entidad, UUID registroId);
}
