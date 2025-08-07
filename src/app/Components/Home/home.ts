import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-home',
  imports: [CommonModule],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class Home implements OnInit {
  isAuthenticated: boolean = false;

  constructor(
    private router: Router,
    private authService: AuthService
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
    // Navigate to login page with demo credentials
    this.router.navigate(['/login']);
  }
}