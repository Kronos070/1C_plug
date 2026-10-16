(function () {
  "use strict";
  const P = window.Perun;
  let items = null;

  P.docOutdated = {
    render: function (box) {
      // Рисует список записей, требующих внимания
      box.innerHTML = "";
      if (!items) {
        box.append(P.h("div", { class: "tree-loading" }, [P.h("span", { class: "spinner" }), "Загрузка…"]));
        P.post("docsOutdated");
        return;
      }
      if (!items.length) {
        box.append(P.h("div", { class: "tree-empty", text: P.docKinds.section("outdated").empty }));
        return;
      }
      box.append(P.h("button", { class: "btn primary describe", type: "button", onclick: function () {
        P.post("askChat", { text: "/docs update" });
      } }, [P.h("span", { html: P.icon("sparkle", 14) }), "Описать с ПерУН"]));
      items.forEach(function (item) {
        box.append(P.h("div", { class: "tree-row flat", "data-doc-id": item.id, title: item.title, onclick: function () {
          P.docsApp.navigate(item.id);
        } }, [
          P.docKinds.icon(item.kind, 14),
          P.h("span", { class: "tree-label" }, [P.h("span", { text: item.title }),
            P.h("small", { text: item.moduleTitle })]),
          P.h("span", { class: "reason " + item.reason, text: P.docKinds.reason(item.reason) }),
        ]));
      });
      P.docTree.mark();
    },

    accept: function (data) {
      // Запоминает список и перерисовывает
      items = data.items || [];
      P.docsApp.tabs();
      if (P.docs.section === "outdated") {
        P.docTree.render();
      }
    },

    count: function () {
      // Возвращает число записей для вкладки
      return items ? items.length : P.docs.status ? P.docs.status.outdated : 0;
    },

    reset: function () {
      // Сбрасывает загруженный список
      items = null;
    },
  };
})();
