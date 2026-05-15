import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';
import { TokenService } from '../services/token.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const snackbar = inject(MatSnackBar);
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
          snackbar.open('Access denied.', 'Close', { duration: 3000 });
          break;
        case 429:
          snackbar.open('Too many attempts. Please wait and try again.', 'Close', { duration: 4000 });
          break;
        case 400:
        case 422:
          snackbar.open(message, 'Close', { duration: 3000 });
          break;
        case 500:
          snackbar.open('Server error. Please try again later.', 'Close', { duration: 4000 });
          break;
      }

      return throwError(() => err);
    })
  );
};
