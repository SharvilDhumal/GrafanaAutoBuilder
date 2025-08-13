import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Navbar } from '../../Components/navbar/navbar';
import { Footer } from '../../Components/footer/footer';
import { DashboardService, UploadResponse } from '../../services/dashboard.service';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule, Navbar, Footer],
  templateUrl: './upload.html',
  styleUrls: ['./upload.css']
})
export class Upload {
  selectedFile: File | null = null;
  isDragging = false;
  loading = false;
  errorMsg: string | null = null;
  result: UploadResponse | null = null;

  constructor(private dashboardService: DashboardService) {}

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file && file.type === 'text/csv') {
      this.selectedFile = file;
      this.errorMsg = null;
    } else {
      alert('Please select a valid CSV file.');
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
      if (file && file.type === 'text/csv') {
        this.selectedFile = file;
        this.errorMsg = null;
      } else {
        alert('Please drop a valid CSV file.');
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
    // Debug: log selected file before upload
    try {
      console.log('[Upload] Starting upload', {
        name: this.selectedFile.name,
        size: this.selectedFile.size,
        type: this.selectedFile.type
      });
    } catch (e) { /* noop */ }

    this.dashboardService.uploadCsv(this.selectedFile)
      .subscribe({
        next: (res) => {
          console.log('[Upload] Success response', res);
          this.result = res;
          this.loading = false;
        },
        error: (err) => {
          // Surface detailed error information in console
          try {
            console.error('[Upload] Error response', {
              status: err?.status,
              statusText: err?.statusText,
              message: err?.message,
              error: err?.error
            });
          } catch (e) { /* noop */ }
          this.errorMsg = (err?.error && (err.error.error || err.error.message)) || err?.message || 'Upload failed';
          this.loading = false;
        }
      });
  }
}