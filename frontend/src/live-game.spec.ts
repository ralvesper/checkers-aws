import { describe, it, expect, vi, afterEach } from 'vitest';
import { LiveGame } from './live-game';
class FakeSocket {
  onopen: (() => void) | null = null;
  onmessage: ((event: {data: string}) => void) | null = null;
  onclose: (() => void) | null = null;
  onerror: (() => void) | null = null;
  close() { this.onclose?.(); }
}
afterEach(() => { vi.useRealTimers(); vi.unstubAllGlobals(); });
describe('conexão da partida', () => {
  it('reconcilia ao conectar, reconecta e encerra sem criar novas conexões', () => {
    vi.useFakeTimers(); vi.stubGlobal('location', {protocol: 'http:', host: 'localhost:4200'});
    const sockets: FakeSocket[] = [];
    const receive = vi.fn(), reconcile = vi.fn(), status = vi.fn();
    const connection = new LiveGame(receive, reconcile, status, () => {
      const socket = new FakeSocket(); sockets.push(socket); return socket as unknown as WebSocket;
    });
    connection.start('one', 'player'); connection.start('one', 'player');
    expect(sockets).toHaveLength(1);
    sockets[0].onopen!(); expect(reconcile).toHaveBeenCalledOnce();
    sockets[0].onmessage!({data: JSON.stringify({id: 'one', version: 1, pieces: []})});
    expect(receive).toHaveBeenCalledOnce();
    sockets[0].onmessage!({data: JSON.stringify({id: 'other', version: 1, pieces: []})});
    expect(receive).toHaveBeenCalledOnce();
    sockets[0].close(); vi.advanceTimersByTime(1000);
    expect(sockets).toHaveLength(2);
    sockets[1].onopen!(); expect(reconcile).toHaveBeenCalledTimes(2);
    connection.stop(); vi.advanceTimersByTime(60000);
    expect(sockets).toHaveLength(2);
  });
  it('ignora callbacks de uma partida anterior', () => {
    vi.stubGlobal('location', {protocol: 'https:', host: 'localhost'});
    const sockets: FakeSocket[] = [], receive = vi.fn();
    const connection = new LiveGame(receive, vi.fn(), vi.fn(), () => {
      const socket = new FakeSocket(); sockets.push(socket); return socket as unknown as WebSocket;
    });
    connection.start('old', 'player'); connection.start('new', 'player');
    sockets[0].onmessage!({data: JSON.stringify({id: 'old', version: 2, pieces: []})});
    expect(receive).not.toHaveBeenCalled(); connection.stop();
  });
});
