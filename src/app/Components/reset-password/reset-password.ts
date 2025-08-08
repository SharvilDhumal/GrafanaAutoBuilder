import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrls: ['./reset-password.css']
})
export class ResetPassword {
  form: FormGroup;
  loading = false;
  success = false;
  errorMessage: string | null = null;

  // Pre-populated from query params if present
  tokenFromLink: string | null = null;
  emailFromLink: string | null = null;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private auth: AuthService
  ) {
    const qp = this.route.snapshot.queryParamMap;
    this.tokenFromLink = qp.get('token');
    this.emailFromLink = qp.get('email');

    this.form = this.fb.group({
      email: [this.emailFromLink ?? '', [Validators.required, Validators.email]],
      token: [this.tokenFromLink ?? '', [Validators.required]],
      newPassword: ['', [
        Validators.required,
        Validators.minLength(8),
        // at least one lowercase, one uppercase, and one digit
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/)
      ]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordsMatchValidator });
  }

  private passwordsMatchValidator(group: FormGroup) {
    const p1 = group.get('newPassword')?.value;
    const p2 = group.get('confirmPassword')?.value;
    return p1 && p2 && p1 === p2 ? null : { passwordsMismatch: true };
  }

  showError(field: string): boolean {
    const control = this.form.get(field);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  onSubmit() {
    if (this.form.invalid || this.loading) {
      Object.keys(this.form.controls).forEach(key => this.form.get(key)?.markAsTouched());
      return;
    }

    this.loading = true;
    this.errorMessage = null;

    const email = this.form.value.email as string;
    const token = this.form.value.token as string;
    const newPassword = this.form.value.newPassword as string;

    this.auth.resetPassword(email, token, newPassword).subscribe({
      next: () => {
        this.loading = false;
        this.success = true;
      },
      error: (err) => {
        console.error('Reset password failed', err);
        this.loading = false;
        // Show backend message if provided; otherwise generic error
        this.errorMessage = err?.message || 'Failed to reset password. Please try again.';
      }
    });
  }

  backToLogin() {
    this.router.navigate(['/login']);
  }
}
