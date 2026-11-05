import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-create-user',
  imports: [FormsModule, RouterLink],
  templateUrl: './create-user.html',
})
export class CreateUserPage {
  private readonly auth = inject(AuthService);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly success = signal(false);
  readonly showPassword = signal(false);
  email = '';
  clave = '';
  nombre = '';
  area = '';
  rol = '';

  togglePasswordVisibility() {
    this.showPassword.update((visible) => !visible);
  }

  submit() {
    this.error.set('');
    this.success.set(false);
    this.loading.set(true);
    this.auth
      .crearUsuario({
        email: this.email,
        clave: this.clave,
        nombre: this.nombre,
        area: this.area,
        rol: this.rol,
      })
      .subscribe({
        next: (created) => {
          this.loading.set(false);
          if (created) {
            this.success.set(true);
          } else {
            this.error.set('No se pudo crear el usuario. Verifica los datos e inténtalo de nuevo.');
          }
        },
        error: () => {
          this.loading.set(false);
          this.error.set('No se pudo crear el usuario. Verifica los datos e inténtalo de nuevo.');
        },
      });
  }
}
