(function () {
  "use strict";
  const P = window.Perun;
  const article = document.getElementById("page");

  P.docOverview = {
    render: function () {
      // Рисует главную страницу документации
      P.docs.page = null;
      article.innerHTML = "";
      if (!P.docs.supported) {
        article.append(hero("Документация недоступна", "Эта версия ПерУН не умеет строить документацию. " +
          "Обновите ПерУН или включите демо-режим в настройках плагина, чтобы посмотреть экран."));
        return;
      }
      const status = P.docs.status;
      article.append(hero("Документация проекта", "Единая для всей команды. ПерУН строит её по последнему " +
        "коммиту в git, описания пишет агент, когда объясняет код, или вы на странице записи."));
      if (!status) {
        article.append(P.h("div", { class: "skeleton" }, [P.h("i"), P.h("i", { class: "short" })]));
        return;
      }
      article.append(P.h("div", { class: "home-stats" }, [
        tile(status.modules, "модулей"),
        tile(status.symbols, "процедур, функций и переменных"),
        tile(status.documented, "описано"),
        tile(status.outdated, "требуют внимания", status.outdated ? "warn" : "", function () {
          P.docsApp.section("outdated");
        }),
      ]));
      if (status.symbols) {
        const share = Math.round(status.documented * 100 / status.symbols);
        article.append(P.h("div", { class: "coverage" }, [P.h("span", { text: "Покрытие описаниями " + share + "%" }),
          P.h("div", { class: "progress" }, [P.h("i", { style: "width:" + share + "%" })])]));
      }
      if (status.lastError) {
        article.append(P.h("div", { class: "page-note warn" }, [P.h("span", { html: P.icon("alert", 16) }),
          P.h("span", { text: "Ошибка индексации: " + status.lastError })]));
      }
      article.append(P.h("p", { class: "home-hint", text: "Выберите модуль или метод в дереве слева или найдите его " +
        "поиском (Ctrl+K)." }));
    },
  };

  function hero(title, text) {
    // Создает заголовок главной страницы
    return P.h("header", { class: "home-hero" }, [P.logo.create(40).el,
      P.h("div", {}, [P.h("h1", { text: title }), P.h("p", { text: text })])]);
  }

  function tile(value, label, tone, onClick) {
    // Создает плитку со счетчиком
    return P.h(onClick ? "button" : "div", { class: "stat" + (tone ? " " + tone : ""), type: onClick ? "button" : null,
      onclick: onClick || null }, [P.h("strong", { text: String(value || 0) }), P.h("span", { text: label })]);
  }
})();
