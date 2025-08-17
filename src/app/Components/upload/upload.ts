import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Navbar } from '../../Components/navbar/navbar';
import { Footer } from '../../Components/footer/footer';
import {
  DashboardService,
  UploadResponse,
} from '../../services/dashboard.service';
import { MetricsService } from '../../services/metrics.service';
import { AuthService } from '../../services/auth.service';
import { GlobalErrorService } from '../../core/errors/global-error.service';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule, Navbar, Footer],
  templateUrl: './upload.html',
  styleUrls: ['./upload.css'],
})
export class Upload {
  selectedFile: File | null = null;
  isDragging = false;
  loading = false;
  errorMsg: string | null = null;
  result: UploadResponse | null = null;
  copied = false;

  constructor(
    private dashboardService: DashboardService,
    private metrics: MetricsService,
    private auth: AuthService,
    private errors: GlobalErrorService,
  ) {}

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (
      file &&
      (file.type === 'text/csv' || file.name.toLowerCase().endsWith('.csv'))
    ) {
      this.selectedFile = file;
      this.errorMsg = null;
    } else {
      this.errors.showError({ title: 'Invalid file', message: 'Please select a valid CSV file.' });
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;

    if (event.dataTransfer?.files) {
      const file = event.dataTransfer.files[0];
      if (
        file &&
        (file.type === 'text/csv' || file.name.toLowerCase().endsWith('.csv'))
      ) {
        this.selectedFile = file;
        this.errorMsg = null;
      } else {
        this.errors.showError({ title: 'Invalid file', message: 'Please drop a valid CSV file.' });
      }
    }
  }

  removeFile(): void {
    this.selectedFile = null;
    this.result = null;
    this.errorMsg = null;
  }

  getFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  generateDashboard(): void {
    if (!this.selectedFile || this.loading) return;
    this.loading = true;
    this.errorMsg = null;
    this.result = null;
    this.copied = false;
    // Debug: log selected file before upload
    try {
      console.log('[Upload] Starting upload', {
        name: this.selectedFile.name,
        size: this.selectedFile.size,
        type: this.selectedFile.type,
        lastModified: this.selectedFile.lastModified,
      });

      // Also log the file content for debugging
      const reader = new FileReader();
      reader.onload = (e) => {
        console.log(
          '[Upload] File content preview:',
          e.target?.result?.toString().substring(0, 200)
        );
      };
      reader.readAsText(this.selectedFile);
    } catch (e) {
      /* noop */
    }

    this.dashboardService.uploadCsv(this.selectedFile).subscribe({
      next: (res) => {
        console.log('[Upload] Success response', res);
        this.result = res;
        this.loading = false;
        // Record conversion activity for admin metrics
        try {
          const email = this.auth.getCurrentEmail() || 'anonymous';
          const docName = this.selectedFile?.name || res?.title || 'CSV';
          this.metrics.recordConversion(email, docName);
        } catch (e) {
          // non-fatal
        }
      },
      error: (err) => {
        // Surface detailed error information in console
        try {
          console.error('[Upload] Error response', {
            status: err?.status,
            statusText: err?.statusText,
            message: err?.message,
            error: err?.error,
          });
        } catch (e) {
          /* noop */
        }
        this.errorMsg =
          (err?.error && (err.error.error || err.error.message)) ||
          err?.message ||
          'Upload failed';
        this.loading = false;
      },
    });
  }

  async copyGrafanaLink(): Promise<void> {
    if (!this.result?.grafanaUrl || this.copied) return;
    const url = this.result.grafanaUrl;
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(url);
      } else {
        const ta = document.createElement('textarea');
        ta.value = url;
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
      }
      this.copied = true;
      setTimeout(() => (this.copied = false), 2000);
    } catch (e) {
      console.warn('Failed to copy link', e);
    }
  }
}
