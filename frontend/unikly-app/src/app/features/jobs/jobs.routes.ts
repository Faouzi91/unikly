import { Routes } from '@angular/router';
import { authGuard } from '../../core/auth/auth.guard';
import { roleGuard } from '../../core/auth/role.guard';

export const JOBS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./job-list/job-list.component').then((m) => m.JobListComponent),
    canActivate: [authGuard],
  },
  {
    path: 'create',
    loadComponent: () =>
      import('./job-create/job-create.component').then((m) => m.JobCreateComponent),
    canActivate: [authGuard, roleGuard('ROLE_CLIENT')],
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./job-detail/job-detail.component').then((m) => m.JobDetailComponent),
    canActivate: [authGuard],
  },
];
