(function () {
  "use strict";
  const P = window.Perun;
  let box = null;
  let items = [];
  let index = 0;
  let onAccept = null;

  P.palette = {
    attach: function (container) {
      // Встраивает список подсказок над полем ввода
      box = P.h("div", { class: "palette", role: "listbox" });
      container.append(box);
    },

    show: function (list, accept, empty) {
      // Показывает подсказки и обработчик выбора
      if (!box) {
        return;
      }
      items = list;
      index = 0;
      onAccept = accept;
      render(empty || "Ничего не найдено");
    },

    isOpen: function () {
      // Проверяет, показан ли список
      return Boolean(box && box.classList.contains("open"));
    },

    close: function () {
      // Скрывает список подсказок
      if (box) {
        box.classList.remove("open");
      }
      items = [];
    },

    handleKey: function (event) {
      // Обрабатывает навигацию по списку
      if (!P.palette.isOpen() || !items.length) {
        return false;
      }
      if (event.key === "ArrowDown" || event.key === "ArrowUp") {
        index = (index + (event.key === "ArrowDown" ? 1 : -1) + items.length) % items.length;
        render("");
      } else if (event.key === "Tab" || (event.key === "Enter" && !event.shiftKey)) {
        onAccept(items[index], event.key === "Enter");
      } else if (event.key === "Escape") {
        P.palette.close();
      } else {
        return false;
      }
      event.preventDefault();
      return true;
    },
  };

  function render(empty) {
    // Рисует список подсказок
    box.innerHTML = "";
    if (!items.length) {
      box.append(P.h("div", { class: "palette-empty", text: empty }));
    }
    items.forEach(function (item, position) {
      const row = P.h("button", {
        class: "palette-item" + (position === index ? " active" : ""),
        type: "button",
        role: "option",
        onmousedown: function (event) {
          event.preventDefault();
          onAccept(item, false);
        },
      }, [
        P.h("span", { class: "palette-name", text: item.title }),
        item.hint ? P.h("span", { class: "palette-hint", text: item.hint }) : null,
        P.h("span", { class: "palette-desc", text: item.desc || "" }),
      ]);
      box.append(row);
      if (position === index) {
        setTimeout(function () {
          row.scrollIntoView({ block: "nearest" });
        }, 0);
      }
    });
    box.classList.add("open");
  }
})();
