import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { PlanningApiService } from '../../core/planning-api.service';
import { Escuela, PeriodoAcademico } from '../../models/api.model';
@Component({
  selector: 'app-create-planning',
  imports: [FormsModule, RouterLink],
  templateUrl: './create-planning.html',
  styleUrl: './create-planning.scss',
})
export class CreatePlanningPage {
  private api = inject(PlanningApiService);
  private auth = inject(AuthService);
  private router = inject(Router);
  schools = signal<Escuela[]>([]);
  periods = signal<PeriodoAcademico[]>([]);
  schoolId = '';
  periodId = '';
  error = signal('');
  loading = signal(false);
  constructor() {
    this.api.escuelas().subscribe({
      next: (v) => this.schools.set(v),
      error: () => this.error.set('No se pudieron cargar las escuelas.'),
    });
    this.api.periodos().subscribe({
      next: (v) => this.periods.set(v),
      error: () => this.error.set('No se pudieron cargar los periodos.'),
    });
  }
  create() {
    if (!this.auth.userId()) {
      this.error.set('Ingresa con un UUID de usuario válido.');
      return;
    }
    this.loading.set(true);
    this.api
      .crearPlanificacion({
        escuelaId: this.schoolId,
        periodoAcademicoId: this.periodId,
        usuarioId: this.auth.userId(),
      })
      .subscribe({
        next: (p) => this.router.navigate(['/planificaciones', p.id, 'importar']),
        error: (e) => {
          this.error.set(e.error?.message || 'No se pudo crear la planificación.');
          this.loading.set(false);
        },
      });
  }
}
