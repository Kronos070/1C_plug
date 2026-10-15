(function () {
  "use strict";
  const P = window.Perun;
  let turn = 0;
  let spinning = null;

  P.stream = {
    expect: function () {
      // Готовит ответ и показывает набор
      const reply = P.thread.reply || P.thread.newReply();
      reply.setTyping(true);
      P.thread.scroll(true);
      P.stream.setStreaming(true);
      return reply;
    },

    current: function () {
      // Возвращает текущий блок ответа
      return P.thread.reply || P.thread.newReply();
    },

    setStreaming: function (value) {
      // Обновляет флаг генерации ответа
      P.state.streaming = value;
      P.emit("streaming", value);
    },
  };

  P.on("streaming", function (active) {
    const logo = active && P.thread.reply ? P.thread.reply.logo : null;
    if (spinning && spinning !== logo) {
      spinning.spin(false);
    }
    spinning = logo;
    if (logo) {
      logo.spin(true);
    }
  });

  P.on("working", function () {
    P.stream.expect();
  });

  P.on("assistantStart", function () {
    turn += 1;
    P.stream.expect();
  });

  P.on("textDelta", function (event) {
    const reply = P.stream.current();
    reply.part("live:" + turn + ":" + event.index, function () {
      return P.parts.text("");
    }).append(event.delta);
    P.thread.scroll(P.thread.nearBottom());
  });

  P.on("thinkingDelta", function (event) {
    const reply = P.stream.current();
    reply.part("live:" + turn + ":" + event.index, function () {
      return P.parts.thinking("");
    }).append(event.delta);
  });

  P.on("toolStart", function (event) {
    const reply = P.stream.current();
    reply.part("tool:" + event.id, function () {
      return P.parts.tool({ name: event.name, args: event.args, running: true });
    });
    P.thread.scroll(P.thread.nearBottom());
  });

  P.on("toolEnd", function (event) {
    const reply = P.stream.current();
    reply.part("tool:" + event.id, function () {
      return P.parts.tool({ name: "tool", args: "", running: true });
    }).finish(event.output, event.failed);
  });

  P.on("assistantEnd", function (event) {
    const reply = P.stream.current();
    event.message.parts.forEach(function (part, index) {
      if (part.kind === "text" && part.text) {
        reply.part("live:" + turn + ":" + index, function () {
          return P.parts.text("");
        }).set(part.text);
      }
    });
    if (event.error) {
      reply.append(P.errorCard("Ошибка модели", event.error));
    }
  });

  P.on("settled", function (event) {
    const reply = P.thread.reply;
    if (reply) {
      if (event.aborted && P.state.streaming) {
        reply.append(P.h("div", { class: "muted", text: "Ответ остановлен" }));
      }
      reply.finish();
    }
    P.stream.setStreaming(false);
  });

  P.on("failure", function (event) {
    const reply = P.stream.current();
    reply.append(P.errorCard("Не удалось выполнить запрос", event.message));
    reply.setTyping(false);
    P.thread.scroll(true);
  });

  P.on("history", function (event) {
    turn = 0;
    P.thread.load(event.messages || []);
    P.stream.setStreaming(Boolean(P.state.streaming));
  });
})();
