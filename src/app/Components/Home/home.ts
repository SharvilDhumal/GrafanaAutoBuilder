import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { GlobalErrorService } from '../../core/errors/global-error.service';
import { Navbar } from '../../Components/navbar/navbar';
import { Footer } from '../../Components/footer/footer';

@Component({
  selector: 'app-home',
  imports: [CommonModule, Navbar, Footer],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class Home implements OnInit {
  isAuthenticated: boolean = false;

  constructor(
    private router: Router,
    private authService: AuthService,
    private errors: GlobalErrorService
  ) {}

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
  }

  onLogout(): void {
    this.authService.logout();
    this.isAuthenticated = false;
    // Optionally refresh the page or navigate
    // window.location.reload();
  }

  onLogin() {
    // Navigate to login page
    this.router.navigate(['/login']);
  }

  onTryDemo() {
    // If authenticated, go to upload; otherwise prompt to login first
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/upload']);
    } else {
      this.errors.showHttpError(401, 'Please login first to try the demo.');
      // Keep user on the page so they can read the dialog; they can click Login in header
    }
  }

  onDocs() {
    // Navigate to documentation page
    this.router.navigate(['/documentation']);
  }
}