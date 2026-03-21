import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RegistrationRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  role: 'CLIENT' | 'FREELANCER';
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  register(payload: RegistrationRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/users/register`, payload);
  }
}
