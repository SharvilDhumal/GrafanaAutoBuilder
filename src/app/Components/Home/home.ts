import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
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
    // If authenticated, go to upload; otherwise prompt to login first
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/upload']);
    } else {
      alert('Please login first to try the demo.');
      this.router.navigate(['/login']);
    }
  }
}