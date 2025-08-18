import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { isPlatformBrowser } from '@angular/common';

export interface UploadResponse {
  uid: string;
  title: string;
  grafanaUrl: string;
  grafanaResponse?: any;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private baseUrl = `${environment.apiUrl}/dashboard`;

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  uploadCsv(file: File, title?: string): Observable<UploadResponse> {
    const form = new FormData();
    form.append('file', file, file.name);
    if (title) {
      form.append('title', title);
    }

    let headers = new HttpHeaders();
    if (isPlatformBrowser(this.platformId)) {
      const token = sessionStorage.getItem('authToken');
      if (token) {
        headers = headers.set('Authorization', `Bearer ${token}`);
      }
    }

    const endpoint = `${this.baseUrl}/upload`;
    // Debug: log request info (excluding sensitive values)
    try {
      console.log('[DashboardService] POST', endpoint, {
        file: { name: file.name, size: file.size, type: file.type },
        hasAuthHeader: headers.has('Authorization'),
        withCredentials: true,
      });
    } catch (e) {
      /* noop */
    }

    // Don't set Content-Type header - let browser set it automatically for FormData
    
    return this.http.post<UploadResponse>(endpoint, form, {
      headers,
      withCredentials: true,
    });
  }
}
