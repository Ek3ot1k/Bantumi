package com.bantumi.model;

/**
 * Состояние игры, передаваемое клиенту (Python frontend).
 * Автор: Хусейнов Амин
 *
 * Структура доски (14 ячеек):
 *   0–5  — лунки Игрока 1
 *   6    — Калах Игрока 1
 *   7–12 — лунки Игрока 2
 *   13   — Калах Игрока 2
 */
public class GameState {

    // Текущее расположение камней на доске
    private int[] board;

    // Игрок, чья сейчас очередь (1 или 2)
    private int currentPlayer;

    // Флаг окончания игры
    private boolean gameOver;

    // Победитель: 1 или 2, 0 = ничья, null = игра не окончена
    private Integer winner;

    // Количество ходов с начала партии
    private int moveCount;

    // Индекс лунки, куда упал последний камень (для подсветки в UI)
    private Integer lastLandedPit;

    public GameState() {}

    public GameState(int[] board, int currentPlayer, boolean gameOver,
                     Integer winner, int moveCount, Integer lastLandedPit) {
        this.board         = board;
        this.currentPlayer = currentPlayer;
        this.gameOver      = gameOver;
        this.winner        = winner;
        this.moveCount     = moveCount;
        this.lastLandedPit = lastLandedPit;
    }

    public int[] getBoard()              { return board; }
    public void  setBoard(int[] board)   { this.board = board; }

    public int  getCurrentPlayer()               { return currentPlayer; }
    public void setCurrentPlayer(int p)          { this.currentPlayer = p; }

    public boolean isGameOver()              { return gameOver; }
    public void    setGameOver(boolean v)    { this.gameOver = v; }

    public Integer getWinner()               { return winner; }
    public void    setWinner(Integer w)      { this.winner = w; }

    public int  getMoveCount()               { return moveCount; }
    public void setMoveCount(int c)          { this.moveCount = c; }

    public Integer getLastLandedPit()            { return lastLandedPit; }
    public void    setLastLandedPit(Integer pit) { this.lastLandedPit = pit; }
}
