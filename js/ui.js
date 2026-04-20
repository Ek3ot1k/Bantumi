/**
 * ui.js — Пользовательский интерфейс (режим двух игроков)
 * Автор: Старостин Максим (frontend)
 * Review: Хусейнов А.
 */

class BantumiUI {
  constructor() {
    this.game = new BantumiGame(4);
    this.moveCount = 0;

    this._cacheDOM();
    this._bindEvents();
    this._render();
  }

  // Кэшируем ссылки на DOM-элементы
  _cacheDOM() {
    this.dom = {
      rowP1:         document.getElementById('row-p1'),
      rowP2:         document.getElementById('row-p2'),
      kalahP1Count:  document.getElementById('kalah-p1-count'),
      kalahP2Count:  document.getElementById('kalah-p2-count'),
      indicator1:    document.getElementById('indicator-p1'),
      indicator2:    document.getElementById('indicator-p2'),
      statusBar:     document.getElementById('status-bar'),
      scoreP1:       document.getElementById('score-p1'),
      scoreP2:       document.getElementById('score-p2'),
      btnNew:        document.getElementById('btn-new'),
      btnUndo:       document.getElementById('btn-undo'),
      stonesRange:   document.getElementById('stones-range'),
      stonesVal:     document.getElementById('stones-val'),
      moveLog:       document.getElementById('move-log'),
      overlay:       document.getElementById('overlay'),
      overlayResult: document.getElementById('overlay-result'),
      overlayScores: document.getElementById('overlay-scores'),
      btnPlayAgain:  document.getElementById('btn-play-again'),
      rulesToggle:   document.getElementById('rules-toggle'),
      rulesContent:  document.getElementById('rules-content'),
    };
  }

  _bindEvents() {
    this.dom.btnNew.addEventListener('click', () => this._newGame());
    this.dom.btnUndo.addEventListener('click', () => this._undo());
    this.dom.btnPlayAgain.addEventListener('click', () => {
      this.dom.overlay.classList.remove('visible');
      this._newGame();
    });
    this.dom.stonesRange.addEventListener('input', () => {
      this.dom.stonesVal.textContent = this.dom.stonesRange.value;
    });
    this.dom.stonesRange.addEventListener('change', () => this._newGame());
    this.dom.rulesToggle.addEventListener('click', () => {
      this.dom.rulesToggle.classList.toggle('open');
      this.dom.rulesContent.classList.toggle('open');
    });
  }

  _newGame() {
    const stones = parseInt(this.dom.stonesRange.value, 10);
    this.game = new BantumiGame(stones);
    this.moveCount = 0;
    this.dom.moveLog.innerHTML = '';
    this._render();
  }

  // Основная отрисовка доски
  _render() {
    this._renderRow(this.dom.rowP1, this.game.getPits(1));
    this._renderRow(this.dom.rowP2, this.game.getPits(2));
    this._renderKalah();
    this._renderStatus();
    this._renderScores();
    this.dom.btnUndo.disabled = this.game.history.length === 0 || this.game.gameOver;
  }

  _renderRow(container, pits) {
    container.innerHTML = '';
    pits.forEach(idx => container.appendChild(this._createPitEl(idx)));
  }

  _createPitEl(idx) {
    const div = document.createElement('div');
    div.className = 'pit';
    div.dataset.idx = idx;

    const count = this.game.board[idx];
    const player = idx <= 5 ? 1 : 2;
    const clickable = this.game.currentPlayer === player && count > 0 && !this.game.gameOver;

    if (!clickable) {
      div.classList.add('disabled');
      if (count === 0) div.classList.add('empty');
    } else {
      div.addEventListener('click', () => this._onPitClick(idx));
    }

    // Подсветка последней лунки и захвата
    if (idx === this.game.lastLandedPit) div.classList.add('last-landed');
    if (idx === this.game.capturedFrom)  div.classList.add('captured-highlight');

    // Число камней
    const stoneCount = document.createElement('div');
    stoneCount.className = 'stone-count';
    stoneCount.textContent = count;
    div.appendChild(stoneCount);

    // Декоративные точки-камни (до 12 штук)
    if (count > 0 && count <= 12) {
      const visual = document.createElement('div');
      visual.className = 'stones-visual';
      for (let i = 0; i < count; i++) {
        const dot = document.createElement('div');
        dot.className = 'stone-dot';
        visual.appendChild(dot);
      }
      div.appendChild(visual);
    }

    return div;
  }

  _renderKalah() {
    this.dom.kalahP1Count.textContent = this.game.board[6];
    this.dom.kalahP2Count.textContent = this.game.board[13];

    const p1Active = this.game.currentPlayer === 1 && !this.game.gameOver;
    const p2Active = this.game.currentPlayer === 2 && !this.game.gameOver;
    this.dom.indicator1.classList.toggle('active', p1Active);
    this.dom.indicator2.classList.toggle('active', p2Active);
  }

  _renderStatus() {
    const bar = this.dom.statusBar;
    if (this.game.gameOver) {
      bar.className = 'gameover';
      bar.textContent = this.game.winner === 0
        ? 'Ничья!'
        : `Игрок ${this.game.winner} победил!`;
    } else {
      const p = this.game.currentPlayer;
      bar.className = p === 1 ? 'p1-turn' : 'p2-turn';
      bar.textContent = `Ход: Игрок ${p}`;
    }
  }

  _renderScores() {
    this.dom.scoreP1.textContent = this.game.board[6];
    this.dom.scoreP2.textContent = this.game.board[13];
  }

  // Клик по лунке
  _onPitClick(idx) {
    if (this.game.gameOver) return;

    const result = this.game.makeMove(idx);
    if (!result.valid) return;

    this.moveCount++;
    this._logMove(idx, result);
    this._animatePit(idx);
    this._render();

    if (result.gameOver) {
      setTimeout(() => this._showOverlay(), 600);
    }
  }

  _animatePit(idx) {
    document.querySelectorAll('.pit').forEach(el => {
      if (parseInt(el.dataset.idx, 10) === idx) {
        el.classList.add('active-pit');
        setTimeout(() => el.classList.remove('active-pit'), 600);
      }
    });
  }

  _undo() {
    if (this.game.history.length === 0) return;
    const prev = this.game.history.pop();
    this.game.board = prev.board;
    this.game.currentPlayer = prev.player;
    this.game.gameOver = false;
    this.game.winner = null;
    this.game.lastLandedPit = null;
    this.game.capturedFrom = null;
    this._render();
    if (this.dom.moveLog.firstChild) this.dom.moveLog.removeChild(this.dom.moveLog.firstChild);
  }

  _logMove(pitIdx, result) {
    const li = document.createElement('li');
    // Показываем номер лунки 1-6 для обоих игроков
    const pitNum = pitIdx <= 5 ? pitIdx + 1 : pitIdx - 6;
    // Игрок, который только что ходил (ход уже применён, currentPlayer мог смениться)
    const whoMoved = result.bonusTurn
      ? this.game.currentPlayer        // бонусный ход — тот же игрок
      : (this.game.currentPlayer === 1 ? 2 : 1); // иначе — предыдущий

    let text = `#${this.moveCount} Игрок ${whoMoved}: лунка ${pitNum}`;
    if (result.bonusTurn) { text += ' — бонусный ход!'; li.classList.add('bonus'); }
    if (result.captured)  { text += ' — захват!';       li.classList.add('capture'); }

    li.textContent = text;
    this.dom.moveLog.insertBefore(li, this.dom.moveLog.firstChild);
  }

  _showOverlay() {
    const { winner } = this.game;
    this.dom.overlayResult.textContent = winner === 0
      ? 'Ничья! Оба набрали поровну.'
      : `🏆 Игрок ${winner} победил!`;
    this.dom.overlayScores.textContent =
      `Счёт: Игрок 1 — ${this.game.board[6]},  Игрок 2 — ${this.game.board[13]}`;
    this.dom.overlay.classList.add('visible');
  }
}

// Запуск после загрузки DOM
document.addEventListener('DOMContentLoaded', () => {
  window.bantumiUI = new BantumiUI();
});
