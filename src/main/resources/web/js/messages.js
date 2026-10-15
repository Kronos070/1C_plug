(function () {
  "use strict";
  const P = window.Perun;
  const SUGGESTIONS = [
    { title: "Объяснить код", text: "Объясни, что делает выбранный модуль и его основные процедуры" },
    { title: "Найти данные", text: "Где в конфигурации хранятся цены номенклатуры?" },
    { title: "Документация", text: "Напиши документацию по единому шаблону для общего модуля" },
    { title: "План доработки", text: "Составь план доработки документа «Заказ клиента»" },
  ];
  let thread;
  let chat;
  let firstUser = "";
  let offline = null;

  P.thread = {
    reply: null,

    init: function () {
      // Готовит ленту сообщений
      thread = document.getElementById("thread");
      chat = document.getElementById("chat");
      P.thread.showEmpty();
    },

    title: function () {
      // Возвращает заголовок по первому вопросу
      return firstUser.length > 60 ? firstUser.slice(0, 60) + "…" : firstUser;
    },

    clear: function () {
      // Очищает ленту сообщений
      thread.innerHTML = "";
      P.thread.reply = null;
      firstUser = "";
      offline = null;
    },

    showEmpty: function () {
      // Показывает приветствие с подсказками
      P.thread.clear();
      thread.append(P.h("div", { class: "empty" }, [
        P.h("div", { class: "empty-logo", html: P.icon("bolt", 26) }),
        P.h("h1", { text: "Чем помочь?" }),
        P.h("p", { text: "ПерУН знает вашу конфигурацию 1С: объяснит код, найдет данные и напишет документацию." }),
        P.h("div", { class: "suggestions" }, SUGGESTIONS.map(function (item) {
          return P.h("button", { class: "suggestion", type: "button", onclick: function () {
            P.composer.submit(item.text);
          } }, [P.h("b", { text: item.title }), item.text]);
        })),
      ]));
    },

    load: function (messages) {
      // Отрисовывает историю сессии целиком
      if (!messages.length) {
        P.thread.showEmpty();
        P.header.update();
        return;
      }
      P.thread.clear();
      messages.forEach(function (message) {
        if (message.role === "user") {
          const parsed = P.context.parse(message.parts.map(function (part) {
            return part.text || "";
          }).join("\n"));
          P.thread.addUser(parsed.text, parsed.context);
        } else {
          const reply = P.thread.reply || P.thread.newReply();
          message.parts.forEach(function (part, index) {
            reply.addPart(message.id + ":" + index, part);
          });
          reply.finish();
        }
      });
      P.header.update();
      P.thread.scroll(true);
    },

    addUser: function (text, context) {
      // Добавляет сообщение пользователя
      if (thread.querySelector(".empty")) {
        P.thread.clear();
      }
      firstUser = firstUser || text.replace(/\s+/g, " ").trim();
      const copyButton = P.iconButton("copy", "Копировать", function () {
        P.copy(text, copyButton);
      });
      P.thread.append(P.h("div", { class: "msg user" }, [
        P.h("div", { class: "bubble" }, [context ? P.context.chip(context) : null, text]),
        P.h("div", { class: "msg-actions" }, [copyButton]),
      ]));
      P.thread.reply = null;
      P.header.update();
      P.thread.scroll(true);
    },

    newReply: function () {
      // Создает блок ответа ассистента
      if (thread.querySelector(".empty")) {
        P.thread.clear();
      }
      P.thread.reply = P.createReply();
      P.thread.append(P.thread.reply.el);
      return P.thread.reply;
    },

    append: function (node) {
      // Добавляет узел в конец ленты
      const stick = P.thread.nearBottom();
      thread.append(node);
      P.thread.scroll(stick);
    },

    showOffline: function (reason) {
      // Показывает карточку отсутствия связи
      if (offline) {
        offline.remove();
      }
      offline = P.errorCard("Агент недоступен", reason, [
        P.h("button", { class: "btn primary", type: "button", html: P.icon("refresh", 14) + "Переподключить",
          onclick: function () {
            P.post("retry");
          } }),
        P.h("button", { class: "btn", type: "button", text: "Настройки", onclick: function () {
          P.post("settings");
        } }),
      ]);
      P.thread.append(offline);
    },

    hideOffline: function () {
      // Убирает карточку отсутствия связи
      if (offline) {
        offline.remove();
        offline = null;
      }
    },

    nearBottom: function () {
      // Проверяет, что лента прокручена вниз
      return chat.scrollHeight - chat.scrollTop - chat.clientHeight < 80;
    },

    scroll: function (force) {
      // Прокручивает ленту к последнему сообщению
      if (force) {
        chat.scrollTop = chat.scrollHeight;
      }
    },
  };
})();
