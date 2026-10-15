(function () {
  "use strict";
  const P = window.Perun;
  const MODE_ICONS = { find: "find", explain: "explain", write: "write", plan: "plan" };
  let container;

  P.chips = {
    mount: function (element) {
      // Запоминает контейнер для чипов
      container = element;
    },

    render: function () {
      // Рисует чипы режима, модели и рассуждений
      if (!container || P.menu.isOpen(container.querySelector(".chip.open"))) {
        return;
      }
      const state = P.state;
      container.innerHTML = "";
      const mode = state.modes.find(function (item) {
        return item.id === state.mode;
      });
      if (state.modes.length) {
        container.append(chip("mode", MODE_ICONS[state.mode] || "chat", mode ? mode.label : "Режим", "accent",
          "Режим агента", state.modes.map(function (item) {
            return { id: item.id, label: item.label, hint: item.hint, icon: MODE_ICONS[item.id],
              active: item.id === state.mode };
          }), "mode"));
      }
      container.append(chip("model", "model", state.model ? state.model.name : "Модель не выбрана",
        state.model ? "" : "warn", "Модель", state.models.map(function (item) {
          return { id: item.key, label: item.name, hint: item.provider,
            active: Boolean(state.model && item.key === state.model.key) };
        }), "model", "Нет доступных моделей.<br>Подключите провайдера командой /login."));
      if (state.thinkingLevels.length > 1) {
        const level = state.thinkingLevels.find(function (item) {
          return item.id === state.thinking;
        });
        container.append(chip("thinking", "brain", level ? level.label : "Рассуждения", "", "Глубина рассуждений",
          state.thinkingLevels.map(function (item) {
            return { id: item.id, label: item.label, active: item.id === state.thinking };
          }), "thinking"));
      }
    },

    open: function (kind) {
      // Открывает меню выбранного чипа
      const button = container && container.querySelector('.chip[data-kind="' + kind + '"]');
      if (button) {
        button.click();
      } else {
        P.toast("info", kind === "thinking" ? "Модель не поддерживает выбор рассуждений" : "Недоступно");
      }
    },
  };

  function chip(kind, icon, label, tone, title, items, action, empty) {
    // Создает чип с выпадающим меню
    const button = P.h("button", { class: "chip" + (tone ? " " + tone : ""), type: "button", title: title,
      "data-kind": kind, html: P.icon(icon, 14) + "<span>" + P.escape(label) + "</span>" + P.icon("chevronDown", 13) });
    button.addEventListener("click", function () {
      if (P.menu.isOpen(button)) {
        P.menu.close();
        return;
      }
      P.menu.open(button, { title: title, items: items, empty: empty, search: items.length > 8,
        onClose: P.chips.render, onSelect: function (item) {
          P.post(action, action === "model" ? { key: item.id } : { id: item.id });
        } });
    });
    return button;
  }
})();
