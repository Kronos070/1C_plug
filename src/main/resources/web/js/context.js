(function () {
  "use strict";
  const P = window.Perun;
  const BLOCK = /\n\n<context file="([^"]*)"(?: lines="(\d+)-(\d+)")?>\n[\s\S]*\n<\/context>\s*$/;
  const dismissed = new Set();
  let slot = null;
  let current = null;
  let pendingAsk = null;
  let historyReady = false;

  P.context = {
    mount: function (element) {
      // Запоминает место плашки в поле ввода
      slot = element;
      render();
    },

    active: function () {
      // Возвращает прикрепленный и не скрытый код
      return current && !dismissed.has(current.id) ? current : null;
    },

    consume: function (context) {
      // Открепляет отправленный фрагмент кода
      if (context && context.kind === "selection") {
        dismissed.add(context.id);
        render();
      }
    },

    chip: function (context, onClose) {
      // Создает плашку прикрепленного кода
      const selection = context.kind === "selection";
      const node = P.h("div", { class: "ctx-chip " + context.kind, title: context.path + (selection ? ":" +
        range(context) : "") }, [
        P.h("span", { class: "ctx-icon", html: P.icon(selection ? "code" : "file", 13) }),
        P.h("span", { class: "ctx-name", text: label(context.path) }),
        P.h("span", { class: "ctx-lines", text: selection ? "стр. " + range(context) : "текущий файл" }),
      ]);
      if (onClose) {
        node.append(P.h("button", { class: "ctx-close", type: "button", title: "Не прикреплять",
          "aria-label": "Не прикреплять", html: P.icon("x", 12), onclick: onClose }));
      }
      return node;
    },

    parse: function (text) {
      // Отделяет прикрепленный код от вопроса
      text = text.replace(/\n\n<project-rules>[\s\S]*?<\/project-rules>/, "");
      const match = BLOCK.exec(text);
      if (!match) {
        return { text: text, context: null };
      }
      return { text: text.slice(0, match.index), context: { path: match[1].replace(/&quot;/g, '"'),
        kind: match[2] ? "selection" : "file", startLine: Number(match[2] || 0), endLine: Number(match[3] || 0) } };
    },
  };

  function render() {
    // Перерисовывает плашку над полем ввода
    if (!slot) {
      return;
    }
    slot.innerHTML = "";
    const context = P.context.active();
    if (context) {
      slot.append(P.context.chip(context, function () {
        dismissed.add(context.id);
        render();
        P.composer.focus();
      }));
    }
  }

  function label(path) {
    // Возвращает короткое имя файла модуля
    const parts = String(path).split(/[\\/]/).filter(Boolean);
    const file = parts[parts.length - 1] || path;
    return /Module\.bsl$/i.test(file) && parts.length > 1 ? parts[parts.length - 2] + "/" + file : file;
  }

  function range(context) {
    // Форматирует диапазон строк фрагмента
    return context.startLine === context.endLine ? String(context.startLine)
      : context.startLine + "–" + context.endLine;
  }

  function ready() {
    // Проверяет, можно ли отправить запрос
    return P.state.connection === "online" && historyReady && !P.state.streaming;
  }

  function flush() {
    // Отправляет отложенный запрос из меню
    if (!pendingAsk || !ready()) {
      return;
    }
    const ask = pendingAsk;
    pendingAsk = null;
    P.composer.submit(ask.text, { context: ask.context, mode: ask.mode });
  }

  P.on("context", function (event) {
    current = event.context || null;
    render();
  });

  P.on("ask", function (event) {
    current = event.context;
    dismissed.delete(current.id);
    render();
    pendingAsk = event;
    if (!ready()) {
      P.toast("info", P.state.streaming ? "Запрос будет отправлен после текущего ответа"
        : "Запрос будет отправлен после подключения агента");
    }
    flush();
  });

  P.on("history", function () {
    historyReady = true;
    flush();
  });

  P.on("streaming", flush);

  P.on("connecting", function () {
    historyReady = false;
  });

  P.on("disconnected", function () {
    historyReady = false;
  });
})();
