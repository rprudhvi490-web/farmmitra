import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

const CLOUDINARY_CLOUD_NAME = 'dhqd6jasz';
const CLOUDINARY_UPLOAD_PRESET = 'weekendbasketpreset';

@Injectable({ providedIn: 'root' })
export class CloudinaryService {
  constructor(private http: HttpClient) {}

  upload(file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('upload_preset', CLOUDINARY_UPLOAD_PRESET);
    formData.append('folder', 'weekendbasket');

    return this.http.post<any>(
      `https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/image/upload`,
      formData
    ).pipe(map(res => res.secure_url));
  }
}
