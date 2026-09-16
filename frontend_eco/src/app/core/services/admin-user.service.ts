import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { UserProfileResponse, CreateUserRequest, UpdateUserRequest } from '../models';

@Injectable({
  providedIn: 'root'
})
export class AdminUserService {
  private http = inject(HttpClient);

  getAllUsers(): Observable<UserProfileResponse[]> {
    return this.http.get<UserProfileResponse[]>(API_ENDPOINTS.users.root);
  }

  createUser(payload: CreateUserRequest): Observable<UserProfileResponse> {
    return this.http.post<UserProfileResponse>(API_ENDPOINTS.users.root, payload);
  }

  updateUser(id: string, payload: UpdateUserRequest): Observable<UserProfileResponse> {
    return this.http.put<UserProfileResponse>(API_ENDPOINTS.users.byId(id), payload);
  }

  deleteUser(id: string): Observable<string> {
    return this.http.delete(API_ENDPOINTS.users.byId(id), { responseType: 'text' });
  }
}
