(function () {
  "use strict";
  const P = (window.Perun = window.Perun || {});
  const handlers = {};
  const outbox = [];
  let waiting = false;

  P.state = {
    connection: "connecting",
    agent: "",
    streaming: false,
    model: null,
    models: [],
    thinking: "off",
    thinkingLevels: [],
    mode: null,
    modes: [],
    sessionName: "",
    statuses: {},
  };

  P.on = function (type, handler) {
    // Подписывает обработчик на событие
    (handlers[type] = handlers[type] || []).push(handler);
  };

  P.emit = function (type, payload) {
    // Вызывает обработчики события
    (handlers[type] || []).forEach(function (handler) {
      try {
        handler(payload);
      } catch (error) {
        console.error("[perun]", type, error);
      }
    });
  };

  P.receiveAll = function (batch) {
    // Принимает пачку сообщений от Java
    batch.forEach(function (message) {
      P.emit(message.type, message);
    });
  };

  P.post = function (action, data) {
    // Отправляет команду в Java
    outbox.push(JSON.stringify(Object.assign({ action: action }, data || {})));
    flushOutbox();
  };

  function flushOutbox() {
    // Доставляет очередь, когда мост готов
    if (typeof window.perunPost !== "function") {
      if (!waiting) {
        waiting = true;
        setTimeout(function () {
          waiting = false;
          flushOutbox();
        }, 50);
      }
      return;
    }
    while (outbox.length) {
      window.perunPost(outbox.shift());
    }
  }

  P.h = function (tag, attrs, children) {
    // Создает DOM-элемент с атрибутами
    const node = document.createElement(tag);
    Object.entries(attrs || {}).forEach(function (entry) {
      const key = entry[0];
      const value = entry[1];
      if (value === null || value === undefined || value === false) {
        return;
      }
      if (key === "class") {
        node.className = value;
      } else if (key === "html") {
        node.innerHTML = value;
      } else if (key === "text") {
        node.textContent = value;
      } else if (key.startsWith("on")) {
        node.addEventListener(key.slice(2), value);
      } else {
        node.setAttribute(key, value === true ? "" : value);
      }
    });
    [].concat(children || []).forEach(function (child) {
      if (child !== null && child !== undefined && child !== false) {
        node.append(child instanceof Node ? child : document.createTextNode(String(child)));
      }
    });
    return node;
  };

  P.escape = function (text) {
    // Экранирует HTML-символы строки
    return String(text === null || text === undefined ? "" : text)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  };

  P.copy = function (text, button) {
    // Копирует текст и показывает отметку
    P.post("copy", { text: text });
    if (!button) {
      return;
    }
    const original = button.innerHTML;
    button.innerHTML = P.icon("check", 15);
    button.classList.add("active");
    setTimeout(function () {
      button.innerHTML = original;
      button.classList.remove("active");
    }, 1400);
  };

  P.relativeTime = function (millis) {
    // Форматирует время относительно сейчас
    const diff = Math.max(0, Date.now() - millis) / 1000;
    if (diff < 60) {
      return "только что";
    }
    if (diff < 3600) {
      return Math.floor(diff / 60) + " мин назад";
    }
    if (diff < 86400) {
      return Math.floor(diff / 3600) + " ч назад";
    }
    return new Date(millis).toLocaleDateString("ru-RU", { day: "numeric", month: "short" });
  };
})();
