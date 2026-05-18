"""
main.py — Графический интерфейс игры Бантуми
Автор: Старостин Максим (frontend, Python/Tkinter)

Требования:
    pip3 install requests
    Java-бэкенд должен быть запущен: cd backend && mvn spring-boot:run
"""

import tkinter as tk
from tkinter import messagebox
import requests

# ── Бэкенд ───────────────────────────────────────────────────────────────────
BASE_URL = "http://localhost:8080/api/v1/game"

# ── Цвета ────────────────────────────────────────────────────────────────────
BG          = "#1a0f02"
BOARD_BG    = "#5d3a1a"
BOARD_EDGE  = "#c8874a"
PIT_NORMAL  = "#3b2208"
PIT_ACTIVE  = "#7a4820"
PIT_HOVER   = "#9a5830"
KALAH_FILL  = "#2a1805"
TEXT_LIGHT  = "#f5e6c8"
TEXT_DIM    = "#a07848"
P1_COLOR    = "#e05040"
P2_COLOR    = "#4090d0"
AI_COLOR    = "#a060e0"   # фиолетовый для ИИ
GOLD        = "#f0c040"
BTN_BG      = "#7b4f27"
BTN_HOVER   = "#9a6535"
BTN_TEXT    = "#f5e6c8"


class BantumiApp:
    # ── Геометрия доски ──────────────────────────────────────────────────────
    CW, CH   = 760, 320          # размер Canvas
    PIT_R    = 33                # радиус лунки
    KALAH_RX = 46                # полуширина Калаха
    KALAH_RY = 112               # полувысота Калаха
    ROW_P2_Y = 100               # центр верхнего ряда (Игрок 2 / ИИ)
    ROW_P1_Y = 220               # центр нижнего ряда (Игрок 1)
    MID_Y    = 160               # середина доски
    KAL2_X   = 68                # центр Калаха Игрока 2 (слева)
    KAL1_X   = 692               # центр Калаха Игрока 1 (справа)
    PIT_XS   = [155, 248, 341, 434, 527, 620]   # X для 6 лунок

    def __init__(self):
        self.state     = None
        self.pit_items = {}   # oval_id → pit_index
        self.mode      = "PVP"  # текущий режим игры

        # Корневое окно — ПЕРВЫМ, потом все tk-переменные
        self.root = tk.Tk()
        self.root.title("Бантуми (Kalah)")
        self.root.configure(bg=BG)
        self.root.resizable(False, False)

        self.stones_var = tk.IntVar(value=4)
        self.mode_var   = tk.StringVar(value="PVP")

        self._build_ui()
        self._fetch_state()

    # ── Построение интерфейса ─────────────────────────────────────────────────

    def _build_ui(self):
        # Заголовок
        tk.Label(self.root, text="БАНТУМИ", bg=BG, fg=BOARD_EDGE,
                 font=("Arial", 26, "bold")).pack(pady=(14, 2))
        self.subtitle_lbl = tk.Label(
            self.root, text="Классическая игра Kalah  ·  2 игрока",
            bg=BG, fg=TEXT_DIM, font=("Arial", 10))
        self.subtitle_lbl.pack()

        # Настройки: количество камней
        cfg = tk.Frame(self.root, bg=BG)
        cfg.pack(pady=(8, 4))
        tk.Label(cfg, text="Камней в лунке:", bg=BG, fg=TEXT_DIM,
                 font=("Arial", 10)).pack(side=tk.LEFT, padx=(0, 6))
        for v in (3, 4, 5, 6):
            tk.Radiobutton(
                cfg, text=str(v), variable=self.stones_var, value=v,
                bg=BG, fg=TEXT_LIGHT, selectcolor="#5d3a1a",
                activebackground=BG, activeforeground=TEXT_LIGHT,
                font=("Arial", 10), command=self._new_game
            ).pack(side=tk.LEFT, padx=4)

        # Настройки: режим игры
        mf = tk.Frame(self.root, bg=BG)
        mf.pack(pady=(0, 4))
        tk.Label(mf, text="Режим игры:", bg=BG, fg=TEXT_DIM,
                 font=("Arial", 10)).pack(side=tk.LEFT, padx=(0, 6))
        tk.Radiobutton(
            mf, text="2 игрока (PvP)", variable=self.mode_var, value="PVP",
            bg=BG, fg=TEXT_LIGHT, selectcolor="#5d3a1a",
            activebackground=BG, activeforeground=TEXT_LIGHT,
            font=("Arial", 10), command=self._on_mode_change
        ).pack(side=tk.LEFT, padx=4)
        tk.Radiobutton(
            mf, text="Против ИИ (PvE)", variable=self.mode_var, value="PVE",
            bg=BG, fg=AI_COLOR, selectcolor="#5d3a1a",
            activebackground=BG, activeforeground=AI_COLOR,
            font=("Arial", 10), command=self._on_mode_change
        ).pack(side=tk.LEFT, padx=4)

        # Статус
        self.status_var = tk.StringVar(value="Ход: Игрок 1")
        self.status_lbl = tk.Label(
            self.root, textvariable=self.status_var,
            bg=BG, fg=P1_COLOR, font=("Arial", 13, "bold"))
        self.status_lbl.pack(pady=(2, 6))

        # Canvas — игровая доска
        self.canvas = tk.Canvas(
            self.root, width=self.CW, height=self.CH,
            bg=BG, highlightthickness=0)
        self.canvas.pack(padx=20)

        # Счёт
        sf = tk.Frame(self.root, bg=BG)
        sf.pack(pady=8)
        tk.Label(sf, text="Игрок 1:", bg=BG, fg=TEXT_DIM,
                 font=("Arial", 11)).grid(row=0, column=0, padx=8)
        self.score1_var = tk.StringVar(value="0")
        tk.Label(sf, textvariable=self.score1_var, bg=BG, fg=P1_COLOR,
                 font=("Arial", 20, "bold")).grid(row=0, column=1, padx=4)
        self.score2_label = tk.Label(sf, text="Игрок 2:", bg=BG, fg=TEXT_DIM,
                 font=("Arial", 11))
        self.score2_label.grid(row=0, column=2, padx=(24, 8))
        self.score2_var = tk.StringVar(value="0")
        self.score2_value_lbl = tk.Label(sf, textvariable=self.score2_var,
                 bg=BG, fg=P2_COLOR, font=("Arial", 20, "bold"))
        self.score2_value_lbl.grid(row=0, column=3, padx=4)

        # Кнопки (Frame+Label — единственный способ задать цвет на macOS)
        bf = tk.Frame(self.root, bg=BG)
        bf.pack(pady=6)
        self.btn_new  = self._make_btn(bf, "  Новая игра  ",  self._new_game)
        self.btn_new.pack(side=tk.LEFT, padx=8)
        self.btn_undo = self._make_btn(bf, "  Отменить ход  ", self._undo)
        self.btn_undo.pack(side=tk.LEFT, padx=8)

        # Журнал ходов
        lf = tk.Frame(self.root, bg=BG)
        lf.pack(pady=(2, 14), padx=20, fill=tk.X)
        tk.Label(lf, text="ЖУРНАЛ ХОДОВ", bg=BG, fg=TEXT_DIM,
                 font=("Arial", 8, "bold")).pack(anchor=tk.W)
        self.log_box = tk.Listbox(
            lf, height=4, bg="#110a02", fg=TEXT_DIM,
            font=("Courier", 9), selectbackground=BOARD_BG,
            borderwidth=0, highlightthickness=1,
            highlightcolor=BOARD_EDGE, relief=tk.FLAT)
        self.log_box.pack(fill=tk.X)

    def _make_btn(self, parent, text, cmd):
        """Кнопка через Frame+Label — цвет работает на macOS."""
        outer = tk.Frame(parent, bg=BOARD_EDGE, padx=1, pady=1)
        inner = tk.Frame(outer, bg=BTN_BG, cursor="hand2")
        inner.pack()
        lbl = tk.Label(inner, text=text, bg=BTN_BG, fg=BTN_TEXT,
                       font=("Arial", 11, "bold"), padx=10, pady=7)
        lbl.pack()
        for w in (outer, inner, lbl):
            w.bind("<Button-1>", lambda e: cmd())
            w.bind("<Enter>",    lambda e, l=lbl, i=inner: (
                l.config(bg=BTN_HOVER), i.config(bg=BTN_HOVER)))
            w.bind("<Leave>",    lambda e, l=lbl, i=inner: (
                l.config(bg=BTN_BG), i.config(bg=BTN_BG)))
        outer._label = lbl  # сохраняем ссылку для смены состояния
        return outer

    def _set_undo_state(self, enabled: bool):
        lbl   = self.btn_undo._label
        inner = lbl.master
        color = BTN_BG if enabled else "#3a2510"
        tcolor = BTN_TEXT if enabled else "#6a4828"
        lbl.config(bg=color, fg=tcolor)
        inner.config(bg=color)
        self.btn_undo.config(bg=BOARD_EDGE if enabled else "#5a3a18")

    # ── Отрисовка доски ───────────────────────────────────────────────────────

    def _draw_board(self):
        self.canvas.delete("all")
        self.pit_items.clear()
        if not self.state:
            return

        board   = self.state["board"]
        player  = self.state["currentPlayer"]
        landed  = self.state.get("lastLandedPit")
        over    = self.state["gameOver"]
        is_pve  = self.mode == "PVE"

        # Фон доски с рамкой
        self.canvas.create_rectangle(
            8, 8, self.CW - 8, self.CH - 8,
            fill=BOARD_BG, outline=BOARD_EDGE, width=3)

        # Метки строк: в PvE верхний ряд подписан "ИИ"
        p2_label = "▲ ИИ (Игрок 2)" if is_pve else "▲ ИГРОК 2"
        p2_color = AI_COLOR if is_pve else P2_COLOR
        self.canvas.create_text(
            self.KAL2_X + self.KALAH_RX + 10, self.ROW_P2_Y - self.PIT_R - 12,
            text=p2_label, anchor=tk.W, fill=p2_color,
            font=("Arial", 8, "bold"))
        self.canvas.create_text(
            self.KAL1_X - self.KALAH_RX - 10, self.ROW_P1_Y + self.PIT_R + 12,
            text="ИГРОК 1 ▼", anchor=tk.E, fill=P1_COLOR,
            font=("Arial", 8, "bold"))

        # Калах Игрока 2 (слева) — в PvE цвет фиолетовый
        self._draw_kalah(self.KAL2_X, self.MID_Y, board[13],
                         player=2, active=player == 2 and not over,
                         color_override=AI_COLOR if is_pve else None)
        # Калах Игрока 1 (справа)
        self._draw_kalah(self.KAL1_X, self.MID_Y, board[6],
                         player=1, active=player == 1 and not over)

        # Лунки Игрока 2: индексы 12→7, слева направо
        # В режиме PvE лунки П2 всегда некликабельны — ходит ИИ
        for col, pit_idx in enumerate([12, 11, 10, 9, 8, 7]):
            clickable = player == 2 and board[pit_idx] > 0 and not over and not is_pve
            self._draw_pit(self.PIT_XS[col], self.ROW_P2_Y,
                           board[pit_idx], pit_idx, clickable,
                           pit_idx == landed,
                           pit_color=AI_COLOR if is_pve else P2_COLOR)

        # Лунки Игрока 1: индексы 0→5, слева направо
        for col, pit_idx in enumerate([0, 1, 2, 3, 4, 5]):
            clickable = player == 1 and board[pit_idx] > 0 and not over
            self._draw_pit(self.PIT_XS[col], self.ROW_P1_Y,
                           board[pit_idx], pit_idx, clickable, pit_idx == landed)

    def _draw_kalah(self, cx, cy, count, player, active, color_override=None):
        color = color_override or (P1_COLOR if player == 1 else P2_COLOR)
        rx, ry = self.KALAH_RX, self.KALAH_RY

        # Внешнее свечение при активном ходе
        if active:
            self.canvas.create_oval(
                cx - rx - 5, cy - ry - 5, cx + rx + 5, cy + ry + 5,
                fill="", outline=color, width=2, dash=(4, 4))

        self.canvas.create_oval(
            cx - rx, cy - ry, cx + rx, cy + ry,
            fill=KALAH_FILL, outline=BOARD_EDGE, width=2)

        name = "Игрок 1" if player == 1 else ("ИИ" if self.mode == "PVE" else "Игрок 2")
        self.canvas.create_text(cx, cy - 48, text=name,
                                 fill=TEXT_DIM, font=("Arial", 8))
        self.canvas.create_text(cx, cy + 10, text=str(count),
                                 fill=color, font=("Arial", 24, "bold"))
        if active:
            arrow = "▶" if player == 1 else "◀"
            self.canvas.create_text(cx, cy + 46, text=arrow,
                                     fill=color, font=("Arial", 12))

    def _draw_pit(self, cx, cy, count, pit_idx, clickable, highlighted,
                  pit_color=None):
        r = self.PIT_R

        if highlighted:
            fill = "#906030"
        elif clickable:
            fill = PIT_ACTIVE
        else:
            fill = PIT_NORMAL

        outline_color = BOARD_EDGE if clickable else "#5a3a18"
        outline_w     = 2 if clickable else 1

        item = self.canvas.create_oval(
            cx - r, cy - r, cx + r, cy + r,
            fill=fill, outline=outline_color, width=outline_w)

        text_color = (pit_color or TEXT_LIGHT) if count > 0 else "#5a3a18"
        self.canvas.create_text(cx, cy - 7, text=str(count),
                                 fill=text_color, font=("Arial", 13, "bold"))

        # Декоративные точки-камни (до 6 штук)
        if 0 < count <= 6:
            positions = [(-8,-2),(0,-2),(8,-2),(-8,8),(0,8),(8,8)]
            for i in range(count):
                dx, dy = positions[i]
                self.canvas.create_oval(
                    cx+dx-4, cy+dy+4, cx+dx+4, cy+dy+12,
                    fill="#c8a060", outline="")

        if clickable:
            self.pit_items[item] = pit_idx
            self.canvas.tag_bind(item, "<Button-1>",
                                 lambda e, p=pit_idx: self._on_pit_click(p))
            self.canvas.tag_bind(item, "<Enter>",
                lambda e, i=item: self.canvas.itemconfig(i, fill=PIT_HOVER))
            self.canvas.tag_bind(item, "<Leave>",
                lambda e, i=item, f=fill: self.canvas.itemconfig(i, fill=f))

    # ── Обновление UI ─────────────────────────────────────────────────────────

    def _update_ui(self, log_text=None):
        self._draw_board()
        board  = self.state["board"]
        player = self.state["currentPlayer"]
        is_pve = self.mode == "PVE"

        self.score1_var.set(str(board[6]))
        self.score2_var.set(str(board[13]))

        # В PvE режиме — подпись «ИИ» и фиолетовый цвет
        if is_pve:
            self.score2_label.config(text="ИИ:")
            self.score2_value_lbl.config(fg=AI_COLOR)
        else:
            self.score2_label.config(text="Игрок 2:")
            self.score2_value_lbl.config(fg=P2_COLOR)

        if self.state["gameOver"]:
            w = self.state.get("winner")
            if w == 0:
                msg = "Ничья!"
            elif w == 1:
                msg = "Игрок 1 победил!"
            else:
                msg = "ИИ победил!" if is_pve else "Игрок 2 победил!"
            self.status_var.set(msg)
            self.status_lbl.configure(fg=GOLD)
            self._set_undo_state(False)
            self.root.after(600, lambda: self._show_result(w, board))
        else:
            if player == 2 and is_pve:
                # В PvE сюда попасть не должны (ИИ ходит на сервере),
                # но на всякий случай показываем
                self.status_var.set("ИИ думает...")
                self.status_lbl.configure(fg=AI_COLOR)
            elif player == 1:
                self.status_var.set("Ход: Игрок 1")
                self.status_lbl.configure(fg=P1_COLOR)
            else:
                self.status_var.set("Ход: Игрок 2")
                self.status_lbl.configure(fg=P2_COLOR)
            can_undo = self.state["moveCount"] > 0
            self._set_undo_state(can_undo)

        if log_text:
            self.log_box.insert(0, log_text)

    def _show_result(self, winner, board):
        is_pve = self.mode == "PVE"
        if winner == 0:
            msg = "Ничья! Оба игрока набрали поровну."
        elif winner == 1:
            msg = "Игрок 1 победил!"
        else:
            msg = "ИИ победил! Попробуйте ещё раз." if is_pve else "Игрок 2 победил!"
        detail = f"Счёт:  Игрок 1 — {board[6]},  {'ИИ' if is_pve else 'Игрок 2'} — {board[13]}"
        ans = messagebox.askquestion("Игра окончена",
                                     f"{msg}\n{detail}\n\nСыграть ещё раз?",
                                     icon="info")
        if ans == "yes":
            self._new_game()

    # ── Обработчики событий ───────────────────────────────────────────────────

    def _on_mode_change(self):
        """Смена режима игры — сразу начинаем новую партию."""
        self.mode = self.mode_var.get()
        is_pve = self.mode == "PVE"
        self.subtitle_lbl.config(
            text="Классическая игра Kalah  ·  Против ИИ" if is_pve
                 else "Классическая игра Kalah  ·  2 игрока"
        )
        self._new_game()

    def _on_pit_click(self, pit_idx):
        try:
            resp = requests.post(f"{BASE_URL}/move/{pit_idx}", timeout=5)
            resp.raise_for_status()
            data = resp.json()
        except requests.exceptions.RequestException as e:
            messagebox.showerror("Ошибка", f"Нет связи с бэкендом:\n{e}")
            return

        if not data.get("valid", True):
            return

        self.state = data["state"]

        # Определяем кто только что ходил
        who = self.state["currentPlayer"] if data.get("bonusTurn") \
              else (2 if self.state["currentPlayer"] == 1 else 1)
        pit_num = pit_idx + 1 if pit_idx <= 5 else pit_idx - 6
        who_name = f"Игрок {who}" if who == 1 or self.mode == "PVP" else "ИИ"
        log = f"#{self.state['moveCount']}  {who_name}: лунка {pit_num}"
        if data.get("bonusTurn"): log += " — бонусный ход!"
        if data.get("captured"):  log += " — захват!"

        self._update_ui(log_text=log)

    def _new_game(self):
        self.mode = self.mode_var.get()
        try:
            resp = requests.post(
                f"{BASE_URL}/new",
                params={"stones": self.stones_var.get(), "mode": self.mode},
                timeout=3)
            resp.raise_for_status()
            self.state = resp.json()
        except requests.exceptions.RequestException as e:
            messagebox.showerror("Ошибка", f"Нет связи с бэкендом:\n{e}")
            return
        self.log_box.delete(0, tk.END)
        self._update_ui()

    def _undo(self):
        if self.state and self.state["moveCount"] == 0:
            return
        try:
            resp = requests.post(f"{BASE_URL}/undo", timeout=3)
            resp.raise_for_status()
            self.state = resp.json()
        except requests.exceptions.RequestException as e:
            messagebox.showerror("Ошибка", f"Нет связи с бэкендом:\n{e}")
            return
        if self.log_box.size() > 0:
            self.log_box.delete(0)
        self._update_ui()

    def _fetch_state(self):
        try:
            resp = requests.get(f"{BASE_URL}/state", timeout=3)
            resp.raise_for_status()
            self.state = resp.json()
        except requests.exceptions.RequestException as e:
            messagebox.showerror(
                "Бэкенд недоступен",
                f"Ошибка соединения: {e}\n\n"
                "Запустите Java-сервер:\n"
                "  cd backend && mvn spring-boot:run")
            return
        try:
            self._update_ui()
        except Exception as e:
            messagebox.showerror("Ошибка интерфейса", str(e))

    def run(self):
        self.root.mainloop()


if __name__ == "__main__":
    BantumiApp().run()
