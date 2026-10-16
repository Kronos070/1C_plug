(function () {
  "use strict";
  const P = window.Perun;
  const DELAY = 250;
  const results = document.getElementById("results");
  const tree = document.getElementById("tree");
  let input;
  let timer = 0;
  let query = "";

  P.docSearch = {
    render: function () {
      // Строит строку поиска
      input = P.h("input", { type: "search", placeholder: "Поиск по имени и описанию", spellcheck: "false",
        "aria-label": "Поиск по документации" });
      input.addEventListener("input", onInput);
      input.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
          event.stopPropagation();
          P.docSearch.clear();
        }
      });
      document.getElementById("side-search").append(P.h("span", { html: P.icon("search", 15) }), input,
        P.h("kbd", { text: "Ctrl K" }));
      document.addEventListener("keydown", function (event) {
        if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
          event.preventDefault();
          input.focus();
          input.select();
        }
      });
    },

    find: function (text) {
      // Подставляет запрос и ищет сразу
      input.value = text;
      onInput();
    },

    clear: function () {
      // Очищает поиск и возвращает дерево
      input.value = "";
      onInput();
    },

    refresh: function () {
      // Повторяет поиск после смены раздела
      if (query.length >= 2) {
        send();
      }
    },

    accept: function (key, data) {
      // Показывает результаты актуального запроса
      if (key !== query) {
        return;
      }
      results.innerHTML = "";
      const hits = data.hits || [];
      results.append(P.h("div", { class: "results-title", text: hits.length ? "Найдено: " + hits.length
        : "Ничего не найдено" }));
      hits.forEach(function (hit) {
        results.append(P.h("button", { type: "button", class: "hit", onclick: function () {
          P.docsApp.navigate(hit.id);
        } }, [
          P.h("span", { class: "hit-head" }, [P.docKinds.icon(hit.kind, 13), P.h("strong", { text: hit.title }),
            hit.documented ? P.h("span", { class: "mark ok", title: "Описано" }) : null]),
          P.h("span", { class: "hit-module", text: hit.moduleTitle }),
          hit.snippet ? P.h("span", { class: "hit-snippet", html: mark(hit.snippet) }) : null,
        ]));
      });
    },
  };

  function onInput() {
    // Переключает дерево и результаты поиска
    query = input.value.trim();
    clearTimeout(timer);
    const active = query.length > 0;
    results.hidden = !active;
    tree.hidden = active;
    if (!active) {
      results.innerHTML = "";
      return;
    }
    if (query.length < 2) {
      results.innerHTML = "";
      results.append(P.h("div", { class: "results-title", text: "Введите хотя бы 2 символа" }));
      return;
    }
    timer = setTimeout(send, DELAY);
  }

  function send() {
    // Отправляет поисковый запрос
    P.post("docsSearch", { query: query, kind: P.docs.section === "outdated" ? "" : P.docs.section });
  }

  function mark(snippet) {
    // Подсвечивает слова в кавычках-елочках
    return P.escape(snippet).replace(/«([^»]+)»/g, "<mark>$1</mark>");
  }
})();
