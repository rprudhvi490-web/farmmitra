import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AppNotification {
  id: number;
  title: string;
  body: string;
  type: string;
  readStatus: boolean;
  sentAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;

  getMy(): Observable<AppNotification[]> {
    return this.http.get<AppNotification[]>(`${this.base}/notifications/my`);
  }

  markRead(id: number): Observable<void> {
    return this.http.put<void>(`${this.base}/notifications/${id}/read`, {});
  }

  markAllRead(): Observable<void> {
    return this.http.put<void>(`${this.base}/notifications/read-all`, {});
  }
}
