import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  Escuela,
  Horario,
  Oferta,
  PeriodoAcademico,
  Planificacion,
  Seccion,
  VistaPreviaImportacion,
} from '../models/api.model';

@Injectable({ providedIn: 'root' })
export class PlanningApiService {
  private readonly http = inject(HttpClient);
  private readonly base = 'http://192.168.30.194:8080/api';

  escuelas() {
    console.log('Consultando escuelas a la API...');
    return this.http.get<Escuela[]>(`${this.base}/escuelas`);
  }

  periodos() {
    console.log('Consultando periodos academicos a la API...');
    return this.http.get<PeriodoAcademico[]>(`${this.base}/periodos-academicos`);
  }

  crearPlanificacion(body: { escuelaId: string; periodoAcademicoId: string; usuarioId: string }) {
    console.log('Creando planificacion en la API...');
    return this.http.post<Planificacion>(`${this.base}/planificaciones`, body);
  }

  obtenerPlanificacion(id: string) {
    console.log('Obteniendo planificacion de la API...');
    return this.http.get<Planificacion>(`${this.base}/planificaciones/${id}`);
  }

  listarPlanificaciones(escuelaId: string) {
    console.log('Listando planificaciones de la API...');
    return this.http.get<Planificacion[]>(`${this.base}/planificaciones/escuela/${escuelaId}`);
  }

  importarDemanda(planificacionId: string, usuarioId: string, pdf: File) {
    console.log('Importando demanda a la API...');
    const f = new FormData();
    f.append('archivo', pdf);
    f.append('usuarioId', usuarioId);
    return this.http.post<VistaPreviaImportacion>(
      `${this.base}/planificaciones/${planificacionId}/importaciones/demanda`,
      f,
    );
  }

  obtenerImportacion(id: string) {
    console.log('Obteniendo importacion de la API...');
    return this.http.get<VistaPreviaImportacion>(`${this.base}/importaciones/${id}`);
  }

  confirmarImportacion(id: string) {
    console.log('Confirmando importacion en la API...');
    return this.http.post<{
      importacionId: string;
      ofertasActualizadas: number;
      seccionesPropuestas: number;
    }>(`${this.base}/importaciones/${id}/confirmar`, null);
  }

  programacion(id: string) {
    console.log('Obteniendo programacion de la API...');
    return this.http.get<Oferta[]>(`${this.base}/validar/verSeccionesCalculadas/${id}`);
  }

  actualizarOferta(planId: string, oferta: Oferta, usuarioId: string) {
    console.log('Actualizando oferta en la API...');
    return this.http.put<Oferta>(`${this.base}/planificaciones/${planId}/ofertas/${oferta.id}`, {
      usuarioId,
      version: oferta.version,
      estudiantesEstimados: oferta.estudiantesEstimados,
      capacidadAplicada: oferta.capacidadAplicada,
      seccionesPropuestas: oferta.seccionesPropuestas,
      situacion: oferta.situacion,
      motivoDecision: oferta.motivoDecision,
    });
  }

  crearSeccion(planId: string, ofertaId: string, section: Seccion, usuarioId: string) {
    console.log('Creando seccion en la API...');
    return this.http.post<Seccion>(
      `${this.base}/planificaciones/${planId}/ofertas/${ofertaId}/secciones`,
      {
        usuarioId,
        codigo: section.codigo,
        profesor: section.profesor,
        capacidad: section.capacidad,
        cuposPlan2018: section.cuposPlan2018,
        cuposPlan2023: section.cuposPlan2023,
      },
    );
  }

  actualizarSeccion(planId: string, section: Seccion, usuarioId: string) {
    console.log('Actualizando seccion en la API...');
    return this.http.put<Seccion>(
      `${this.base}/planificaciones/${planId}/secciones/${section.id}`,
      {
        usuarioId,
        version: section.version,
        codigo: section.codigo,
        profesor: section.profesor,
        capacidad: section.capacidad,
        cuposPlan2018: section.cuposPlan2018,
        cuposPlan2023: section.cuposPlan2023,
        estado: section.estado,
      },
    );
  }

  retirarSeccion(planId: string, section: Seccion, usuarioId: string) {
    console.log('Retirando seccion de la API...');
    return this.http.delete<Seccion>(
      `${this.base}/planificaciones/${planId}/secciones/${section.id}`,
      { params: { usuarioId, version: section.version! } },
    );
  }

  crearHorario(planId: string, sectionId: string, item: Horario, usuarioId: string) {
    console.log('Creando horario en la API...');
    return this.http.post<Horario>(
      `${this.base}/planificaciones/${planId}/secciones/${sectionId}/horarios`,
      { usuarioId, dia: Number(item.dia), horaInicio: item.horaInicio, horaFin: item.horaFin },
    );
  }

  actualizarHorario(planId: string, sectionId: string, item: Horario, usuarioId: string) {
    console.log('Actualizando horario en la API...');
    return this.http.put<Horario>(
      `${this.base}/planificaciones/${planId}/secciones/${sectionId}/horarios/${item.id}`,
      {
        usuarioId,
        version: item.version,
        dia: Number(item.dia),
        horaInicio: item.horaInicio,
        horaFin: item.horaFin,
      },
    );
  }

  eliminarHorario(planId: string, sectionId: string, item: Horario, usuarioId: string) {
    console.log('Eliminando horario de la API...');
    return this.http.delete<void>(
      `${this.base}/planificaciones/${planId}/secciones/${sectionId}/horarios/${item.id}`,
      { params: { usuarioId } },
    );
  }

  descargarExcel(planId: string) {
    console.log('Descargando Excel de la API...');
    return this.http.get(`${this.base}/planificaciones/${planId}/exportaciones/cupos`, {
      responseType: 'blob',
    });
  }
}
