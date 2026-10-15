(function () {
  "use strict";
  const P = window.Perun;
  const header = document.getElementById("header");
  let title;
  let dot;
  let historyButton;

  P.header = {
    render: function () {
      // Строит шапку с историей и действиями
      title = P.h("strong", { text: "Новый чат" });
      dot = P.h("span", { class: "status-dot pending" });
      historyButton = P.iconButton("history", "История чатов", function () {
        P.history.toggle(historyButton);
      });
      header.append(
        P.h("div", { class: "brand" }, [P.logo.create(24, { egg: true }).el]),
        P.h("div", { class: "header-title" }, [title, dot]),
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
      title.title = title.textContent;
      const tone = { online: "online", connecting: "pending", offline: "offline" }[state.connection] || "pending";
      const label = { online: "Агент подключен", connecting: "Подключение…", offline: "Нет связи с агентом" };
      dot.className = "status-dot " + tone;
      dot.title = label[state.connection] || "";
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
