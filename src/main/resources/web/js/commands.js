(function () {
  "use strict";
  const P = window.Perun;
  const PATTERN = /^\/([\w:.\-]+)(?:\s+([\s\S]*))?$/;
  const QUERY = /^[\/\\]([\w:.\-]*)$/;
  let items = [];

  P.commands = {
    all: function () {
      // Возвращает известные слэш-команды
      return items;
    },

    normalize: function (text) {
      // Заменяет ведущий обратный слеш на прямой
      return /^\\[\w:.\-]/.test(text) ? "/" + text.slice(1) : text;
    },

    find: function (text) {
      // Находит команду по введенному тексту
      const match = PATTERN.exec(P.commands.normalize(text));
      if (!match) {
        return null;
      }
      const command = items.find(function (item) {
        return item.id === match[1];
      });
      return command ? { command: command, args: (match[2] || "").trim() } : null;
    },

    suggest: function (text) {
      // Подбирает команды под введенный текст
      const match = QUERY.exec(text);
      if (!match) {
        return null;
      }
      const query = match[1].toLowerCase();
      const starts = items.filter(function (item) {
        return item.id.toLowerCase().startsWith(query);
      });
      const contains = items.filter(function (item) {
        return !starts.includes(item) && query && (item.id + " " + item.label).toLowerCase().includes(query);
      });
      return starts.concat(contains).map(function (item) {
        return { title: "/" + item.id, hint: item.hint, desc: item.label, command: item };
      });
    },

    run: function (text) {
      // Выполняет команду, если текст ей является
      const found = P.commands.find(text);
      if (!found || found.command.kind === "prompt") {
        return false;
      }
      if (found.command.kind === "client") {
        runClient(found.command.id, found.args);
        return true;
      }
      if (P.state.connection !== "online") {
        P.toast("warning", "Агент не подключен");
        return true;
      }
      P.post("send", { text: P.commands.normalize(text) });
      return true;
    },
  };

  function runClient(name, args) {
    // Выполняет команду на стороне интерфейса
    if (name === "login") {
      P.login.open(args);
    } else if (name === "model" || name === "thinking") {
      P.composer.openChip(name);
    } else if (name === "history") {
      P.header.openHistory();
    } else if (name === "settings") {
      P.post("settings");
    } else if (name === "help") {
      P.composer.setText("/");
    }
  }

  P.on("commands", function (event) {
    items = event.items || [];
  });
})();
