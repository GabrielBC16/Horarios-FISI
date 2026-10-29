export type PlanningStatus = 'BORRADOR' | 'EN_REVISION' | 'VALIDADA' | 'EXPORTADA';
export type SectionStatus = 'PROPUESTA' | 'VALIDADA' | 'OBSERVADA';

export interface CourseDemand {
  id: string;
  code: string;
  name: string;
  plan: string;
  students: number;
  capacity: number;
  calculatedSections: number;
  proposedSections: number;
  valid: boolean;
  issue?: string;
}

export interface Section {
  id: string;
  courseId: string;
  courseCode: string;
  courseName: string;
  code: string;
  students: number;
  capacity: number;
  professor: string;
  day: string;
  time: string;
  room: string;
  status: SectionStatus;
}

export interface Planning {
  id: string;
  period: string;
  school: string;
  status: PlanningStatus;
  createdAt: string;
  demand: CourseDemand[];
  sections: Section[];
}
