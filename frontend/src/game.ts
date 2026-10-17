export interface Position { row: number; column: number }
export type Color = 'WHITE' | 'BLACK';
export interface Piece extends Position { color: Color; king: boolean }
export interface Move { from: Position; landings: Position[] }
export interface Game {
  id: string; blackPlayerId: string; whitePlayerId: string; turn: Color;
  currentPlayerId: string | null; version: number; status: 'IN_PROGRESS' | 'FINISHED';
  winner: Color | null; pieces: Piece[]; legalMoves: Move[];
}
export interface Command extends Move { expectedVersion: number; clientMoveId: string }
export const same = (a: Position | null, b: Position | null) => !!a && !!b && a.row === b.row && a.column === b.column;
export function continuations(moves: Move[], from: Position | null, path: Position[]): Move[] {
  return moves.filter(move => same(move.from, from) && path.every((p, i) => same(p, move.landings[i] ?? null)));
}
export function destinations(moves: Move[], from: Position | null, path: Position[]): Position[] {
  return continuations(moves, from, path).flatMap(move => move.landings[path.length] ? [move.landings[path.length]] : [])
    .filter((p, i, all) => all.findIndex(other => same(p, other)) === i);
}
export function complete(moves: Move[], from: Position | null, path: Position[]): boolean {
  return path.length > 0 && continuations(moves, from, path).some(move => move.landings.length === path.length);
}
export function mayApply(current: Game | null, incoming: Game): boolean {
  return !current || (current.id === incoming.id && incoming.version > current.version);
}
