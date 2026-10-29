import { Injectable, computed, signal } from '@angular/core';
import {
  CourseDemand,
  Planning,
  PlanningStatus,
  Section,
  SectionStatus,
} from '../models/planning.model';

const demand: CourseDemand[] = [
  {
    id: 'c1',
    code: '20118082',
    name: 'Sistemas Distribuidos',
    plan: '2023',
    students: 91,
    capacity: 35,
    calculatedSections: 3,
    proposedSections: 3,
    valid: true,
  },
  {
    id: 'c2',
    code: '20118083',
    name: 'Desarrollo de Sistemas Móviles',
    plan: '2023',
    students: 76,
    capacity: 35,
    calculatedSections: 3,
    proposedSections: 3,
    valid: true,
  },
  {
    id: 'c3',
    code: '2010901',
    name: 'Desarrollo de Tesis I',
    plan: '2018',
    students: 31,
    capacity: 15,
    calculatedSections: 3,
    proposedSections: 3,
    valid: true,
  },
  {
    id: 'c4',
    code: 'INE002',
    name: 'Programación y Computación',
    plan: '2023',
    students: 41,
    capacity: 35,
    calculatedSections: 2,
    proposedSections: 2,
    valid: false,
    issue: 'Código no encontrado en el plan 2023.',
  },
];

const sections: Section[] = [
  {
    id: 's1',
    courseId: 'c1',
    courseCode: '20118082',
    courseName: 'Sistemas Distribuidos',
    code: 'S01',
    students: 35,
    capacity: 35,
    professor: '',
    day: 'Lunes / Miércoles',
    time: '08:00 – 10:00',
    room: 'Por asignar',
    status: 'PROPUESTA',
  },
  {
    id: 's2',
    courseId: 'c1',
    courseCode: '20118082',
    courseName: 'Sistemas Distribuidos',
    code: 'S02',
    students: 35,
    capacity: 35,
    professor: '',
    day: 'Martes / Jueves',
    time: '10:00 – 12:00',
    room: 'Por asignar',
    status: 'PROPUESTA',
  },
  {
    id: 's3',
    courseId: 'c1',
    courseCode: '20118082',
    courseName: 'Sistemas Distribuidos',
    code: 'S03',
    students: 21,
    capacity: 35,
    professor: '',
    day: 'Viernes',
    time: '14:00 – 18:00',
    room: 'Por asignar',
    status: 'PROPUESTA',
  },
  {
    id: 's4',
    courseId: 'c3',
    courseCode: '2010901',
    courseName: 'Desarrollo de Tesis I',
    code: 'S01',
    students: 15,
    capacity: 15,
    professor: '',
    day: 'Miércoles',
    time: '14:00 – 18:00',
    room: 'Por asignar',
    status: 'PROPUESTA',
  },
];

@Injectable({ providedIn: 'root' })
export class PlanningStateService {
  private readonly state = signal<Planning>({
    id: 'demo-planificacion-2026-1',
    period: '2026-I',
    school: 'Escuela Profesional de Ingeniería de Software',
    status: 'BORRADOR',
    createdAt: new Date().toLocaleDateString('es-PE'),
    demand,
    sections,
  });

  readonly planning = this.state.asReadonly();
  readonly validDemand = computed(() => this.state().demand.filter((d) => d.valid));
  readonly invalidDemand = computed(() => this.state().demand.filter((d) => !d.valid));
  readonly proposalCount = computed(() => this.state().sections.length);

  start(period: string) {
    this.state.update((p) => ({ ...p, period, status: 'EN_REVISION' }));
  }
  updateDemand(item: CourseDemand) {
    this.state.update((p) => ({
      ...p,
      demand: p.demand.map((d) =>
        d.id === item.id
          ? { ...item, calculatedSections: Math.ceil(item.students / item.capacity) }
          : d,
      ),
    }));
  }
  updateSection(item: Section) {
    this.state.update((p) => ({
      ...p,
      sections: p.sections.map((s) => (s.id === item.id ? item : s)),
    }));
  }
  validateSections() {
    this.state.update((p) => ({
      ...p,
      status: 'VALIDADA',
      sections: p.sections.map((s) => ({ ...s, status: 'VALIDADA' as SectionStatus })),
    }));
  }
  markExported() {
    this.state.update((p) => ({ ...p, status: 'EXPORTADA' }));
  }
}
