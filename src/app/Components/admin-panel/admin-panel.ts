import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Navbar } from '../../Components/navbar/navbar';
import { MetricsService, ActivityItem } from '../../services/metrics.service';
import { AuthService } from '../../services/auth.service';
import { Observable } from 'rxjs';
import { HttpClient, HttpClientModule } from '@angular/common/http';

@Component({
  selector: 'app-admin-panel',
  standalone: true,
  imports: [CommonModule, Navbar, HttpClientModule],
  templateUrl: './admin-panel.html',
  styleUrls: ['./admin-panel.css']
})

export class AdminPanel {
  totalVisits$!: Observable<number>;
  activeUsers$!: Observable<number>;
  activities$!: Observable<ActivityItem[]>;

  selectedFile: File | null = null;
  uploading = false;
  uploadError: string | null = null;
  uploadSuccess: any = null;

  constructor(private metrics: MetricsService, private auth: AuthService, private http: HttpClient) {
    this.totalVisits$ = this.metrics.totalVisits$;
    this.activeUsers$ = this.metrics.activeUsers$;
    this.activities$ = this.metrics.activities$;
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.uploadError = null;
    this.uploadSuccess = null;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
    } else {
      this.selectedFile = null;
    }
  }

  upload() {
    if (!this.selectedFile) return;
    const form = new FormData();
    form.append('file', this.selectedFile);
    this.uploading = true;
    this.uploadError = null;
    this.uploadSuccess = null;
    this.http.post<any>('/api/files/upload', form).subscribe({
      next: (res) => {
        this.uploadSuccess = res;
        this.uploading = false;
      },
      error: (err) => {
        this.uploadError = err?.error?.error || 'Upload failed';
        this.uploading = false;
      }
    });
  }
}
