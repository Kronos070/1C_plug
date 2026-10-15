(function () {
  "use strict";
  const P = window.Perun;

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
})();
