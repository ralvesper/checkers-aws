import { Game } from './game';

export type ConnectionStatus = 'connecting' | 'live' | 'reconnecting' | 'offline';
/** Transient transport only; reconnects always reconcile through REST. */
export class LiveGame {
  private socket: WebSocket | null = null;
  private timer: ReturnType<typeof setTimeout> | null = null;
  private key = '';
  private generation = 0;
  private retries = 0;

  constructor(private snapshot: (game: Game) => void, private reconcile: () => void,
              private status: (state: ConnectionStatus) => void,
              private factory: (url: string) => WebSocket = url => new WebSocket(url)) {}

  start(gameId: string, playerId: string) {
    const key = gameId + ':' + playerId;
    if (key === this.key) return;
    this.stop(); this.key = key;
    this.connect(gameId, playerId, this.generation);
  }

  stop() {
    this.generation++; this.key = ''; this.retries = 0;
    if (this.timer) clearTimeout(this.timer);
    this.timer = null;
    const old = this.socket; this.socket = null;
    if (old) old.close();
  }

  private connect(gameId: string, playerId: string, generation: number) {
    if (generation !== this.generation) return;
    this.status(this.retries ? 'reconnecting' : 'connecting');
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    let socket: WebSocket;
    try { socket = this.factory(`${protocol}//${location.host}/api/games/${gameId}/stream?playerId=${playerId}`); }
    catch { this.retry(gameId, playerId, generation); return; }
    this.socket = socket;
    socket.onopen = () => {
      if (generation !== this.generation) return;
      this.retries = 0; this.status('live'); this.reconcile();
    };
    socket.onmessage = event => {
      if (generation !== this.generation) return;
      try {
        const game = JSON.parse(event.data) as Game;
        if (game.id === gameId && Number.isSafeInteger(game.version) && game.version >= 0 && Array.isArray(game.pieces)) this.snapshot(game);
      } catch { this.reconcile(); }
    };
    socket.onerror = () => socket.close();
    socket.onclose = () => { if (generation === this.generation) this.retry(gameId, playerId, generation); };
  }

  private retry(gameId: string, playerId: string, generation: number) {
    this.status('reconnecting');
    const delay = Math.min(15000, 1000 * 2 ** this.retries++);
    this.timer = setTimeout(() => this.connect(gameId, playerId, generation), delay);
  }
}
