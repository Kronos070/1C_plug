(function () {
  "use strict";
  const P = window.Perun;
  const TITLES = { confirm: "Подтверждение", select: "Выбор", input: "Ввод", editor: "Редактирование" };

  P.on("request", function (event) {
    const request = event.request;
    const card = P.h("div", { class: "card" });
    card.append(P.h("div", { class: "card-title" }, [
      P.h("span", { html: P.icon(request.kind === "confirm" ? "shield" : "info", 16) }),
      P.h("span", { text: request.title || TITLES[request.kind] }),
    ]));
    if (request.message && request.kind === "confirm") {
      card.append(P.h("div", { class: "card-text", text: request.message }));
    }
    card.append(controls(request, card));
    P.stream.current().append(card);
    P.thread.scroll(true);
  });

  function controls(request, card) {
    // Создает элементы ответа на запрос
    const resolve = function (payload, summary) {
      // Отправляет ответ и сворачивает карточку
      P.post("answer", Object.assign({ id: request.id }, payload));
      card.className = "card resolved";
      card.textContent = summary + " · " + (request.title || TITLES[request.kind]);
    };
    const cancel = P.h("button", { class: "btn", type: "button", text: "Отмена", onclick: function () {
      resolve({ cancelled: true }, "Отменено");
    } });
    if (request.kind === "confirm") {
      return P.h("div", { class: "card-actions" }, [
        split("Разрешить", "primary", true, [
          ["once", "Разрешить один раз", "Разрешено один раз"],
          ["session", "Разрешить в этой сессии", "Разрешено до конца сессии"],
          ["always", "Разрешать всегда", "Разрешено всегда"],
        ], resolve),
        split("Отклонить", "danger", false, [
          ["once", "Отклонить один раз", "Отклонено"],
          ["never", "Никогда не разрешать", "Запрещено всегда"],
        ], resolve),
      ]);
    }
    if (request.kind === "select") {
      return P.h("div", { class: "card-actions" }, request.options.map(function (option) {
        return P.h("button", { class: "btn", type: "button", text: option, onclick: function () {
          resolve({ value: option }, "Выбрано: " + option);
        } });
      }).concat([cancel]));
    }
    const field = P.h("textarea", { placeholder: request.message || "" });
    field.value = request.prefill || "";
    setTimeout(function () {
      field.focus();
    }, 0);
    return P.h("div", {}, [field, P.h("div", { class: "card-actions" }, [
      P.h("button", { class: "btn primary", type: "button", text: "Отправить", onclick: function () {
        resolve({ value: field.value }, "Отправлено");
      } }),
      cancel,
    ])]);
  }

  function split(label, tone, confirmed, options, resolve) {
    // Создает кнопку с выпадающим выбором области
    const main = P.h("button", { class: "btn " + tone, type: "button", text: label, onclick: function () {
      resolve({ confirmed: confirmed, scope: options[0][0] }, options[0][2]);
    } });
    const more = P.h("button", { class: "btn " + tone + " split-more", type: "button", title: "Другие варианты",
      html: P.icon("chevronDown", 14) });
    more.addEventListener("click", function () {
      if (P.menu.isOpen(more)) {
        P.menu.close();
        return;
      }
      P.menu.open(more, {
        items: options.map(function (option) {
          return { id: option[0], label: option[1], summary: option[2] };
        }),
        onSelect: function (item) {
          resolve({ confirmed: confirmed, scope: item.id }, item.summary);
        },
      });
    });
    return P.h("div", { class: "split" }, [main, more]);
  }
})();
