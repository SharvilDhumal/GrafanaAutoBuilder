import { Component } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { Navbar } from '../navbar/navbar';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, Navbar],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css'
})
export class ForgotPassword {
  form: FormGroup;
  loading = false;
  emailSent = false;
  errorMessage: string | null = null;

  constructor(private fb: FormBuilder, private router: Router, private auth: AuthService) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  showError(field: string): boolean {
    const control = this.form.get(field);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  onSubmit() {
    if (this.form.valid && !this.loading) {
      this.loading = true;
      this.errorMessage = null;
      const email = this.form.value.email as string;
      this.auth.forgotPassword(email).subscribe({
        next: () => {
          this.loading = false;
          this.emailSent = true;
        },
        error: (err) => {
          // For security, still show success but log error; optionally surface a generic error
          console.error('Forgot password failed', err);
          this.loading = false;
          this.emailSent = true;
        }
      });
    } else {
      // Mark all fields as touched to show validation errors
      Object.keys(this.form.controls).forEach(key => {
        this.form.get(key)?.markAsTouched();
      });
    }
  }

  onBack() {
    this.router.navigate(['/login']);
  }
}
