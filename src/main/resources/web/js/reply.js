(function () {
  "use strict";
  const P = window.Perun;

  P.createReply = function () {
    // Создает контейнер ответа ассистента
    const body = P.h("div", { class: "reply-body" });
    const actions = P.h("div", { class: "msg-actions" });
    const logo = P.logo.create(16);
    const el = P.h("div", { class: "msg assistant" }, [
      P.h("div", { class: "reply" }, [P.h("div", { class: "reply-avatar" }, [logo.el]), body]),
    ]);
    const parts = {};
    const order = [];
    let typing = null;

    function anchor() {
      // Возвращает узел для вставки перед ним
      return typing || (actions.parentNode ? actions : null);
    }

    const reply = {
      el: el,
      logo: logo,

      part: function (key, factory) {
        // Возвращает часть по ключу или создает
        if (!parts[key]) {
          parts[key] = factory();
          order.push(parts[key]);
          body.insertBefore(parts[key].node, anchor());
        }
        return parts[key];
      },

      addPart: function (key, data) {
        // Добавляет готовую часть из истории
        if (data.kind === "text") {
          reply.part(key, function () {
            return P.parts.text(data.text);
          });
        } else if (data.kind === "thinking" && data.text) {
          reply.part(key, function () {
            return P.parts.thinking(data.text);
          });
        } else if (data.kind === "tool") {
          reply.part("tool:" + data.id, function () {
            return P.parts.tool(data);
          });
        }
      },

      append: function (node) {
        // Добавляет произвольный узел в ответ
        body.insertBefore(node, anchor());
      },

      setTyping: function (active) {
        // Показывает или скрывает индикатор набора
        if (active && !typing) {
          typing = P.h("div", { class: "typing" }, [P.h("i"), P.h("i"), P.h("i")]);
          body.append(typing);
        } else if (!active && typing) {
          typing.remove();
          typing = null;
        }
      },

      text: function () {
        // Собирает текст ответа для копирования
        return order.filter(function (part) {
          return part.kind === "text";
        }).map(function (part) {
          return part.raw();
        }).join("\n\n").trim();
      },

      finish: function () {
        // Завершает ответ и добавляет действия
        reply.setTyping(false);
        if (!actions.childElementCount && reply.text()) {
          const copyButton = P.iconButton("copy", "Копировать ответ", function () {
            P.copy(reply.text(), copyButton);
          });
          actions.append(copyButton);
          body.append(actions);
        }
      },
    };
    return reply;
  };
})();
