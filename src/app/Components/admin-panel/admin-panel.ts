import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Navbar } from '../../Components/navbar/navbar';
import { MetricsService, ActivityItem } from '../../services/metrics.service';
import { AuthService } from '../../services/auth.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-admin-panel',
  standalone: true,
  imports: [CommonModule, Navbar],
  templateUrl: './admin-panel.html',
  styleUrls: ['./admin-panel.css']
})

export class AdminPanel {
  totalVisits$!: Observable<number>;
  activeUsers$!: Observable<number>;
  activities$!: Observable<ActivityItem[]>;

  constructor(private metrics: MetricsService, private auth: AuthService) {
    this.totalVisits$ = this.metrics.totalVisits$;
    this.activeUsers$ = this.metrics.activeUsers$;
    this.activities$ = this.metrics.activities$;
  }
}
