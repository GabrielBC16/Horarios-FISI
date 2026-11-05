import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { PlanningApiService } from '../../core/planning-api.service';
import { Escuela, Planificacion } from '../../models/api.model';

@Component({
  selector: 'app-dashboard',
  imports: [FormsModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardPage {
  readonly auth = inject(AuthService);
  private readonly api = inject(PlanningApiService);

  readonly schools = signal<Escuela[]>([]);
  readonly plans = signal<Planificacion[]>([]);
  readonly loadingSchools = signal(true);
  readonly loadingPlans = signal(false);
  readonly deletingPlanId = signal<string | null>(null);
  readonly error = signal('');
  schoolId = '';

  constructor() {
    console.log('Cargando escuelas...');
    this.api.escuelas().subscribe({
      next: (schools) => {
        this.schools.set(schools);
        this.loadingSchools.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las escuelas.');
        this.loadingSchools.set(false);
        console.log('Error al cargar escuelas de la API');
      },
    });
  }

  loadPlans(schoolId: string) {
    this.schoolId = schoolId;
    this.plans.set([]);
    this.error.set('');
    if (!schoolId) {
      return;
    }

    this.loadingPlans.set(true);
    this.api.listarPlanificaciones(schoolId).subscribe({
      next: (plans) => {
        this.plans.set(plans);
        this.loadingPlans.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las planificaciones de esta escuela.');
        this.loadingPlans.set(false);
      },
    });
  }

  deletePlan(plan: Planificacion) {
    const usuarioId = this.auth.userId();
    if (!usuarioId) {
      this.error.set('Falta el UUID del usuario activo para eliminar la planificación.');
      return;
    }

    const confirmed = window.confirm(
      `¿Eliminar la planificación del periodo ${plan.periodoAcademico}? Esta acción no se puede deshacer.`,
    );
    if (!confirmed) {
      return;
    }

    this.error.set('');
    this.deletingPlanId.set(plan.id);
    this.api.eliminarPlanificacion(plan.id, usuarioId).subscribe({
      next: () => {
        this.plans.update((plans) => plans.filter((item) => item.id !== plan.id));
        this.deletingPlanId.set(null);
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error, 'No se pudo eliminar la planificación.'));
        this.deletingPlanId.set(null);
      },
    });
  }

  private errorMessage(error: unknown, fallback: string): string {
    if (
      typeof error === 'object' &&
      error !== null &&
      'error' in error &&
      typeof error.error === 'object' &&
      error.error !== null &&
      'message' in error.error &&
      typeof error.error.message === 'string'
    ) {
      return error.error.message;
    }
    return fallback;
  }
}
