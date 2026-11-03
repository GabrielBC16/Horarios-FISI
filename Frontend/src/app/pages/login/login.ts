import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly loading = signal(false);
  readonly error = signal('');
  correo = '';
  clave = '';

  submit() {
    this.error.set('');
    this.loading.set(true);
    this.auth.login(this.correo, this.clave).subscribe({
      next: () => {
        this.loading.set(false);
        void this.router.navigateByUrl('/planificaciones');
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo iniciar sesión. Revisa tus credenciales e inténtalo de nuevo.');
      },
    });
  }
}
