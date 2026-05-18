package com.bantumi.controller;

import com.bantumi.service.GameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты REST API через MockMvc.
 * Проверяем HTTP-коды и базовую структуру ответов.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GameService gameService;

    /** Перед каждым тестом создаём свежую партию */
    @BeforeEach
    void setUp() {
        gameService.newGame(4, "PVP");
    }

    /**
     * Тест: GET /api/v1/game/state должен вернуть 200 OK с полем board.
     */
    @Test
    void testGetState_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/game/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.board").isArray())
                .andExpect(jsonPath("$.board.length()").value(14))
                .andExpect(jsonPath("$.currentPlayer").exists())
                .andExpect(jsonPath("$.gameOver").value(false));
    }

    /**
     * Тест: POST /api/v1/game/new должен вернуть 200 OK и сброшенное состояние.
     */
    @Test
    void testNewGame_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/game/new")
                        .param("stones", "4")
                        .param("mode", "PVP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.board").isArray())
                .andExpect(jsonPath("$.moveCount").value(0))
                .andExpect(jsonPath("$.currentPlayer").value(1))
                .andExpect(jsonPath("$.gameOver").value(false));
    }

    /**
     * Тест: POST /api/v1/game/new с режимом PVE должен вернуть корректный ответ.
     */
    @Test
    void testNewGame_pveMode_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/game/new")
                        .param("stones", "4")
                        .param("mode", "PVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.board").isArray())
                .andExpect(jsonPath("$.moveCount").value(0));
    }

    /**
     * Тест: POST /api/v1/game/move/1 (валидная лунка П1) должен вернуть 200 OK с valid=true.
     */
    @Test
    void testMakeMove_validPit_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/game/move/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.state").exists())
                .andExpect(jsonPath("$.state.board").isArray());
    }

    /**
     * Тест: POST /api/v1/game/move/9 (лунка П2, а ходит П1) должен вернуть valid=false.
     */
    @Test
    void testMakeMove_invalidPit_returnsValid_false() throws Exception {
        // Сейчас ход П1, он пытается ходить из лунки П2 (индекс 9)
        mockMvc.perform(post("/api/v1/game/move/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.reason").isNotEmpty());
    }

    /**
     * Тест: POST /api/v1/game/undo без предыдущих ходов должен вернуть 200 OK.
     */
    @Test
    void testUndo_returns200() throws Exception {
        // Делаем один ход, потом откатываем
        mockMvc.perform(post("/api/v1/game/move/0"));
        mockMvc.perform(post("/api/v1/game/undo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.board").isArray())
                .andExpect(jsonPath("$.moveCount").value(0));
    }

    /**
     * Тест: бонусный ход (лунка 2, при 4 камнях -> Калах) — currentPlayer остаётся 1.
     */
    @Test
    void testMakeMove_bonusTurn_playerStaysTheSame() throws Exception {
        // Лунка 2 + 4 камня = попадают в 3,4,5,6(Калах) — бонусный ход
        mockMvc.perform(post("/api/v1/game/move/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.bonusTurn").value(true))
                .andExpect(jsonPath("$.state.currentPlayer").value(1));
    }
}
