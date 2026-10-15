(function () {
  "use strict";
  const P = window.Perun;
  let started = false;

  const handlers = {
    status: function (event) {
      // Обновляет состояние индекса
      P.docs.status = event.data.status || null;
      P.docsApp.footer();
      P.docsApp.tabs();
      if (!P.docs.current) {
        P.docOverview.render();
      }
      if (event.data.report) {
        P.docsApp.toast("info", event.data.report.upToDate ? "Документация актуальна" : "Документация обновлена");
        P.docsApp.reload();
      }
      if (!started) {
        started = true;
        P.docTree.render();
      }
    },
    tree: function (event) {
      // Передает узлы дереву
      P.docTree.accept(event.key, event.data);
    },
    page: function (event) {
      // Рисует пришедшую страницу
      if (event.key === P.docs.current && !P.docEdit.active) {
        P.docPage.render(event.data.page);
        P.docsApp.crumbs(event.data.page);
      }
    },
    callers: function (event) {
      // Передает вызывающие методы графу
      P.docGraph.accept(event.key, event.data);
    },
    search: function (event) {
      // Показывает результаты поиска
      P.docSearch.accept(event.key, event.data);
    },
    outdated: function (event) {
      // Обновляет список внимания
      P.docOutdated.accept(event.data);
    },
    saved: function (event) {
      // Применяет результат сохранения
      P.docEdit.active = false;
      if (!event.data.page) {
        P.docsApp.toast("info", "Описание сохранено, появится после коммита");
        P.docEdit.close();
      } else {
        P.docsApp.toast("info", event.data.page.note ? "Описание сохранено" : "Описание удалено");
        P.docPage.render(event.data.page);
        P.docsApp.crumbs(event.data.page);
      }
      P.docOutdated.reset();
      P.docTree.reset();
      P.post("docsStatus");
    },
  };

  P.on("docs", function (event) {
    (handlers[event.kind] || function () {})(event);
  });

  P.on("docsError", function (event) {
    if (event.kind === "tree") {
      P.docTree.failed(event.key);
    }
    if (event.kind === "saved") {
      P.docEdit.failed();
    }
    if (event.kind === "page" && event.key === P.docs.current) {
      if (/не найден/i.test(event.text)) {
        P.docsApp.toast("warning", "Этой записи нет в документации: метод удалён или ещё не закоммичен");
        P.docTree.reset();
        P.docsApp.navigate("", false);
        return;
      }
      P.docPage.error(event.text);
      return;
    }
    P.docsApp.toast("error", event.text);
  });

  P.on("docsAvailability", function (event) {
    P.docs.supported = event.supported;
    document.getElementById("docs").classList.toggle("unsupported", !event.supported);
    P.docOverview.render();
    if (!event.supported) {
      document.getElementById("tree").innerHTML = "";
    }
  });

  P.on("docsSignal", function (event) {
    P.docsApp.signal(event.text);
  });

  P.on("docsOpen", function (event) {
    if (started) {
      P.docsApp.navigate(event.id);
    } else {
      P.docsApp.pending(event.id);
    }
  });

  P.on("notice", function (event) {
    P.docsApp.toast(event.level, event.text);
  });

  function init() {
    // Запускает страницу документации
    P.docsApp.chrome();
    P.docsApp.tabs();
    P.docsApp.footer();
    P.docSearch.render();
    P.docsResize.bind();
    P.docsApp.navigate("", true);
    document.addEventListener("click", function (event) {
      const target = event.target.closest("[data-doc], [data-nav], a[data-href], [data-copy-code], [data-insert-code]");
      if (!target || (target.hasAttribute("data-doc") && !target.getAttribute("data-doc"))) {
        return;
      }
      event.preventDefault();
      if (target.hasAttribute("data-doc") || target.hasAttribute("data-nav")) {
        P.docsApp.navigate(target.getAttribute("data-doc") || target.getAttribute("data-nav") || "");
      } else if (target.hasAttribute("data-href")) {
        P.post("link", { url: target.getAttribute("data-href") });
      } else {
        const code = target.closest(".code").querySelector("code").textContent;
        P.post(target.hasAttribute("data-copy-code") ? "copy" : "insert", { code: code, text: code });
      }
    });
    P.post("ready");
  }

  P.on("docs", function () {
    const id = started ? P.docsApp.takePending() : "";
    if (id) {
      P.docsApp.navigate(id);
    }
  });

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
