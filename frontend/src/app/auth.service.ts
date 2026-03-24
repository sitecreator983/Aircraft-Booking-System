import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

type LoginResponse = { username: string; message: string };

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private http: HttpClient) {}

  login(username: string, password: string) {
    return this.http.post<LoginResponse>('/api/auth/login', { username, password });
  }

  setLoggedIn(username: string) {
    localStorage.setItem('loggedIn', 'true');
    localStorage.setItem('username', username);
  }

  isLoggedIn(): boolean {
    return localStorage.getItem('loggedIn') === 'true';
  }

  logout() {
    localStorage.removeItem('loggedIn');
    localStorage.removeItem('username');
  }
}