import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
@Component({selector:'app-login',imports:[FormsModule],templateUrl:'./login.html',styleUrl:'./login.scss'})
export class LoginPage { private auth=inject(AuthService); private router=inject(Router); email=''; userId=''; password=''; submit(){if(this.email&&this.userId&&this.password){this.auth.login(this.email,this.userId);this.router.navigateByUrl('/planificaciones');}} }
