package com.bantumi.controller;

import com.bantumi.model.GameState;
import com.bantumi.model.MoveResult;
import com.bantumi.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * REST-контроллер игры Бантуми.
 * Автор: Хусейнов Амин (backend)
 *
 * Все эндпоинты начинаются с /api/v1/game
 * @CrossOrigin разрешает запросы от Python-клиента
 */
@RestController
@RequestMapping("/api/v1/game")
@CrossOrigin(origins = "*")
public class GameController {

    @Autowired
    private GameService gameService;

    /**
     * GET /api/v1/game/state
     * Получить текущее состояние игры
     */
    @GetMapping("/state")
    public GameState getState() {
        return gameService.getState();
    }

    /**
     * POST /api/v1/game/new?stones=4
     * Начать новую партию
     */
    @PostMapping("/new")
    public GameState newGame(@RequestParam(defaultValue = "4") int stones) {
        return gameService.newGame(stones);
    }

    /**
     * POST /api/v1/game/move/{pit}
     * Сделать ход из лунки с индексом pit (0–5 для Игрока 1, 7–12 для Игрока 2)
     */
    @PostMapping("/move/{pit}")
    public MoveResult makeMove(@PathVariable int pit) {
        return gameService.makeMove(pit);
    }

    /**
     * POST /api/v1/game/undo
     * Отменить последний ход
     */
    @PostMapping("/undo")
    public GameState undo() {
        return gameService.undo();
    }
}
