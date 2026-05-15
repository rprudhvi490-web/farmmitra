import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import {
  provideRouter,
  withComponentInputBinding,
  withRouterConfig
} from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),

    // Router — withComponentInputBinding lets route params bind directly as @Input()
    // withRouterConfig paramsInheritanceStrategy ensures child routes inherit params
    provideRouter(
      routes,
      withComponentInputBinding(),
      withRouterConfig({ paramsInheritanceStrategy: 'always' })
    ),

    // HTTP client with functional interceptors (auth JWT + global error handling)
    provideHttpClient(
      withInterceptors([authInterceptor, errorInterceptor])
    ),

    // No animations — eliminates ~50KB bundle, Material components work fine without motion
    provideNoopAnimations(),
  ],
};
