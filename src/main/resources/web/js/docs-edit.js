(function () {
  "use strict";
  const P = window.Perun;
  const article = document.getElementById("page");
  let busy = [];

  P.docEdit = {
    active: false,

    open: function (page) {
      // Переводит страницу в режим правки
      P.docEdit.active = true;
      const note = page.note || { summary: "", details: "", cases: [] };
      const summary = field("textarea", note.summary, "Одно-два предложения: что делает и зачем", 3);
      const details = field("textarea", note.details || "", "Подробности в Markdown: как работает, где хранятся данные",
        8);
      const cases = P.h("div", { class: "edit-cases" });
      (note.cases || []).forEach(function (item) {
        cases.append(caseRow(item));
      });
      const save = action("Сохранить", "primary", function () {
        submit(page, summary.value, details.value, cases);
      });
      busy = [save];
      article.innerHTML = "";
      article.append(
        P.h("header", { class: "page-head" }, [
          P.h("div", { class: "page-kicker" }, [P.docKinds.icon(page.kind, 14), "Правка описания"]),
          P.h("div", { class: "page-title" }, [P.h("h1", { text: page.title }), P.h("div", { class: "page-actions" }, [
            page.note ? remember(action("Удалить описание", "danger", function () {
              if (window.confirm("Удалить описание «" + page.title + "»?")) {
                lock(true);
                P.post("docsRemove", { id: page.id });
              }
            })) : null,
            remember(action("Отмена", "", function () {
              P.docEdit.close();
            })),
            save,
          ])]),
        ]),
        label("Кратко", "обязательно"), summary,
        label("Подробно", "Markdown"), details,
        label("Частные случаи", "условие и что происходит"), cases,
        P.h("button", { class: "btn add-case", type: "button", onclick: function () {
          cases.append(caseRow({ title: "", text: "" }));
        } }, [P.h("span", { html: P.icon("plus", 14) }), "Добавить"]),
        P.h("p", { class: "edit-hint", html: "<kbd>Ctrl Enter</kbd> — сохранить, <kbd>Esc</kbd> — отмена" })
      );
      article.onkeydown = function (event) {
        if (event.key === "Enter" && (event.ctrlKey || event.metaKey)) {
          event.preventDefault();
          submit(page, summary.value, details.value, cases);
        } else if (event.key === "Escape") {
          P.docEdit.close();
        }
      };
      summary.focus({ preventScroll: true });
    },

    close: function () {
      // Возвращает страницу к просмотру
      P.docEdit.active = false;
      article.onkeydown = null;
      if (P.docs.page) {
        P.docPage.render(P.docs.page);
      }
    },

    failed: function () {
      // Разблокирует форму после ошибки
      lock(false);
    },
  };

  function submit(page, summary, details, cases) {
    // Отправляет описание целиком
    if (!summary.trim()) {
      P.docsApp.toast("error", "Заполните поле «Кратко»");
      return;
    }
    const list = Array.prototype.map.call(cases.querySelectorAll(".case-row"), function (row) {
      return { title: row.querySelector("input").value.trim(), text: row.querySelector("textarea").value.trim() };
    }).filter(function (item) {
      return item.title || item.text;
    });
    lock(true);
    P.post("docsSave", { id: page.id, summary: summary.trim(), details: details.trim(), cases: list });
  }

  function caseRow(item) {
    // Создает строку частного случая
    const row = P.h("div", { class: "case-row" }, [
      field("input", item.title, "Условие, например «Склад не указан»"),
      field("textarea", item.text, "Что происходит", 2),
    ]);
    row.append(P.iconButton("x", "Удалить случай", function () {
      row.remove();
    }));
    return row;
  }

  function field(tag, value, placeholder, rows) {
    // Создает поле ввода формы
    const node = P.h(tag, { class: "edit-field", placeholder: placeholder, rows: rows || null, spellcheck: "true" });
    node.value = value || "";
    return node;
  }

  function label(text, hint) {
    // Создает подпись поля формы
    return P.h("label", { class: "edit-label" }, [text, hint ? P.h("span", { text: hint }) : null]);
  }

  function action(text, tone, onClick) {
    // Создает кнопку формы
    return P.h("button", { class: "btn" + (tone ? " " + tone : ""), type: "button", text: text, onclick: onClick });
  }

  function remember(button) {
    // Добавляет кнопку в блокируемые
    busy.push(button);
    return button;
  }

  function lock(value) {
    // Блокирует кнопки на время запроса
    busy.forEach(function (button) {
      button.disabled = value;
    });
  }
})();
