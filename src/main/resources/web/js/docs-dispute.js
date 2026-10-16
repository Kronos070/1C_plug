(function () {
  "use strict";
  const P = window.Perun;

  P.docDispute = {
    open: function (page, anchor) {
      // Показывает форму «описание неверно»
      const existing = document.querySelector(".dispute");
      if (existing) {
        existing.remove();
        return;
      }
      const field = P.h("textarea", { class: "edit-field", rows: 3,
        placeholder: "Что не так в описании? Например: «остаток считается без резервов»" });
      const send = function () {
        // Отправляет замечание агенту
        const comment = field.value.trim();
        if (!comment) {
          P.docsApp.toast("warning", "Опишите, что не так");
          return;
        }
        P.post("askChat", { text: task(page, comment) });
        box.remove();
        P.docsApp.toast("info", "ПерУН перепроверит описание, ход работы виден в чате");
      };
      field.addEventListener("keydown", function (event) {
        if (event.key === "Enter" && (event.ctrlKey || event.metaKey)) {
          event.preventDefault();
          send();
        } else if (event.key === "Escape") {
          box.remove();
        }
      });
      const box = P.h("div", { class: "dispute" }, [
        P.h("strong", { text: "Описание неверно" }),
        P.h("p", { class: "muted", text: "ПерУН перепроверит запись по коду и исправит описание." }),
        field,
        P.h("div", { class: "page-actions" }, [
          P.h("button", { class: "btn primary", type: "button", text: "Отправить ПерУН", onclick: send }),
          P.h("button", { class: "btn", type: "button", text: "Отмена", onclick: function () {
            box.remove();
          } }),
        ]),
      ]);
      anchor.after(box);
      field.focus({ preventScroll: true });
    },
  };

  function task(page, comment) {
    // Составляет задачу на исправление описания
    return "Описание «" + page.title + "» (" + page.id + ") в документации ПерУН неверно.\n" +
      "Что не так: " + comment + "\n\n" +
      "Прочитай текущее описание через 1c_docs_read, перепроверь его по коду, не доверяя старому тексту, " +
      "и сохрани исправленное описание через 1c_docs_write. Коротко напиши, что было неверно. Код проекта не меняй.";
  }
})();
