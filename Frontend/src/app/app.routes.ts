import { Routes } from '@angular/router';
import { LoginPage } from './pages/login/login';
import { DashboardPage } from './pages/dashboard/dashboard';
import { CreatePlanningPage } from './pages/create-planning/create-planning';
import { ImportDemandPage } from './pages/import-demand/import-demand';
import { ReviewImportPage } from './pages/review-import/review-import';
import { ProgrammingPage } from './pages/programming/programming';
import { ExportPage } from './pages/export/export';
export const routes: Routes = [
  { path: 'login', component: LoginPage },
  { path: 'planificaciones', component: DashboardPage },
  { path: 'planificaciones/nueva', component: CreatePlanningPage },
  { path: 'planificaciones/:id/importar', component: ImportDemandPage },
  { path: 'importaciones/:id/revision', component: ReviewImportPage },
  { path: 'planificaciones/:id/programacion', component: ProgrammingPage },
  { path: 'planificaciones/:id/exportar', component: ExportPage },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: '**', redirectTo: 'login' },
];
