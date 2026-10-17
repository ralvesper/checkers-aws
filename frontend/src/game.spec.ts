import { describe, it, expect } from 'vitest';
import { complete, continuations, destinations, mayApply, Game, Move } from './game';
const from = { row: 5, column: 0 };
const a = { row: 3, column: 2 }, b = { row: 1, column: 4 }, c = { row: 1, column: 0 };
const moves: Move[] = [{ from, landings: [a, b] }, { from, landings: [a, c] }];
describe('composição de um turno completo', () => {
  it('oferece apenas os próximos pousos e elimina duplicatas', () => {
    expect(destinations(moves, from, [])).toEqual([a]);
    expect(destinations(moves, from, [a])).toEqual([b, c]);
  });
  it('não confirma captura parcial e aceita os caminhos completos', () => {
    expect(complete(moves, from, [a])).toBe(false);
    expect(complete(moves, from, [a, b])).toBe(true);
    expect(complete(moves, from, [a, c])).toBe(true);
  });
  it('rejeita origem ou prefixo que não está nos lances legais', () => {
    expect(continuations(moves, a, [])).toEqual([]);
    expect(continuations(moves, from, [b])).toEqual([]);
    expect(complete(moves, from, [])).toBe(false);
  });
  it('não deixa recibos antigos ou eventos repetidos reduzirem a versão', () => {
    const game = {id: 'one', version: 3} as Game;
    expect(mayApply(game, {...game, version: 2})).toBe(false);
    expect(mayApply(game, game)).toBe(false);
    expect(mayApply(game, {...game, version: 4})).toBe(true);
    expect(mayApply(game, {...game, id: 'other', version: 9})).toBe(false);
  });
});
