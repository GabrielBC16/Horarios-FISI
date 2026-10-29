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
}
