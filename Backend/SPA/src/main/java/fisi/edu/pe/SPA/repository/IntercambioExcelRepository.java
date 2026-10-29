package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.IntercambioExcel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntercambioExcelRepository extends JpaRepository<IntercambioExcel, UUID> {
    List<IntercambioExcel> findByPlanificacion_IdOrderByNumeroRondaAscRegistradoEnAsc(UUID planificacionId);
}
