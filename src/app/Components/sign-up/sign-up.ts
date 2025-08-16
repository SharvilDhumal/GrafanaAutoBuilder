import { Component } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService, SignupRequest } from '../../services/auth.service';
import { Navbar } from '../../Components/navbar/navbar';
import { Footer } from '../../Components/footer/footer';

@Component({
  selector: 'app-sign-up',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, Navbar, Footer],
  templateUrl: './sign-up.html',
  styleUrls: ['./sign-up.css'],
})
export class SignUp {
  form: FormGroup;
  loading = false;
  showPwd = false;
  showConfirm = false;
  errorMessage: string | null = null;

  constructor(private fb: FormBuilder, private router: Router, private authService: AuthService) {
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
    this.errorMessage = null; // Clear previous errors

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;

    const signupRequest: SignupRequest = {
      email: this.form.value.email,
      password: this.form.value.password
    };

    this.authService.signup(signupRequest).subscribe({
      next: (response) => {
        console.log('Signup successful!', response);
        this.loading = false;
        // Navigate to login page with success message
        this.router.navigate(['/login'], { 
          state: { message: 'Registration successful! Please check your email to verify your account.' } 
        });
      },
      error: (error) => {
        console.error('Signup failed:', error);
        this.loading = false;
        this.errorMessage = error.message || 'An error occurred during signup. Please try again.';
      }
    });
  }
}
