import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router'; // <-- Import RouterLink

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

  constructor(private router: Router) {}

  onSubmit() {
    if (this.isLoading) return;

    this.isLoading = true;
    this.loginError = '';

    // Simulate login process (replace with actual authentication logic)
    setTimeout(() => {
      // Mock validation
      if (this.email === 'demo@example.com' && this.password === 'password') {
        console.log('Login successful!');
        // Navigate to dashboard or home page
        // this.router.navigate(['/dashboard']);
      } else {
        this.loginError = 'Invalid email or password. Please try again.';
      }
      this.isLoading = false;
    }, 1500);
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
