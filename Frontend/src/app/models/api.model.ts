export interface Escuela {
  id: string;
  codigo: string;
  nombre: string;
}
export interface PeriodoAcademico {
  id: string;
  codigo: string;
  fechaInicio: string;
  fechaFin: string;
}
export interface Planificacion {
  id: string;
  escuelaId: string;
  escuela: string;
  periodoAcademicoId: string;
  periodoAcademico: string;
  creadoPor: string;
  estado: string;
  version: number;
}
export interface FilaImportacion {
  numero: number;
  codigo: string;
  asignatura: string;
  plan: string;
  estudiantesEstimados: number;
  cursoId: string | null;
  valida: boolean;
  error: string | null;
}
export interface VistaPreviaImportacion {
  importacionId: string;
  estado: string;
  totalRegistros: number;
  registrosValidos: number;
  registrosInvalidos: number;
  filas: FilaImportacion[];
}
export interface Horario {
  id?: string;
  dia: number;
  horaInicio: string;
  horaFin: string;
  version?: number;
  local?: boolean;
}
export interface Seccion {
  id?: string;
  codigo: string;
  profesor: string | null;
  capacidad: number;
  cuposPlan2018: number;
  cuposPlan2023: number;
  estado: 'ACTIVA' | 'RETIRADA';
  version?: number;
  horarios: Horario[];
  local?: boolean;
}
export interface Oferta {
  id: string;
  curso: { id: string; codigo: string; nombre: string; ciclo: number };
  estudiantesEstimados: number;
  capacidadAplicada: number;
  seccionesCalculadas: number;
  seccionesPropuestas: number;
  situacion: 'PENDIENTE' | 'OFERTAR' | 'NO_OFERTAR';
  motivoDecision: string | null;
  version: number;
  secciones: Seccion[];
}
