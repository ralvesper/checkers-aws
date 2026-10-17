import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Command, Game } from './game';

@Injectable({ providedIn: 'root' })
export class GameApi {
  private http = inject(HttpClient);
  private credential = '';
  token() { return this.credential; }
  async session(fresh = false) {
    const stored = sessionStorage.getItem('checkers.session');
    if (!fresh && stored) {
      try {
        const value = JSON.parse(stored) as {token: string; expiresAt: number};
        if (value.token && value.expiresAt * 1000 > Date.now()) { this.credential = value.token; return; }
      } catch { /* Replace malformed local session. */ }
    }
    const issued = await firstValueFrom(this.http.post<{token: string; expiresAt: number}>('/api/sessions', {}));
    sessionStorage.setItem('checkers.session', JSON.stringify(issued)); this.credential = issued.token;
  }
  private headers() { return {Authorization: `Bearer ${this.credential}`}; }
  async get(id: string) {
    await this.session();
    const response = await firstValueFrom(this.http.get<Game>(`/api/games/${id}`, {headers: this.headers(), observe: 'response'}));
    const player = response.headers.get('X-Seat-Id');
    if (!player || !response.body) throw new Error('Missing authorized seat');
    return {game: response.body, player};
  }
  async create() {
    await this.session();
    return firstValueFrom(this.http.post<{game: Game; invitation: string}>('/api/games', {}, {headers: this.headers()}));
  }
  async join(id: string, invitation: string) {
    // A separate tab claims the invitation with its own credential. Preserve it on network retries.
    const key = `checkers.claim:${id}`;
    if (sessionStorage.getItem(key) !== invitation) { await this.session(true); sessionStorage.setItem(key, invitation); }
    else await this.session();
    const game = await firstValueFrom(this.http.post<Game>(`/api/games/${id}/join`, {invitation}, {headers: this.headers()}));
    sessionStorage.removeItem(key); return game;
  }
  async play(id: string, command: Command) {
    return firstValueFrom(this.http.post<Game>(`/api/games/${id}/moves`, command, {headers: this.headers()}));
  }
}
