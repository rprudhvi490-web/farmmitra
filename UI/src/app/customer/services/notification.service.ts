import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
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
    return this.http.get<any>(`${this.base}/notifications/my`).pipe(
      map(res => {
        if (!res) return [];
        if (Array.isArray(res)) return res;
        return Array.isArray(res.content) ? res.content : [];
      })
    );
  }

  // Only unread ORDER_UPDATE notifications — used for auto-show on home page
  getUnreadOrderUpdates(): Observable<AppNotification[]> {
    return this.getMy().pipe(
      map(list => list.filter(n => !n.readStatus && n.type === 'ORDER_UPDATE'))
    );
  }

  markRead(id: number): Observable<void> {
    return this.http.put<void>(`${this.base}/notifications/${id}/read`, {});
  }

  markAllRead(): Observable<void> {
    return this.http.put<void>(`${this.base}/notifications/read-all`, {});
  }
}
