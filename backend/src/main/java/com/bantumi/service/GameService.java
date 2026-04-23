package com.bantumi.service;

import com.bantumi.model.GameState;
import com.bantumi.model.MoveResult;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Структура board[14]:
 *   [0..5]  — лунки Игрока 1
 *   [6]     — Калах Игрока 1
 *   [7..12] — лунки Игрока 2
 *   [13]    — Калах Игрока 2
 */
@Service
public class GameService {

    private int[] board = new int[14];
    private int   currentPlayer = 1;
    private boolean gameOver    = false;
    private Integer winner      = null;
    private int     moveCount   = 0;
    private Integer lastLandedPit = null;
    private int     stonesPerPit  = 4;

    // Стек для функции Undo (сохраняем снимки состояния)
    private final Deque<int[]> history = new ArrayDeque<>();

    public GameService() {
        initBoard(4);
    }

    public GameState newGame(int stones) {
        this.stonesPerPit = Math.max(3, Math.min(6, stones)); // клампируем 3..6
        initBoard(this.stonesPerPit);
        return buildState();
    }

    public GameState getState() {
        return buildState();
    }

    public MoveResult makeMove(int pitIndex) {
        // Валидация
        if (gameOver)
            return invalid("Игра окончена");
        if (!ownsPit(currentPlayer, pitIndex))
            return invalid("Это не ваша лунка");
        if (board[pitIndex] == 0)
            return invalid("Лунка пуста");

        // Сохраняем снимок для Undo
        history.push(snapshot());

        int opponentKalah = kalahOf(currentPlayer == 1 ? 2 : 1);
        int myKalah       = kalahOf(currentPlayer);

        // Берём все камни из выбранной лунки
        int stones = board[pitIndex];
        board[pitIndex] = 0;
        int current = pitIndex;

        // Раскладываем по одному против часовой стрелки
        while (stones > 0) {
            current = (current + 1) % 14;
            if (current == opponentKalah) continue; // пропускаем Калах противника
            board[current]++;
            stones--;
        }

        lastLandedPit = current;
        moveCount++;
        boolean bonusTurn = false;
        boolean captured  = false;

        // Бонусный ход: последний камень попал в свой Калах
        if (current == myKalah) {
            bonusTurn = true;
        }
        // Захват: последний камень попал в пустую лунку своей стороны
        else if (ownsPit(currentPlayer, current) && board[current] == 1) {
            int opposite = 12 - current;
            if (board[opposite] > 0) {
                board[myKalah] += board[opposite] + 1;
                board[opposite] = 0;
                board[current]  = 0;
                captured = true;
            }
        }
\
        boolean p1Empty = arePitsEmpty(1);
        boolean p2Empty = arePitsEmpty(2);

        if (p1Empty || p2Empty) {
            // Остатки камней — каждый в свой Калах
            for (int i = 0; i <= 5;  i++) { board[6]  += board[i]; board[i] = 0; }
            for (int i = 7; i <= 12; i++) { board[13] += board[i]; board[i] = 0; }
            gameOver = true;
            if      (board[6] > board[13]) winner = 1;
            else if (board[13] > board[6]) winner = 2;
            else                           winner = 0; // ничья
        } else if (!bonusTurn) {
            // Передаём ход
            currentPlayer = (currentPlayer == 1) ? 2 : 1;
        }

        return new MoveResult(true, null, bonusTurn, captured, buildState());
    }

    /** Отменить последний ход */
    public GameState undo() {
        if (history.isEmpty()) return buildState();

        int[] snap = history.pop();
        // Формат снимка: board[14] + currentPlayer + gameOver(0/1) + winner(-1/0/1/2) + moveCount
        board         = Arrays.copyOfRange(snap, 0, 14);
        currentPlayer = snap[14];
        gameOver      = snap[15] == 1;
        winner        = snap[16] == -1 ? null : snap[16];
        moveCount     = snap[17];
        lastLandedPit = null;
        return buildState();
    }

    private void initBoard(int stones) {
        board = new int[14];
        for (int i = 0; i < 14; i++) {
            if (i == 6 || i == 13) board[i] = 0;
            else                   board[i] = stones;
        }
        currentPlayer = 1;
        gameOver      = false;
        winner        = null;
        moveCount     = 0;
        lastLandedPit = null;
        history.clear();
    }

    /** Принадлежит ли лунка (без Калаха) игроку */
    private boolean ownsPit(int player, int pit) {
        return player == 1 ? (pit >= 0 && pit <= 5) : (pit >= 7 && pit <= 12);
    }

    /** Индекс Калаха игрока */
    private int kalahOf(int player) {
        return player == 1 ? 6 : 13;
    }

    /** Пусты ли все лунки игрока (без Калаха) */
    private boolean arePitsEmpty(int player) {
        int from = player == 1 ? 0 : 7;
        int to   = player == 1 ? 5 : 12;
        for (int i = from; i <= to; i++) {
            if (board[i] > 0) return false;
        }
        return true;
    }

    /** Сохранить снимок состояния для Undo */
    private int[] snapshot() {
        int[] snap = new int[18];
        System.arraycopy(board, 0, snap, 0, 14);
        snap[14] = currentPlayer;
        snap[15] = gameOver ? 1 : 0;
        snap[16] = winner == null ? -1 : winner;
        snap[17] = moveCount;
        return snap;
    }

    /** Построить DTO для ответа клиенту */
    private GameState buildState() {
        return new GameState(
            Arrays.copyOf(board, 14),
            currentPlayer,
            gameOver,
            winner,
            moveCount,
            lastLandedPit
        );
    }

    /** Ответ с ошибкой */
    private MoveResult invalid(String reason) {
        return new MoveResult(false, reason, false, false, buildState());
    }
}
