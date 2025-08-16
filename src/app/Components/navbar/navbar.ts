import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.css']
})
export class Navbar implements OnInit, OnDestroy {
  isAuthenticated = false;
  email: string | null = null;
  isAdmin = false;
  userInitial = '';

  private sub?: any;

  constructor(private router: Router, private authService: AuthService) {}

  ngOnInit(): void {
    // Initial state
    this.updateFromAuth();
    // React to subsequent auth changes
    this.sub = this.authService.isAuthenticated$.subscribe(() => this.updateFromAuth());
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe?.();
  }

  private updateFromAuth(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.email = this.authService.getCurrentEmail();
    this.isAdmin = (this.email?.toLowerCase() === 'admin@gmail.com');
    this.userInitial = this.email ? this.email.trim().charAt(0).toUpperCase() : '';
  }

  onLogin(): void {
    this.router.navigate(['/login']);
  }

  onLogout(): void {
    this.authService.logout();
    this.isAuthenticated = false;
    this.email = null;
    this.isAdmin = false;
    this.userInitial = '';
  }
}
