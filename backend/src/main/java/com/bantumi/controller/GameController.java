package com.bantumi.controller;

import com.bantumi.model.GameState;
import com.bantumi.model.MoveResult;
import com.bantumi.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/game")
@CrossOrigin(origins = "*") // разрешает запросы для пайтон клиента
public class GameController {

    @Autowired
    private GameService gameService;

    @GetMapping("/state")
    public GameState getState() {
        return gameService.getState();
    }

    
    @PostMapping("/new")
    public GameState newGame(@RequestParam(defaultValue = "4") int stones) {
        return gameService.newGame(stones);
    }

    
    @PostMapping("/move/{pit}")
    public MoveResult makeMove(@PathVariable int pit) {
        return gameService.makeMove(pit);
    }

    
    @PostMapping("/undo")
    public GameState undo() {
        return gameService.undo();
    }
}
