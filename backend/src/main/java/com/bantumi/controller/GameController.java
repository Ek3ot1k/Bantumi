package com.bantumi.controller;

import com.bantumi.model.GameState;
import com.bantumi.model.MoveResult;
import com.bantumi.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * REST контроллер игры Калах/Манкала.
 * CORS настроен глобально в WebConfig, аннотация @CrossOrigin здесь не нужна.
 */
@RestController
@RequestMapping("/api/v1/game")
public class GameController {

    @Autowired
    private GameService gameService;

    /** Получить текущее состояние доски */
    @GetMapping("/state")
    public GameState getState() {
        return gameService.getState();
    }

    /**
     * Начать новую партию.
     *
     * @param stones количество камней в каждой лунке (по умолчанию 4, диапазон 3..6)
     * @param mode   режим игры: "PVP" (два человека) или "PVE" (против AI), по умолчанию "PVP"
     */
    @PostMapping("/new")
    public GameState newGame(
            @RequestParam(defaultValue = "4") int stones,
            @RequestParam(defaultValue = "PVP") String mode) {
        return gameService.newGame(stones, mode);
    }

    /**
     * Сделать ход из указанной лунки.
     *
     * @param pit индекс лунки (0-5 для Игрока 1, 7-12 для Игрока 2)
     */
    @PostMapping("/move/{pit}")
    public MoveResult makeMove(@PathVariable int pit) {
        return gameService.makeMove(pit);
    }

    /** Отменить последний ход (undo) */
    @PostMapping("/undo")
    public GameState undo() {
        return gameService.undo();
    }
}
