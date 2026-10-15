(function () {
  "use strict";
  const P = window.Perun;

  P.rating = {
    attach: function (message, actions, answer) {
      // Добавляет оценку ответа под сообщением
      const form = P.h("div", { class: "rate-form" });
      const like = P.iconButton("thumbUp", "Хороший ответ", function () {
        send(message, "like", "", answer);
        mark(like, dislike);
        form.innerHTML = "";
      });
      const dislike = P.iconButton("thumbDown", "Плохой ответ", function () {
        mark(dislike, like);
        open(form, message, answer);
      });
      actions.append(like, dislike);
      actions.after(form);
    },

    question: function (node) {
      // Находит вопрос, на который дан ответ
      let message = node.closest(".msg");
      while (message && !message.classList.contains("user")) {
        message = message.previousElementSibling;
      }
      const bubble = message && message.querySelector(".bubble");
      return bubble ? bubble.textContent : "";
    },
  };

  function open(form, message, answer) {
    // Показывает поле «что не так»
    form.innerHTML = "";
    const field = P.h("textarea", { class: "field rate-text", placeholder: "Что не так? (необязательно)" });
    const recheck = P.h("input", { type: "checkbox", checked: true });
    const submit = function () {
      // Отправляет дизлайк с комментарием
      send(message, "dislike", field.value.trim(), answer);
      form.innerHTML = "";
      form.append(P.h("div", { class: "muted", text: recheck.checked ? "ПерУН перепроверит ответ и документацию"
        : "Спасибо, учтём" }));
      if (recheck.checked) {
        P.emit("ask", { text: task(field.value.trim(), answer(), P.rating.question(message)), mode: "",
          context: null });
      }
    };
    field.addEventListener("keydown", function (event) {
      if (event.key === "Enter" && (event.ctrlKey || event.metaKey)) {
        submit();
      }
    });
    form.append(field, P.h("label", { class: "rate-check" }, [recheck,
      "Пусть ПерУН перепроверит по коду и исправит документацию"]), P.h("div", { class: "card-actions" }, [
      P.h("button", { class: "btn primary", type: "button", text: "Отправить", onclick: submit }),
      P.h("button", { class: "btn", type: "button", text: "Без комментария", onclick: function () {
        field.value = "";
        submit();
      } }),
    ]));
    field.focus();
  }

  function task(comment, answer, question) {
    // Составляет задачу на разбор ошибки
    const cut = function (text, size) {
      const flat = String(text || "").replace(/\s+/g, " ").trim();
      return flat.length > size ? flat.slice(0, size) + "…" : flat;
    };
    return "Разработчик отметил твой ответ как неверный.\n" +
      "Что не так: " + (comment || "пояснения нет, найди ошибку сам") + "\n" +
      "Вопрос: «" + cut(question, 300) + "»\nНачало ответа: «" + cut(answer, 400) + "»\n\n" +
      "Перепроверь по коду проекта, не доверяя готовым описаниям: найди, где ответ ошибся, и дай исправленный ответ. " +
      "Затем исправь описания затронутых модулей и методов через 1c_docs_write. Если неверный ответ взят из " +
      "документации ПерУН, исправь это описание обязательно. Код проекта не меняй.";
  }

  function send(message, rating, comment, answer) {
    // Передает оценку ответа плагину
    P.post("rateChat", { rating: rating, comment: comment, answer: answer(),
      prompt: P.rating.question(message), model: P.state.model ? P.state.model.key : "" });
  }

  function mark(active, other) {
    // Подсвечивает выбранную оценку
    active.classList.add("active");
    other.classList.remove("active");
    active.parentElement.classList.add("rated");
  }
})();
