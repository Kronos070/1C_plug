(function () {
  "use strict";
  const P = window.Perun;
  const KEY = "perun.docs.side";
  const MIN = 220;
  const MAX = 520;

  P.docsResize = {
    bind: function () {
      // Включает изменение ширины дерева
      const root = document.getElementById("docs");
      const handle = document.getElementById("resizer");
      apply(root, read());
      handle.addEventListener("mousedown", function (start) {
        start.preventDefault();
        root.classList.add("resizing");
        const move = function (event) {
          apply(root, event.clientX);
        };
        const stop = function () {
          root.classList.remove("resizing");
          document.removeEventListener("mousemove", move);
          document.removeEventListener("mouseup", stop);
          write(getComputedStyle(root).getPropertyValue("--side").trim());
        };
        document.addEventListener("mousemove", move);
        document.addEventListener("mouseup", stop);
      });
    },
  };

  function apply(root, width) {
    // Применяет ширину дерева в пределах
    if (width) {
      root.style.setProperty("--side", Math.max(MIN, Math.min(MAX, parseInt(width, 10))) + "px");
    }
  }

  function read() {
    // Читает сохраненную ширину дерева
    try {
      return window.localStorage.getItem(KEY);
    } catch (error) {
      return null;
    }
  }

  function write(value) {
    // Запоминает ширину дерева
    try {
      window.localStorage.setItem(KEY, value);
    } catch (error) {
      return;
    }
  }
})();
