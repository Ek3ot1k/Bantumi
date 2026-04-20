package com.bantumi.model;

/**
 * Результат выполненного хода.
 * Автор: Хусейнов Амин
 */
public class MoveResult {

    // Признак корректного хода
    private boolean valid;

    // Причина отказа (если valid = false)
    private String reason;

    // Флаг бонусного хода (последний камень попал в свой Калах)
    private boolean bonusTurn;

    // Флаг захвата (последний камень попал в пустую свою лунку)
    private boolean captured;

    // Текущее состояние доски после хода
    private GameState state;

    public MoveResult() {}

    public MoveResult(boolean valid, String reason,
                      boolean bonusTurn, boolean captured, GameState state) {
        this.valid     = valid;
        this.reason    = reason;
        this.bonusTurn = bonusTurn;
        this.captured  = captured;
        this.state     = state;
    }

    public boolean isValid()              { return valid; }
    public void    setValid(boolean v)    { this.valid = v; }

    public String getReason()             { return reason; }
    public void   setReason(String r)     { this.reason = r; }

    public boolean isBonusTurn()          { return bonusTurn; }
    public void    setBonusTurn(boolean b){ this.bonusTurn = b; }

    public boolean isCaptured()           { return captured; }
    public void    setCaptured(boolean c) { this.captured = c; }

    public GameState getState()           { return state; }
    public void      setState(GameState s){ this.state = s; }
}
