package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.IntercambioExcel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntercambioExcelRepository extends JpaRepository<IntercambioExcel, UUID> {
    List<IntercambioExcel> findByPlanificacion_IdOrderByNumeroRondaAscRegistradoEnAsc(UUID planificacionId);

    @Modifying
    @Query("delete from IntercambioExcel intercambio where intercambio.planificacion.id = :planificacionId")
    int deleteAllByPlanificacionId(@Param("planificacionId") UUID planificacionId);
}
