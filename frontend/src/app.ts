import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { GameApi } from './game-api';
import { Color, Command, Game, Piece, Position, complete, destinations, mayApply, same } from './game';
import { ConnectionStatus, LiveGame } from './live-game';

@Component({ selector: 'app-root', standalone: true, templateUrl: './app.html' })
export class AppComponent {
  readonly String = String;
  private api = inject(GameApi);
  game = signal<Game | null>(null);
  player = signal('');
  busy = signal(false);
  error = signal('');
  notice = signal('');
  origin = signal<Position | null>(null);
  path = signal<Position[]>([]);
  pending = signal<Command | null>(null);
  invitation = signal('');
  connection = signal<ConnectionStatus>('offline');
  private needsSync = false;
  private live = new LiveGame(game => {
    const current = this.game();
    if (!current || !mayApply(current, game)) return;
    if (game.version > current.version + 1) { this.requestSync(); return; }
    this.game.set(game); this.clearSelection();
  }, () => this.requestSync(), state => this.connection.set(state));
  color = computed<Color>(() => this.game()?.blackPlayerId === this.player() ? 'BLACK' : 'WHITE');
  myTurn = computed(() => this.game()?.currentPlayerId === this.player());
  canConfirm = computed(() => complete(this.game()?.legalMoves ?? [], this.origin(), this.path()));
  next = computed(() => destinations(this.game()?.legalMoves ?? [], this.origin(), this.path()));
  squares = computed(() => {
    const indices = Array.from({length: 8}, (_, i) => i);
    const rows = this.color() === 'BLACK' ? [...indices].reverse() : indices;
    const columns = this.color() === 'BLACK' ? [...indices].reverse() : indices;
    return rows.flatMap(row => columns.map(column => ({row, column})));
  });

  constructor() {
    inject(DestroyRef).onDestroy(() => this.live.stop());
    const fragment = new URLSearchParams(location.hash.slice(1));
    const gameId = fragment.get('game'), actor = fragment.get('player');
    if (gameId && actor && this.uuid(gameId) && this.uuid(actor)) {
      this.player.set(actor);
      void this.load(gameId);
    } else if (gameId || actor) this.error.set('O link da partida está incompleto ou inválido.');
  }

  async create() {
    if (this.busy() || this.pending()) return;
    this.busy.set(true); this.error.set('');
    const white = crypto.randomUUID(), black = crypto.randomUUID();
    try {
      const game = await this.api.create(black, white, white);
      this.player.set(white); this.game.set(game); this.clearSelection();
      this.updateLink(); this.notice.set('Partida criada. Compartilhe o link das pretas para começar.');
    } catch (error) { this.showError(error); }
    finally { this.finish(); }
  }

  async load(id = this.game()?.id) {
    if (!id || this.busy()) return;
    this.busy.set(true); this.error.set('');
    try {
      const game = await this.api.get(id, this.player());
      if (!this.game() || mayApply(this.game(), game)) { this.game.set(game); this.clearSelection(); }
      this.updateLink();
    } catch (error) { this.showError(error); }
    finally { this.finish(); }
  }

  choose(square: Position) {
    if (!this.myTurn() || this.busy() || this.pending()) return;
    if (this.next().some(p => same(p, square))) {
      this.path.update(path => [...path, square]);
      return;
    }
    if (this.game()?.legalMoves.some(move => same(move.from, square))) {
      this.origin.set(square); this.path.set([]);
    }
  }

  async confirm() {
    if (!this.canConfirm() || this.busy() || this.pending()) return;
    this.pending.set({from: this.origin()!, landings: [...this.path()], expectedVersion: this.game()!.version,
      clientMoveId: crypto.randomUUID()});
    await this.sendPending();
  }

  async sendPending() {
    const command = this.pending(), game = this.game();
    if (!command || !game || this.busy()) return;
    this.busy.set(true); this.error.set('');
    let reload = false;
    try {
      const next = await this.api.play(game.id, this.player(), command);
      if (mayApply(this.game(), next)) this.game.set(next);
      this.pending.set(null); this.clearSelection(); this.notice.set('Lance confirmado.');
    } catch (error) {
      this.showError(error);
      if (error instanceof HttpErrorResponse && error.status >= 400 && error.status < 500) {
        this.pending.set(null); this.clearSelection(); reload = true;
      } else this.notice.set('O resultado é incerto. Tente novamente para confirmar o mesmo lance.');
    } finally { this.finish(); }
    if (reload) { const message = this.error(); await this.load(); this.error.set(message); }
  }

  clearSelection() { this.origin.set(null); this.path.set([]); }
  undo() { if (!this.pending()) this.path().length ? this.path.update(p => p.slice(0, -1)) : this.clearSelection(); }
  dark(square: Position) { return (square.row + square.column) % 2 === 1; }
  selected(square: Position) { return same(this.path().at(-1) ?? this.origin(), square); }
  available(square: Position) { return this.next().some(p => same(p, square)); }
  piece(square: Position): Piece | undefined {
    if (!this.game()) return this.dark(square) && (square.row < 3 || square.row > 4)
      ? {...square, color: square.row < 3 ? 'BLACK' : 'WHITE', king: false} : undefined;
    const original = this.game()?.pieces.find(piece => same(piece, this.origin()));
    if (original && this.path().length) {
      if (same(square, this.path().at(-1)!)) return {...original, ...square};
      if (same(square, this.origin())) return undefined;
    }
    return this.game()?.pieces.find(piece => same(piece, square));
  }
  label(square: Position) {
    const piece = this.piece(square);
    return `${String.fromCharCode(97 + square.column)}${8 - square.row}${piece ? ', ' + (piece.king ? 'dama ' : 'pedra ') + (piece.color === 'WHITE' ? 'branca' : 'preta') : ', vazia'}`;
  }
  count(color: Color) { return this.game()?.pieces.filter(piece => piece.color === color).length ?? 12; }
  async copyInvite() {
    try { await navigator.clipboard.writeText(this.invitation()); this.notice.set('Link copiado. Abra em outra aba ou envie ao adversário.'); }
    catch { this.notice.set('Selecione e copie o link exibido abaixo.'); }
  }
  private updateLink() {
    const game = this.game(); if (!game) return;
    const base = location.origin + location.pathname;
    history.replaceState(null, '', `#game=${game.id}&player=${this.player()}`);
    const opponent = this.color() === 'WHITE' ? game.blackPlayerId : game.whitePlayerId;
    this.invitation.set(`${base}#game=${game.id}&player=${opponent}`);
    this.live.start(game.id, this.player());
  }
  private uuid(value: string) { return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value); }
  private requestSync() {
    this.needsSync = true;
    if (!this.busy()) { this.needsSync = false; void this.load(); }
  }
  private finish() {
    this.busy.set(false);
    if (this.needsSync) { this.needsSync = false; void this.load(); }
  }
  private showError(error: unknown) {
    const code = error instanceof HttpErrorResponse ? error.error?.code : '';
    const messages: Record<string, string> = {
      VERSION_CONFLICT: 'A partida avançou. Atualizamos o tabuleiro; escolha seu lance novamente.',
      WRONG_TURN: 'Agora é a vez do adversário.', INVALID_MOVE: 'Esse lance não é permitido pelas regras.',
      COMMAND_CONFLICT: 'Este comando já foi usado com outro lance. Atualize a partida.',
      NOT_FOUND: 'Partida não encontrada. O servidor em memória pode ter sido reiniciado.',
      FORBIDDEN: 'Este link não pertence a um participante da partida.', FINISHED: 'A partida já terminou.',
      INVALID_REQUEST: 'O pedido está incompleto ou inválido.'
    };
    this.error.set(messages[code] ?? 'Não foi possível acessar o servidor. Confira a conexão e tente novamente.');
  }
}
