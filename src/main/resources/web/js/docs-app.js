(function () {
  "use strict";
  const P = window.Perun;
  const root = document.getElementById("docs");
  const bar = document.getElementById("bar");
  const history = [""];
  let position = 0;
  let pendingOpen = "";
  let indexing = false;
  let backButton;
  let forwardButton;

  P.docsApp = {
    navigate: function (id, replace) {
      // Открывает запись и пишет историю
      if (!replace && id !== history[position]) {
        history.splice(position + 1);
        history.push(id);
        position = history.length - 1;
      }
      show(id);
    },

    section: function (id) {
      // Переключает вкладку раздела
      P.docs.section = id;
      P.docsApp.tabs();
      P.docTree.render();
      P.docSearch.refresh();
    },

    tabs: function () {
      // Перерисовывает вкладки разделов
      const tabs = document.getElementById("side-tabs");
      tabs.innerHTML = "";
      P.docKinds.sections().forEach(function (item) {
        const count = item.id === "outdated" ? P.docOutdated.count() : 0;
        tabs.append(P.h("button", { type: "button", role: "tab", class: "tab" + (item.id === P.docs.section ?
          " active" : ""), onclick: function () {
          P.docsApp.section(item.id);
        } }, [item.label, count ? P.h("span", { class: "tab-count", text: String(count) }) : null]));
      });
    },

    toast: function (level, text) {
      // Показывает короткое уведомление
      const box = document.getElementById("toasts");
      const tone = level === "error" || level === "warning" ? level : "info";
      const item = P.h("div", { class: "toast " + tone }, [P.h("span", { html: P.icon(tone === "info" ? "info"
        : "alert", 15) }), P.h("span", { text: text })]);
      box.append(item);
      setTimeout(function () {
        item.remove();
      }, tone === "error" ? 6000 : 3500);
    },
  };

  function show(id) {
    // Показывает запись или главную
    P.docEdit.active = false;
    P.docs.current = id;
    root.classList.toggle("show-page", Boolean(id));
    P.docTree.mark();
    backButton.disabled = position === 0;
    forwardButton.disabled = position >= history.length - 1;
    document.getElementById("scroll").scrollTop = 0;
    crumbs(null);
    if (!id) {
      P.docOverview.render();
      return;
    }
    P.docPage.loading();
    P.post("docsPage", { id: id });
  }

  function crumbs(page) {
    // Обновляет хлебные крошки над страницей
    const trail = P.h("nav", { class: "crumbs" }, [P.h("a", { "data-nav": "", text: "Документация" })]);
    ((page && page.breadcrumbs) || []).forEach(function (label, index, all) {
      const last = index === all.length - 1;
      const target = page.module && !last ? page.module.id : null;
      trail.append(P.h("span", { class: "crumb-sep", html: P.icon("chevronRight", 12) }),
        P.h(target ? "a" : "span", { "data-doc": target, text: label }));
    });
    bar.querySelector(".crumbs").replaceWith(trail);
  }

  function step(delta) {
    // Переходит назад или вперед по истории
    const next = position + delta;
    if (next >= 0 && next < history.length) {
      position = next;
      show(history[position]);
    }
  }

  function footer() {
    // Показывает версию кода и прогресс
    const status = P.docs.status || {};
    const foot = document.getElementById("side-foot");
    foot.innerHTML = "";
    foot.append(P.h("span", { html: P.icon("commit", 13) }), P.h("span", { text: status.source === "worktree"
      ? "Источник: файлы проекта (git не найден)" : status.revision ? "Коммит " + status.revision.slice(0, 8)
        : "Индекс ещё не построен" }));
    if (status.syncing) {
      const progress = status.progress || {};
      foot.append(P.h("span", { class: "foot-sync" }, [P.h("span", { class: "spinner" }),
        progress.total ? progress.done + "/" + progress.total : ""]));
    }
  }

  function chrome() {
    // Строит шапку сайдбара и панель навигации
    document.getElementById("side-head").append(P.h("a", { class: "side-brand", "data-nav": "" },
      [P.logo.create(22).el, P.h("strong", { text: "Документация" })]));
    backButton = P.iconButton("back", "Назад", function () {
      step(-1);
    });
    forwardButton = P.iconButton("chevronRight", "Вперед", function () {
      step(1);
    });
    bar.append(P.h("button", { class: "btn to-list", type: "button", onclick: function () {
      root.classList.remove("show-page");
    } }, [P.h("span", { html: P.icon("back", 14) }), "К списку"]), P.iconButton("sidebar", "Скрыть дерево",
      function () {
        root.classList.toggle("side-hidden");
      }, "toggle-side"), backButton, forwardButton, P.h("nav", { class: "crumbs" }),
    P.iconButton("refresh", "Синхронизировать с git", function () {
      P.post("docsSync");
      P.docsApp.toast("info", "Проверяю новые коммиты…");
    }));
  }

  P.docsApp.crumbs = crumbs;
  P.docsApp.footer = footer;
  P.docsApp.chrome = chrome;
  P.docsApp.reload = function () {
    // Перезапрашивает дерево и открытую страницу
    P.docOutdated.reset();
    P.docTree.reset();
    P.post("docsStatus");
    if (P.docs.current && !P.docEdit.active) {
      P.post("docsPage", { id: P.docs.current });
    }
  };
  P.docsApp.pending = function (id) {
    // Откладывает открытие до загрузки
    pendingOpen = id;
  };
  P.docsApp.takePending = function () {
    // Забирает отложенную запись
    const id = pendingOpen;
    pendingOpen = "";
    return id;
  };
  P.docsApp.signal = function (text) {
    // Реагирует на смену статуса индексации
    if (text === "docs:updated") {
      P.docsApp.reload();
      return;
    }
    const now = /индексация/i.test(text || "");
    if ((indexing && !now) || text === "") {
      P.docsApp.reload();
    }
    indexing = now;
  };
})();
