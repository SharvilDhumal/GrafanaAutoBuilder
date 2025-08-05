import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  imports: [],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class Home {
  constructor(private router: Router) {}

  onLogin() {
    // Navigate to login page
    this.router.navigate(['/login']);
  }

  onTryDemo() {
    // Navigate to login page with demo credentials
    this.router.navigate(['/login']);
  }
}