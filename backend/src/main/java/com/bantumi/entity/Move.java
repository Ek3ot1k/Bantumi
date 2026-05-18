package com.bantumi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA-сущность для хранения одного хода в партии.
 * Используется для функции undo — boardSnapshot хранит состояние доски ДО этого хода.
 */
@Entity
@Table(name = "moves")
public class Move {

    /** Первичный ключ, генерируется автоматически */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Индекс лунки, из которой был сделан ход (0-12, без калахов) */
    @Column(name = "pit_index", nullable = false)
    private int pitIndex;

    /** Номер игрока, сделавшего ход (1 или 2) */
    @Column(name = "player_number", nullable = false)
    private int playerNumber;

    /** Порядковый номер хода в партии */
    @Column(name = "move_number", nullable = false)
    private int moveNumber;

    /** Был ли бонусный ход (последний камень попал в Калах) */
    @Column(name = "bonus_turn", nullable = false)
    private boolean bonusTurn;

    /** Был ли захват (последний камень в пустую свою лунку с камнями напротив) */
    @Column(name = "captured", nullable = false)
    private boolean captured;

    /**
     * Снимок состояния доски ДО этого хода (для функции undo).
     * Формат: 14 чисел через запятую + ";" + currentPlayer + ";" + winner
     */
    @Column(name = "board_snapshot", nullable = false, length = 200)
    private String boardSnapshot;

    /** Дата и время совершения хода */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Партия, к которой относится этот ход */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    public Move() {}

    /** Устанавливаем временную метку при создании */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // --- Геттеры и сеттеры ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getPitIndex() { return pitIndex; }
    public void setPitIndex(int pitIndex) { this.pitIndex = pitIndex; }

    public int getPlayerNumber() { return playerNumber; }
    public void setPlayerNumber(int playerNumber) { this.playerNumber = playerNumber; }

    public int getMoveNumber() { return moveNumber; }
    public void setMoveNumber(int moveNumber) { this.moveNumber = moveNumber; }

    public boolean isBonusTurn() { return bonusTurn; }
    public void setBonusTurn(boolean bonusTurn) { this.bonusTurn = bonusTurn; }

    public boolean isCaptured() { return captured; }
    public void setCaptured(boolean captured) { this.captured = captured; }

    public String getBoardSnapshot() { return boardSnapshot; }
    public void setBoardSnapshot(String boardSnapshot) { this.boardSnapshot = boardSnapshot; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }
}
