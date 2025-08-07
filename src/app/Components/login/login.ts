import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService, LoginRequest } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, CommonModule, RouterLink], // <-- Add RouterLink here
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  // Form properties
  email: string = '';
  password: string = '';
  rememberMe: boolean = false;
  showPassword: boolean = false;
  isLoading: boolean = false;
  loginError: string = '';

  constructor(private router: Router, private authService: AuthService) {}

  onSubmit() {
    if (this.isLoading) return;

    this.isLoading = true;
    this.loginError = '';

    const loginRequest: LoginRequest = {
      email: this.email,
      password: this.password
    };

    this.authService.login(loginRequest).subscribe({
      next: (response) => {
        console.log('Login successful!', response);
        // Navigate to dashboard or home page
        this.router.navigate(['/']);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Login failed:', error);
        this.loginError = 'Invalid email or password. Please try again.';
        this.isLoading = false;
      }
    });
  }

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  onBack() {
    // Navigate back to home page
    this.router.navigate(['/']);
  }

  onTryDemo() {
    // Navigate to demo or fill demo credentials
    this.email = 'demo@example.com';
    this.password = 'password';
  }
}
