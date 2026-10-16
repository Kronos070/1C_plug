(function () {
  "use strict";
  const P = window.Perun;
  const article = document.getElementById("page");

  P.docPage = {
    loading: function () {
      // Показывает заготовку загрузки
      article.innerHTML = "";
      article.append(P.h("div", { class: "skeleton" }, [P.h("i", { class: "title" }), P.h("i"), P.h("i"),
        P.h("i", { class: "short" })]));
    },

    render: function (page) {
      // Рисует страницу модуля или символа
      P.docs.page = page;
      article.innerHTML = "";
      const parts = P.docParts;
      [head(page), page.signature ? P.h("pre", { class: "page-signature", html: P.highlight(page.signature, "bsl") })
        : null, meta(page), page.note && page.note.stale ? staleNote(page) : null, parts.description(page),
      parts.cases(page.note), P.docGraph.section(page), parts.parameters(page.parameters), parts.comment(page.comment),
      parts.calls(page.calls), parts.members(page.members)].forEach(function (part) {
        if (part) {
          article.append(part);
        }
      });
    },

    error: function (text) {
      // Показывает ошибку открытия записи
      article.innerHTML = "";
      article.append(P.h("div", { class: "page-error" }, [P.h("span", { html: P.icon("alert", 18) }),
        P.h("div", {}, [P.h("strong", { text: "Не удалось открыть запись" }), P.h("p", { text: text })])]));
    },

    ask: function (page) {
      // Просит ПерУН объяснить и описать запись
      P.post("askChat", { text: "Объясни, что делает " + page.title + " (" + page.id + "), и обнови документацию" });
    },
  };

  function head(page) {
    // Строит заголовок с метками и действиями
    const info = P.docKinds.info(page.kind);
    return P.h("header", { class: "page-head" }, [
      P.h("div", { class: "page-kicker" }, [P.docKinds.icon(page.kind, 14), info.label,
        page.isExport ? P.h("span", { class: "badge export", text: "Экспорт" }) : null,
        page.directive ? P.h("span", { class: "badge", text: page.directive }) : null,
        page.region ? P.h("span", { class: "badge region", text: page.region }) : null]),
      P.h("div", { class: "page-title" }, [P.h("h1", { text: page.title }), P.h("div", { class: "page-actions" }, [
        page.path ? button("external", "Открыть в коде", function () {
          P.post("docsOpenSource", { path: page.path, line: page.line || 1 });
        }) : null,
        button("write", "Изменить", function () {
          P.docEdit.open(page);
        }),
        button("sparkle", "Спросить ПерУН", function () {
          P.docPage.ask(page);
        }),
        page.note ? button("alert", "Описание неверно", function () {
          P.docDispute.open(page, document.querySelector(".page-head"));
        }) : null,
      ])]),
    ]);
  }

  function meta(page) {
    // Строит строку с файлом и версией кода
    return P.h("div", { class: "page-meta" }, [
      page.path ? P.h("a", { class: "meta-file", title: "Открыть в редакторе", onclick: function () {
        P.post("docsOpenSource", { path: page.path, line: page.line || 1 });
      } }, [P.h("span", { html: P.icon("file", 13) }), page.path + (page.line ? ":" + page.line : "")]) : null,
      page.module ? P.h("a", { class: "meta-module", "data-doc": page.module.id }, [
        P.h("span", { html: P.icon("code", 13) }), page.module.title]) : null,
      page.revision ? P.h("span", { title: "Коммит, по которому построен индекс" }, [
        P.h("span", { html: P.icon("commit", 13) }), page.revision.slice(0, 8)]) : null,
    ]);
  }

  function staleNote(page) {
    // Предупреждает об устаревшем описании
    return P.h("div", { class: "page-note warn" }, [P.h("span", { html: P.icon("alert", 16) }),
      P.h("span", { text: "Код изменился после того, как писалось описание. " +
        "Проверьте его или попросите ПерУН обновить." }),
      P.h("button", { class: "btn", type: "button", text: "Обновить с ПерУН", onclick: function () {
        P.docPage.ask(page);
      } })]);
  }

  function button(icon, text, onClick) {
    // Создает кнопку действия страницы
    return P.h("button", { class: "btn", type: "button", onclick: onClick }, [P.h("span", { html: P.icon(icon, 14) }),
      text]);
  }
})();
