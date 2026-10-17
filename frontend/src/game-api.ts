import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Command, Game } from './game';

@Injectable({ providedIn: 'root' })
export class GameApi {
  private http = inject(HttpClient);
  get(id: string, player: string) {
    return firstValueFrom(this.http.get<Game>(`/api/games/${id}`, {headers: {'X-Player-Id': player}}));
  }
  create(black: string, white: string, actor: string) {
    return firstValueFrom(this.http.post<Game>('/api/games', {blackPlayerId: black, whitePlayerId: white},
      {headers: {'X-Player-Id': actor}}));
  }
  play(id: string, player: string, command: Command) {
    return firstValueFrom(this.http.post<Game>(`/api/games/${id}/moves`, command, {headers: {'X-Player-Id': player}}));
  }
}
