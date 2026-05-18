package com.bantumi.service;

import com.bantumi.entity.Game;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Сервис искусственного интеллекта для режима PVE.
 * Использует алгоритм Minimax с альфа-бета отсечением.
 *
 * Структура board[14]:
 *   [0..5]  — лунки Игрока 1
 *   [6]     — Калах Игрока 1
 *   [7..12] — лунки Игрока 2
 *   [13]    — Калах Игрока 2
 */
@Service
public class AiService {

    /** Глубина поиска Minimax (количество полуходов вперёд) */
    private static final int SEARCH_DEPTH = 7;

    /**
     * Выбирает лучший ход для Игрока 2 (AI) и применяет его к объекту Game.
     * Метод изменяет поля game (boardState, currentPlayer и т.д.), но НЕ сохраняет в БД —
     * это задача вызывающего метода.
     *
     * @param game текущая партия (currentPlayer должен быть 2)
     */
    public void makeAiMove(Game game) {
        int[] board = boardFromString(game.getBoardState());
        int bestPit = -1;
        int bestScore = Integer.MIN_VALUE;

        // Перебираем все допустимые лунки Игрока 2 (индексы 7-12)
        for (int pit = 7; pit <= 12; pit++) {
            if (board[pit] == 0) continue; // пустая лунка — пропускаем

            int[] simulated = simulateMove(board, pit, 2);
            // Определяем, чей ход после симуляции (бонусный ход или передача)
            boolean bonusForAi = lastLandedInKalah(board, pit, 2);
            boolean nextIsMax = bonusForAi; // если бонус, AI ходит снова — maximizing

            int score = minimax(simulated, SEARCH_DEPTH - 1, nextIsMax,
                    Integer.MIN_VALUE, Integer.MAX_VALUE);

            if (score > bestScore) {
                bestScore = score;
                bestPit = pit;
            }
        }

        // Если нет валидных ходов — ничего не делаем
        if (bestPit == -1) return;

        // Применяем выбранный ход к доске
        applyMoveToGame(game, board, bestPit);
    }

    /**
     * Рекурсивный Minimax с альфа-бета отсечением.
     *
     * @param board       текущее состояние доски
     * @param depth       оставшаяся глубина поиска
     * @param isMaximizing true = ход AI (Игрок 2, максимизирует), false = ход П1
     * @param alpha       лучшая оценка для максимизирующего игрока
     * @param beta        лучшая оценка для минимизирующего игрока
     * @return эвристическая оценка позиции
     */
    private int minimax(int[] board, int depth, boolean isMaximizing, int alpha, int beta) {
        // Терминальные условия: конец игры или достигнута максимальная глубина
        if (depth == 0 || isGameOver(board)) {
            return evaluate(board);
        }

        if (isMaximizing) {
            // Ход Игрока 2 (AI) — максимизируем счёт
            int maxEval = Integer.MIN_VALUE;
            for (int pit = 7; pit <= 12; pit++) {
                if (board[pit] == 0) continue;

                int[] next = simulateMove(board, pit, 2);
                boolean bonus = lastLandedInKalah(board, pit, 2);
                int eval = minimax(next, depth - 1, bonus, alpha, beta);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break; // альфа-отсечение
            }
            // Если не было доступных ходов — возвращаем оценку текущей позиции
            return maxEval == Integer.MIN_VALUE ? evaluate(board) : maxEval;
        } else {
            // Ход Игрока 1 — минимизируем счёт
            int minEval = Integer.MAX_VALUE;
            for (int pit = 0; pit <= 5; pit++) {
                if (board[pit] == 0) continue;

                int[] next = simulateMove(board, pit, 1);
                boolean bonus = lastLandedInKalah(board, pit, 1);
                int eval = minimax(next, depth - 1, !bonus, alpha, beta);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break; // бета-отсечение
            }
            return minEval == Integer.MAX_VALUE ? evaluate(board) : minEval;
        }
    }

    /**
     * Симулирует ход из указанной лунки для указанного игрока.
     * Возвращает НОВЫЙ массив — оригинал не изменяется.
     *
     * @param board  текущее состояние доски
     * @param pit    индекс лунки (0-5 для П1, 7-12 для П2)
     * @param player номер игрока (1 или 2)
     * @return новое состояние доски после хода
     */
    int[] simulateMove(int[] board, int pit, int player) {
        int[] b = Arrays.copyOf(board, 14);
        int opponentKalah = (player == 1) ? 13 : 6;
        int myKalah       = (player == 1) ? 6  : 13;

        int stones = b[pit];
        b[pit] = 0;
        int current = pit;

        // Раскладываем камни против часовой стрелки, пропуская Калах противника
        while (stones > 0) {
            current = (current + 1) % 14;
            if (current == opponentKalah) continue;
            b[current]++;
            stones--;
        }

        // Проверка захвата: последний камень в пустую свою лунку
        boolean inMyPit = (player == 1) ? (current >= 0 && current <= 5)
                                        : (current >= 7 && current <= 12);
        if (inMyPit && b[current] == 1 && current != myKalah) {
            int opposite = 12 - current;
            if (b[opposite] > 0) {
                b[myKalah] += b[opposite] + 1;
                b[opposite] = 0;
                b[current]  = 0;
            }
        }

        // Проверка окончания игры — переносим остатки камней в Калахи
        boolean p1Empty = arePitsEmpty(b, 1);
        boolean p2Empty = arePitsEmpty(b, 2);
        if (p1Empty || p2Empty) {
            for (int i = 0; i <= 5;  i++) { b[6]  += b[i]; b[i] = 0; }
            for (int i = 7; i <= 12; i++) { b[13] += b[i]; b[i] = 0; }
        }

        return b;
    }

    /**
     * Проверяет, попал ли последний камень хода в Калах игрока (бонусный ход).
     *
     * @param board  состояние доски ДО хода
     * @param pit    лунка, из которой делается ход
     * @param player номер игрока
     * @return true, если последний камень попадает в Калах игрока
     */
    private boolean lastLandedInKalah(int[] board, int pit, int player) {
        int opponentKalah = (player == 1) ? 13 : 6;
        int myKalah       = (player == 1) ? 6  : 13;
        int stones = board[pit];
        int current = pit;

        while (stones > 0) {
            current = (current + 1) % 14;
            if (current == opponentKalah) continue;
            stones--;
        }
        return current == myKalah;
    }

    /**
     * Проверяет, закончилась ли игра (все лунки одной из сторон пусты).
     *
     * @param board состояние доски
     * @return true, если игра завершена
     */
    boolean isGameOver(int[] board) {
        return arePitsEmpty(board, 1) || arePitsEmpty(board, 2);
    }

    /**
     * Эвристическая оценка позиции.
     * Положительная оценка выгодна AI (Игрок 2), отрицательная — Игроку 1.
     *
     * @param board состояние доски
     * @return оценка: Калах П2 минус Калах П1
     */
    int evaluate(int[] board) {
        return board[13] - board[6];
    }

    /**
     * Применяет выбранный ход AI к объекту Game.
     * Обновляет все поля: boardState, currentPlayer, gameOver, winner, moveCount, lastLandedPit.
     */
    private void applyMoveToGame(Game game, int[] boardBefore, int bestPit) {
        int opponentKalah = 6;  // Калах П1 (противник AI)
        int myKalah       = 13; // Калах AI (П2)

        int[] b = Arrays.copyOf(boardBefore, 14);
        int stones = b[bestPit];
        b[bestPit] = 0;
        int current = bestPit;

        // Раскладываем камни, пропуская Калах противника
        while (stones > 0) {
            current = (current + 1) % 14;
            if (current == opponentKalah) continue;
            b[current]++;
            stones--;
        }

        game.setLastLandedPit(current);
        game.setMoveCount(game.getMoveCount() + 1);

        boolean bonusTurn = (current == myKalah);

        // Захват: последний камень в пустую лунку AI
        if (!bonusTurn && current >= 7 && current <= 12 && b[current] == 1) {
            int opposite = 12 - current;
            if (b[opposite] > 0) {
                b[myKalah] += b[opposite] + 1;
                b[opposite] = 0;
                b[current]  = 0;
            }
        }

        // Проверка окончания игры
        boolean p1Empty = arePitsEmpty(b, 1);
        boolean p2Empty = arePitsEmpty(b, 2);

        if (p1Empty || p2Empty) {
            for (int i = 0; i <= 5;  i++) { b[6]  += b[i]; b[i] = 0; }
            for (int i = 7; i <= 12; i++) { b[13] += b[i]; b[i] = 0; }
            game.setGameOver(true);
            if      (b[6] > b[13]) game.setWinner(1);
            else if (b[13] > b[6]) game.setWinner(2);
            else                   game.setWinner(0);
        } else if (!bonusTurn) {
            // Передаём ход Игроку 1
            game.setCurrentPlayer(1);
        }
        // При бонусном ходе currentPlayer остаётся 2

        game.setBoardState(boardToString(b));
    }

    /**
     * Проверяет, пусты ли все лунки (без Калаха) указанного игрока.
     */
    private boolean arePitsEmpty(int[] board, int player) {
        int from = (player == 1) ? 0 : 7;
        int to   = (player == 1) ? 5 : 12;
        for (int i = from; i <= to; i++) {
            if (board[i] > 0) return false;
        }
        return true;
    }

    /**
     * Преобразует строку "0,0,4,4,4,4,0,4,4,4,4,4,4,0" в массив int[14].
     */
    private int[] boardFromString(String s) {
        String[] parts = s.split(",");
        int[] board = new int[14];
        for (int i = 0; i < 14; i++) {
            board[i] = Integer.parseInt(parts[i].trim());
        }
        return board;
    }

    /**
     * Преобразует массив int[14] в строку через запятую.
     */
    private String boardToString(int[] board) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            if (i > 0) sb.append(',');
            sb.append(board[i]);
        }
        return sb.toString();
    }
}
