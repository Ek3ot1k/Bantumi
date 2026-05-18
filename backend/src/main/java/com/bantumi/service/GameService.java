package com.bantumi.service;

import com.bantumi.entity.Game;
import com.bantumi.entity.Move;
import com.bantumi.model.GameState;
import com.bantumi.model.MoveResult;
import com.bantumi.repository.GameRepository;
import com.bantumi.repository.MoveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;

/**
 * Сервис игровой логики Калах/Манкала.
 *
 * Структура board[14]:
 *   [0..5]  — лунки Игрока 1
 *   [6]     — Калах Игрока 1
 *   [7..12] — лунки Игрока 2
 *   [13]    — Калах Игрока 2
 */
@Service
@Transactional
public class GameService {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private MoveRepository moveRepository;

    @Autowired
    private AiService aiService;

    /**
     * Создаёт новую партию, сохраняет в БД и возвращает начальное состояние.
     *
     * @param stones количество камней в каждой лунке (клампируется до 3..6)
     * @param mode   режим игры: "PVP" или "PVE"
     */
    public GameState newGame(int stones, String mode) {
        int clampedStones = Math.max(3, Math.min(6, stones));
        String validMode  = ("PVE".equalsIgnoreCase(mode)) ? "PVE" : "PVP";

        // Инициализируем доску
        int[] board = new int[14];
        for (int i = 0; i < 14; i++) {
            board[i] = (i == 6 || i == 13) ? 0 : clampedStones;
        }

        // Создаём и сохраняем сущность партии
        Game game = new Game();
        game.setBoardState(boardToString(board));
        game.setCurrentPlayer(1);
        game.setGameOver(false);
        game.setWinner(null);
        game.setMoveCount(0);
        game.setLastLandedPit(null);
        game.setStonesPerPit(clampedStones);
        game.setGameMode(validMode);

        gameRepository.save(game);
        return buildState(game);
    }

    /**
     * Возвращает текущее состояние доски из БД.
     * Если партии нет — создаёт новую с дефолтными параметрами.
     */
    @Transactional(readOnly = true)
    public GameState getState() {
        Game game = loadCurrentGame();
        return buildState(game);
    }

    /**
     * Выполняет ход из указанной лунки, сохраняет Move и обновлённую Game в БД.
     * В режиме PVE после хода игрока автоматически делает ход AI.
     *
     * @param pitIndex индекс лунки (0-12)
     */
    public MoveResult makeMove(int pitIndex) {
        Game game = loadCurrentGame();

        // Валидация
        if (game.isGameOver())
            return invalid("Игра окончена", game);
        if (!ownsPit(game.getCurrentPlayer(), pitIndex))
            return invalid("Это не ваша лунка", game);

        int[] board = boardFromString(game.getBoardState());
        if (board[pitIndex] == 0)
            return invalid("Лунка пуста", game);

        // Сохраняем снимок состояния ДО хода для возможности undo
        String snapshot = snapshotToString(board, game.getCurrentPlayer(), game.getWinner());

        int opponentKalah = kalahOf(game.getCurrentPlayer() == 1 ? 2 : 1);
        int myKalah       = kalahOf(game.getCurrentPlayer());

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

        game.setLastLandedPit(current);
        game.setMoveCount(game.getMoveCount() + 1);
        boolean bonusTurn = false;
        boolean captured  = false;

        // Бонусный ход: последний камень попал в свой Калах
        if (current == myKalah) {
            bonusTurn = true;
        }
        // Захват: последний камень попал в пустую свою лунку с камнями напротив
        else if (ownsPit(game.getCurrentPlayer(), current) && board[current] == 1) {
            int opposite = 12 - current;
            if (board[opposite] > 0) {
                board[myKalah] += board[opposite] + 1;
                board[opposite] = 0;
                board[current]  = 0;
                captured = true;
            }
        }

        // Проверка окончания игры
        boolean p1Empty = arePitsEmpty(board, 1);
        boolean p2Empty = arePitsEmpty(board, 2);

        if (p1Empty || p2Empty) {
            // Остатки камней — каждый игрок забирает в свой Калах
            for (int i = 0; i <= 5;  i++) { board[6]  += board[i]; board[i] = 0; }
            for (int i = 7; i <= 12; i++) { board[13] += board[i]; board[i] = 0; }
            game.setGameOver(true);
            if      (board[6] > board[13]) game.setWinner(1);
            else if (board[13] > board[6]) game.setWinner(2);
            else                           game.setWinner(0); // ничья
        } else if (!bonusTurn) {
            // Передаём ход противнику
            game.setCurrentPlayer(game.getCurrentPlayer() == 1 ? 2 : 1);
        }

        game.setBoardState(boardToString(board));

        // Сохраняем запись о ходе
        Move move = new Move();
        move.setGame(game);
        move.setPitIndex(pitIndex);
        move.setPlayerNumber(bonusTurn ? game.getCurrentPlayer()
                : (game.getCurrentPlayer() == 1 ? 2 : 1)); // игрок ДО смены хода
        move.setMoveNumber(game.getMoveCount());
        move.setBonusTurn(bonusTurn);
        move.setCaptured(captured);
        move.setBoardSnapshot(snapshot);
        moveRepository.save(move);

        // Сохраняем обновлённую партию
        gameRepository.save(game);

        // В режиме PVE — делаем ход AI, если не бонусный ход и игра не окончена и сейчас П2
        if ("PVE".equals(game.getGameMode())
                && !bonusTurn
                && !game.isGameOver()
                && game.getCurrentPlayer() == 2) {

            // Сохраняем снимок для undo хода AI
            int[] boardBeforeAi = boardFromString(game.getBoardState());
            String aiSnapshot = snapshotToString(boardBeforeAi, 2, game.getWinner());

            aiService.makeAiMove(game);
            game.setMoveCount(game.getMoveCount()); // уже обновлено внутри AiService

            // Записываем ход AI в историю
            Move aiMove = new Move();
            aiMove.setGame(game);
            aiMove.setPitIndex(game.getLastLandedPit() != null ? game.getLastLandedPit() : -1);
            aiMove.setPlayerNumber(2);
            aiMove.setMoveNumber(game.getMoveCount());
            aiMove.setBonusTurn(false);
            aiMove.setCaptured(false);
            aiMove.setBoardSnapshot(aiSnapshot);
            moveRepository.save(aiMove);

            // Сохраняем состояние после хода AI
            gameRepository.save(game);
        }

        return new MoveResult(true, null, bonusTurn, captured, buildState(game));
    }

    /**
     * Отменяет последний ход: восстанавливает boardSnapshot последнего Move,
     * удаляет запись Move из БД, сохраняет обновлённую Game.
     */
    public GameState undo() {
        Game game = loadCurrentGame();
        Optional<Move> lastMoveOpt = moveRepository.findTopByGameOrderByMoveNumberDesc(game);

        if (lastMoveOpt.isEmpty()) return buildState(game);

        Move lastMove = lastMoveOpt.get();
        String snapshot = lastMove.getBoardSnapshot();

        // Восстанавливаем состояние из снимка
        String[] parts = snapshot.split(";");
        int[] board = boardFromString(parts[0]);
        game.setBoardState(parts[0]);
        game.setCurrentPlayer(Integer.parseInt(parts[1]));
        String winnerStr = parts[2];
        game.setWinner("null".equals(winnerStr) ? null : Integer.parseInt(winnerStr));
        game.setMoveCount(game.getMoveCount() - 1);
        game.setGameOver(false);
        game.setLastLandedPit(null);

        // Удаляем запись последнего хода из БД
        moveRepository.delete(lastMove);

        // Сохраняем восстановленное состояние
        gameRepository.save(game);
        return buildState(game);
    }

    // =========================================================
    //  Вспомогательные приватные методы
    // =========================================================

    /**
     * Загружает текущую (самую свежую) партию из БД.
     * Если партий нет — создаёт новую PVP-партию с 4 камнями.
     */
    private Game loadCurrentGame() {
        return gameRepository.findTopByOrderByUpdatedAtDesc()
                .orElseGet(() -> {
                    // Создаём начальную партию, если БД пустая
                    int[] board = new int[14];
                    for (int i = 0; i < 14; i++) {
                        board[i] = (i == 6 || i == 13) ? 0 : 4;
                    }
                    Game g = new Game();
                    g.setBoardState(boardToString(board));
                    g.setCurrentPlayer(1);
                    g.setGameOver(false);
                    g.setWinner(null);
                    g.setMoveCount(0);
                    g.setLastLandedPit(null);
                    g.setStonesPerPit(4);
                    g.setGameMode("PVP");
                    return gameRepository.save(g);
                });
    }

    /**
     * Строит DTO GameState из сущности Game.
     */
    private GameState buildState(Game game) {
        return new GameState(
                boardFromString(game.getBoardState()),
                game.getCurrentPlayer(),
                game.isGameOver(),
                game.getWinner(),
                game.getMoveCount(),
                game.getLastLandedPit()
        );
    }

    /**
     * Преобразует строку "4,4,4,4,4,4,0,4,4,4,4,4,4,0" в массив int[14].
     */
    int[] boardFromString(String s) {
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
    String boardToString(int[] board) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            if (i > 0) sb.append(',');
            sb.append(board[i]);
        }
        return sb.toString();
    }

    /**
     * Создаёт строку-снимок для undo.
     * Формат: "<board>;<currentPlayer>;<winner>"
     */
    private String snapshotToString(int[] board, int currentPlayer, Integer winner) {
        return boardToString(board) + ";" + currentPlayer + ";" + winner;
    }

    /**
     * Проверяет, принадлежит ли лунка (без Калаха) данному игроку.
     */
    private boolean ownsPit(int player, int pit) {
        return (player == 1) ? (pit >= 0 && pit <= 5) : (pit >= 7 && pit <= 12);
    }

    /**
     * Возвращает индекс Калаха для указанного игрока.
     */
    private int kalahOf(int player) {
        return (player == 1) ? 6 : 13;
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
     * Создаёт MoveResult с признаком ошибки.
     */
    private MoveResult invalid(String reason, Game game) {
        return new MoveResult(false, reason, false, false, buildState(game));
    }
}
