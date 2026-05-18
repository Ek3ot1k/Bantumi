package com.bantumi.service;

import com.bantumi.entity.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для AiService — проверяем корректность выбора ходов AI.
 */
@SpringBootTest
class AiServiceTest {

    @Autowired
    private AiService aiService;

    /** Вспомогательный метод: создаёт Game с нужным состоянием доски */
    private Game createGame(int[] board, String mode) {
        Game game = new Game();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            if (i > 0) sb.append(',');
            sb.append(board[i]);
        }
        game.setBoardState(sb.toString());
        game.setCurrentPlayer(2);
        game.setGameOver(false);
        game.setWinner(null);
        game.setMoveCount(0);
        game.setLastLandedPit(null);
        game.setStonesPerPit(4);
        game.setGameMode(mode);
        return game;
    }

    /**
     * Тест: AI должен выбрать валидную лунку (непустую и принадлежащую П2).
     */
    @Test
    void testAiMakesValidMove() {
        // Стандартная начальная позиция: у П2 лунки 7-12 содержат по 4 камня
        int[] board = {4,4,4,4,4,4,0, 4,4,4,4,4,4,0};
        Game game = createGame(board, "PVE");
        String boardBefore = game.getBoardState();

        aiService.makeAiMove(game);

        // После хода AI состояние доски должно измениться
        assertNotEquals(boardBefore, game.getBoardState(),
                "AI должен изменить состояние доски");
    }

    /**
     * Тест: AI не выбирает пустую лунку.
     * У П2 только одна непустая лунка (индекс 10), AI должен выбрать именно её.
     */
    @Test
    void testAiDoesNotPickEmptyPit() {
        // У П2 все лунки пусты, кроме лунки 10 (3 камня)
        int[] board = {4,4,4,4,4,4,0, 0,0,0,3,0,0,0};
        Game game = createGame(board, "PVE");

        aiService.makeAiMove(game);

        // После хода лунка 10 должна стать пустой (из неё сделан ход)
        String[] parts = game.getBoardState().split(",");
        assertEquals(0, Integer.parseInt(parts[10]),
                "AI должен выбрать единственную непустую лунку П2 (индекс 10)");
    }

    /**
     * Тест: AI выбирает ход в Калах, когда это единственная доступная лунка.
     * Лунка 11 содержит 2 камня — ход 11->12->13(Калах) — единственный возможный ход.
     */
    @Test
    void testAiPrefersKalahMove() {
        // У П2: все лунки пусты, кроме лунки 11 (2 камня) — ход 11->12->13(Калах)
        // Это единственный доступный ход, и он заканчивается в Калахе (бонусный ход)
        int[] board = {4,4,4,4,4,4,0, 0,0,0,0,2,0,0};
        Game game = createGame(board, "PVE");

        aiService.makeAiMove(game);

        // При бонусном ходе currentPlayer остаётся 2
        assertEquals(2, game.getCurrentPlayer(),
                "При единственном ходе в Калах должен быть бонусный ход (currentPlayer=2)");

        // Лунка 11 должна стать пустой
        String[] parts = game.getBoardState().split(",");
        assertEquals(0, Integer.parseInt(parts[11]),
                "Лунка 11 должна опустеть после хода");

        // Калах П2 должен получить камни
        assertTrue(Integer.parseInt(parts[13]) > 0,
                "Калах П2 должен получить камни");
    }

    /**
     * Тест: вспомогательный метод simulateMove не изменяет оригинальную доску.
     */
    @Test
    void testSimulateMove_doesNotMutateOriginal() {
        int[] board = {4,4,4,4,4,4,0, 4,4,4,4,4,4,0};
        int[] original = board.clone();

        aiService.simulateMove(board, 7, 2);

        assertArrayEquals(original, board,
                "simulateMove не должен изменять оригинальный массив");
    }

    /**
     * Тест: evaluate возвращает разницу Калах П2 минус Калах П1.
     */
    @Test
    void testEvaluate() {
        int[] board = {0,0,0,0,0,0,8, 0,0,0,0,0,0,12};
        int score = aiService.evaluate(board);
        assertEquals(4, score, "Оценка должна быть board[13] - board[6] = 12 - 8 = 4");
    }

    /**
     * Тест: isGameOver возвращает true, если все лунки одной стороны пусты.
     */
    @Test
    void testIsGameOver_whenP1Empty() {
        int[] board = {0,0,0,0,0,0,20, 4,4,4,4,4,4,0};
        assertTrue(aiService.isGameOver(board),
                "Игра должна завершиться, если лунки П1 пусты");
    }
}
