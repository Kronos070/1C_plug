(function () {
  "use strict";
  const P = window.Perun;

  function init() {
    // Запускает интерфейс и сообщает Java
    P.header.render();
    P.thread.init();
    P.composer.render();
    bindDelegates();
    bindScroll();
    P.focusInput = P.composer.focus;
    P.post("ready");
    P.composer.focus();
  }

  function bindDelegates() {
    // Обрабатывает ссылки и копирование кода
    document.addEventListener("click", function (event) {
      const link = event.target.closest("a[data-href]");
      if (link) {
        event.preventDefault();
        P.post("link", { url: link.getAttribute("data-href") });
        return;
      }
      const copyCode = event.target.closest("[data-copy-code]");
      if (copyCode) {
        P.copy(copyCode.closest(".code").querySelector("code").textContent, copyCode);
      }
      const insertCode = event.target.closest("[data-insert-code]");
      if (insertCode) {
        P.post("insert", { code: insertCode.closest(".code").querySelector("code").textContent,
          prompt: P.rating.question(insertCode), model: P.state.model ? P.state.model.key : "" });
      }
    });
  }

  function bindScroll() {
    // Показывает кнопку прокрутки вниз
    const chat = document.getElementById("chat");
    const button = document.getElementById("scroll-down");
    button.innerHTML = P.icon("arrowDown", 16);
    button.addEventListener("click", function () {
      chat.scrollTo({ top: chat.scrollHeight, behavior: "smooth" });
    });
    chat.addEventListener("scroll", function () {
      button.classList.toggle("visible", !P.thread.nearBottom());
    });
  }

  function applyTheme(event) {
    // Применяет цвета темы EDT
    const root = document.documentElement;
    root.setAttribute("data-theme", event.dark ? "dark" : "light");
    if (event.background) {
      root.style.setProperty("--edt-bg", event.background);
    } else {
      root.style.removeProperty("--edt-bg");
    }
  }

  P.on("theme", applyTheme);

  P.on("connecting", function () {
    P.thread.hideOffline();
  });

  P.on("connected", function () {
    P.thread.hideOffline();
  });

  P.on("disconnected", function (event) {
    P.stream.setStreaming(false);
    P.thread.showOffline(event.reason);
  });

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
