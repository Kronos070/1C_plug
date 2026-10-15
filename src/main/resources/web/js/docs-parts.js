(function () {
  "use strict";
  const P = window.Perun;

  P.docParts = {
    description: function (page) {
      // Строит блок описания записи
      const note = page.note;
      if (!note) {
        return section("Описание", P.h("div", { class: "page-empty" }, [P.h("p", { text: "Описания пока нет." }),
          P.h("button", { class: "btn primary", type: "button", text: "Спросить ПерУН", onclick: function () {
            P.docPage.ask(page);
          } })]));
      }
      const body = P.h("div", { class: "md page-body" });
      body.innerHTML = "<p>" + P.escape(note.summary) + "</p>" + (note.details ? P.markdown(note.details) : "");
      const when = Date.parse(note.updatedAt);
      return section("Описание", body, P.h("span", { class: "section-meta",
        text: (note.author === "user" ? "вы" : "агент") + (when ? " · " + P.relativeTime(when) : "") }));
    },

    cases: function (note) {
      // Строит раскрывающиеся частные случаи
      if (!note || !note.cases || !note.cases.length) {
        return null;
      }
      return section("Частные случаи", P.h("div", {}, note.cases.map(function (item, index) {
        const body = P.h("div", { class: "md case-body" });
        body.innerHTML = P.markdown(item.text);
        const details = P.h("details", { class: "case" }, [P.h("summary", { text: item.title }), body]);
        details.open = index === 0;
        return details;
      })), P.h("span", { class: "cases-count", text: String(note.cases.length) }));
    },

    parameters: function (list) {
      // Строит таблицу параметров метода
      if (!list || !list.length) {
        return null;
      }
      return section("Параметры", P.h("div", { class: "md" }, [P.h("table", {}, [
        P.h("thead", {}, [P.h("tr", {}, [P.h("th", { text: "Имя" }), P.h("th", { text: "Знач" }),
          P.h("th", { text: "По умолчанию" })])]),
        P.h("tbody", {}, list.map(function (item) {
          return P.h("tr", {}, [P.h("td", {}, [P.h("code", { text: item.name })]),
            P.h("td", { text: item.byValue ? "да" : "" }),
            P.h("td", {}, item.defaultValue ? [P.h("code", { text: item.defaultValue })] : [])]);
        })),
      ])]));
    },

    comment: function (text) {
      // Показывает комментарий из кода
      return text ? section("Комментарий в коде", P.h("pre", { class: "page-comment", text: text })) : null;
    },

    calls: function (list) {
      // Строит чипы вызываемых методов
      if (!list || !list.length) {
        return null;
      }
      return section("Вызывает", P.h("div", { class: "chips" }, list.map(function (name) {
        return P.h("button", { type: "button", class: "chip", title: "Найти в документации", onclick: function () {
          P.docSearch.find(name);
        } }, [P.docKinds.icon("procedure", 13), name]);
      })));
    },

    members: function (list) {
      // Строит состав модуля
      if (!list || !list.length) {
        return null;
      }
      return section("Состав модуля", P.h("div", { class: "members" }, list.map(function (item) {
        return P.h("a", { class: "member" + (item.stale ? " stale" : ""), "data-doc": item.id }, [
          P.docKinds.icon(item.kind, 14),
          P.h("span", { class: "member-text" }, [P.h("strong", { text: item.title }),
            P.h("span", { text: item.summary || "Описания пока нет" })]),
        ].concat(P.docKinds.marks(item)));
      })), P.h("span", { class: "cases-count", text: String(list.length) }));
    },
  };

  function section(title, body, extra) {
    // Оборачивает блок страницы с заголовком
    return P.h("section", { class: "page-section" }, [P.h("h2", {}, [title, extra || null]), body]);
  }
})();
