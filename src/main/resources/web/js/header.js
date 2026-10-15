(function () {
  "use strict";
  const P = window.Perun;
  const header = document.getElementById("header");
  let title;
  let meta;
  let historyButton;

  P.header = {
    render: function () {
      // Строит шапку с историей и действиями
      title = P.h("strong", { text: "Новый чат" });
      meta = P.h("div", { class: "header-meta" });
      historyButton = P.iconButton("history", "История чатов", function () {
        P.history.toggle(historyButton);
      });
      header.append(
        P.h("div", { class: "brand", html: P.icon("bolt", 15) }),
        P.h("div", { class: "header-title" }, [title, meta]),
        P.h("div", { class: "header-actions" }, [
          historyButton,
          P.iconButton("plus", "Новый чат", function () {
            P.post("newChat");
          }),
          P.iconButton("settings", "Настройки ПерУН", function () {
            P.post("settings");
          }),
        ])
      );
      P.header.update();
    },

    openHistory: function () {
      // Открывает панель истории чатов
      if (historyButton && !P.menu.isOpen(historyButton)) {
        P.history.toggle(historyButton);
      }
    },

    update: function () {
      // Обновляет заголовок и статусы
      if (!title) {
        return;
      }
      const state = P.state;
      title.textContent = state.sessionName || P.thread.title() || "Новый чат";
      meta.innerHTML = "";
      const dot = { online: "online", connecting: "pending", offline: "offline" }[state.connection] || "pending";
      const label = { online: state.agent || "подключено", connecting: "подключение…", offline: "нет связи" };
      meta.append(P.h("span", { class: "status-dot " + dot }), P.h("span", { text: label[state.connection] }));
      const permissions = state.statuses["1c-permissions"];
      if (permissions) {
        meta.append(P.h("span", { class: "pill", text: permissions.replace(/^\[|\]$/g, "") }));
      }
    },
  };

  P.on("connecting", function (event) {
    P.state.connection = "connecting";
    P.state.agent = event.agent;
    P.header.update();
  });

  P.on("connected", function (event) {
    P.state.connection = "online";
    P.state.agent = event.agent;
    P.header.update();
  });

  P.on("disconnected", function () {
    P.state.connection = "offline";
    P.header.update();
  });

  P.on("status", function (event) {
    P.state.statuses[event.key] = event.text;
    P.header.update();
  });

  P.on("state", function (event) {
    Object.assign(P.state, event.state);
    P.header.update();
  });
})();
