import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { TokenService } from '../services/token.service';
import { ToastService } from '../services/toast.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router       = inject(Router);
  const toast        = inject(ToastService);
  const tokenService = inject(TokenService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      const message = err.error?.message ?? 'Something went wrong. Please try again.';

      switch (err.status) {
        case 401:
          tokenService.remove();
          router.navigate(['/auth/login']);
          break;
        case 403:
          toast.error('Access denied.');
          break;
        case 429:
          toast.error('Too many attempts. Please wait and try again.');
          break;
        case 400:
        case 422:
          toast.error(message);
          break;
        case 500:
          toast.error('Server error. Please try again later.');
          break;
      }

      return throwError(() => err);
    })
  );
};
