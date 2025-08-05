import { Component } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-sign-up',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './sign-up.html',
  styleUrl: './sign-up.css',
})
export class SignUp {
  form: FormGroup;
  loading = false;
  showPwd = false;
  showConfirm = false;

  constructor(private fb: FormBuilder, private router: Router) {
    this.form = this.fb.group(
      {
        fullName: ['', [Validators.required, Validators.minLength(2)]],
        email: ['', [Validators.required, Validators.email]],
        password: [
          '',
          [
            Validators.required,
            Validators.minLength(8),
            // at least one letter and one number
            Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/),
          ],
        ],
        confirmPassword: ['', Validators.required],
        acceptTerms: [false, Validators.requiredTrue],
      },
      { validators: this.passwordMatchValidator }
    );
  }

  // Custom validator on the group
  passwordMatchValidator(form: FormGroup) {
    const password = form.get('password')?.value;
    const confirm = form.get('confirmPassword')?.value;
    return password === confirm ? null : { passwordMismatch: true };
  }

  get passwordMismatch() {
    return (
      this.form.hasError('passwordMismatch') &&
      this.form.get('confirmPassword')?.touched
    );
  }

  showError(controlName: string) {
    const control = this.form.get(controlName);
    return control && control.invalid && (control.dirty || control.touched);
  }

  onSubmit() {
    // mark all as touched to show errors if any
    (this.form as any).submitted = true;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;

    // Simulate API call
    setTimeout(() => {
      this.loading = false;

      // For demo only: save minimal info in memory/localStorage
      const { fullName, email } = this.form.value;
      localStorage.setItem('demo_user', JSON.stringify({ fullName, email }));

      // Navigate to login or a welcome page
      this.router.navigateByUrl('/login');
    }, 1500);
  }
}
