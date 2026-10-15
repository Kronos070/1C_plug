(function () {
  "use strict";
  const P = window.Perun;
  let input;
  let sendButton;

  P.composer = {
    render: function () {
      // Строит поле ввода с выбором модели
      input = P.h("textarea", { rows: "1", placeholder: "Спросите о конфигурации 1С · / — команды · @ — файлы",
        "aria-label": "Сообщение" });
      const chips = P.h("div", { class: "chips" });
      sendButton = P.h("button", { class: "send", type: "button", onclick: onButton });
      input.addEventListener("input", function () {
        resize();
        suggest();
        P.composer.update();
      });
      input.addEventListener("keydown", onKey);
      input.addEventListener("blur", function () {
        setTimeout(P.palette.close, 150);
      });
      const box = P.h("div", { class: "composer" }, [input, P.h("div", { class: "composer-bar" }, [chips, sendButton])]);
      P.chips.mount(chips);
      P.palette.attach(box);
      document.getElementById("composer").append(box,
        P.h("div", { class: "composer-hint", text: "Enter — отправить · Shift+Enter — новая строка · / — команды · @ — файлы" }));
      P.composer.update();
    },

    submit: function (text) {
      // Отправляет текст агенту или выполняет команду
      const raw = (text === undefined ? input.value : text).trim();
      if (!raw) {
        return;
      }
      if (P.commands.run(raw)) {
        P.composer.setText("");
        return;
      }
      if (P.state.connection !== "online" || P.state.streaming) {
        P.toast("warning", P.state.streaming ? "Дождитесь окончания ответа" : "Агент не подключен");
        return;
      }
      const value = P.commands.find(raw) ? P.commands.normalize(raw) : raw;
      P.thread.addUser(value);
      P.post("send", { text: value });
      P.stream.expect();
      P.composer.setText("");
    },

    setText: function (text) {
      // Подставляет текст в поле ввода
      input.value = text || "";
      resize();
      P.composer.focus();
      suggest();
      P.composer.update();
    },

    focus: function () {
      // Переводит фокус в поле ввода
      if (input) {
        input.focus();
        input.setSelectionRange(input.value.length, input.value.length);
      }
    },

    openChip: function (kind) {
      // Открывает меню модели или рассуждений
      P.chips.open(kind);
    },

    update: function () {
      // Обновляет кнопку и чипы выбора
      if (!input) {
        return;
      }
      const hasText = input.value.trim().length > 0;
      const stop = P.state.streaming;
      sendButton.className = "send" + (stop ? " stop" : "");
      sendButton.innerHTML = P.icon(stop ? "stop" : "send", stop ? 16 : 17);
      sendButton.title = stop ? "Остановить" : "Отправить";
      sendButton.disabled = !hasText && !stop;
      P.chips.render();
    },
  };

  function suggest() {
    // Показывает подсказки команд или файлов
    const query = P.mentions.query(input);
    if (query !== null) {
      P.mentions.request(query, function (files) {
        if (P.mentions.query(input) === query) {
          P.palette.show(files, acceptFile, "Файлы не найдены");
        }
      });
      return;
    }
    const commands = P.commands.suggest(input.value);
    if (commands) {
      P.palette.show(commands, acceptCommand, "Нет такой команды");
    } else {
      P.palette.close();
    }
  }

  function acceptFile(item) {
    // Вставляет выбранный файл в сообщение
    P.palette.close();
    P.mentions.insert(input, item.value);
    resize();
    input.focus();
    P.composer.update();
  }

  function acceptCommand(entry, execute) {
    // Подставляет выбранную команду в поле
    const item = entry.command;
    P.palette.close();
    const runNow = execute && !item.hint && item.kind !== "prompt";
    if (runNow) {
      P.composer.submit("/" + item.id);
    } else {
      input.value = "/" + item.id + " ";
      resize();
      P.composer.focus();
      P.composer.update();
    }
  }

  function onButton() {
    // Отправляет сообщение или останавливает ответ
    if (P.state.streaming) {
      P.post("abort");
    } else {
      P.composer.submit();
    }
  }

  function onKey(event) {
    // Обрабатывает список команд и Enter
    if (P.palette.handleKey(event)) {
      return;
    }
    if (event.key === "Enter" && !event.shiftKey && !event.isComposing) {
      event.preventDefault();
      P.composer.submit();
    }
  }

  function resize() {
    // Подгоняет высоту поля под текст
    input.style.height = "auto";
    input.style.height = Math.min(input.scrollHeight, 220) + "px";
  }

  P.on("streaming", P.composer.update);
  P.on("state", P.composer.update);
  P.on("connected", P.composer.update);
  P.on("disconnected", P.composer.update);
  P.on("editorText", function (event) {
    P.composer.setText(event.text);
  });
})();
