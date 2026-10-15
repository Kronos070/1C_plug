(function () {
  "use strict";
  const P = window.Perun;
  let current = null;

  P.menu = {
    open: function (anchor, options) {
      // Открывает всплывающее меню у элемента
      P.menu.close();
      const menu = P.h("div", { class: "menu" + (options.className ? " " + options.className : ""), role: "menu" });
      if (options.title) {
        menu.append(P.h("div", { class: "menu-title", text: options.title }));
      }
      const list = P.h("div", { class: "menu-list" });
      let input = null;
      if (options.search) {
        input = P.h("input", { type: "text", placeholder: options.searchPlaceholder || "Поиск" });
        menu.append(P.h("label", { class: "menu-search" }, [P.h("span", { html: P.icon("search", 14) }), input]));
        input.addEventListener("input", function () {
          fill(list, options, input.value);
        });
      }
      menu.append(list);
      fill(list, options, "");
      document.body.append(menu);
      position(menu, anchor, options.placement || "top");
      anchor.classList.add("open");
      current = { menu: menu, anchor: anchor, onClose: options.onClose };
      setTimeout(function () {
        document.addEventListener("mousedown", outside, true);
        document.addEventListener("keydown", escape, true);
        if (input) {
          input.focus();
        }
      }, 0);
      return menu;
    },

    close: function () {
      // Закрывает открытое меню
      if (!current) {
        return;
      }
      current.menu.remove();
      current.anchor.classList.remove("open");
      document.removeEventListener("mousedown", outside, true);
      document.removeEventListener("keydown", escape, true);
      const onClose = current.onClose;
      current = null;
      if (onClose) {
        onClose();
      }
    },

    isOpen: function (anchor) {
      // Проверяет, открыто ли меню у элемента
      return Boolean(current && current.anchor === anchor);
    },
  };

  function fill(list, options, query) {
    // Заполняет меню отфильтрованными пунктами
    list.innerHTML = "";
    const needle = query.trim().toLowerCase();
    const items = options.items.filter(function (item) {
      return !needle || (item.label + " " + (item.hint || "")).toLowerCase().includes(needle) || item.group;
    });
    const visible = items.filter(function (item) {
      return !item.group;
    });
    if (!visible.length) {
      list.append(P.h("div", { class: "menu-empty", html: options.empty || "Ничего не найдено" }));
      return;
    }
    items.forEach(function (item) {
      if (item.group) {
        list.append(P.h("div", { class: "menu-group", text: item.group }));
        return;
      }
      list.append(P.h("button", {
        class: "menu-item" + (item.active ? " active" : ""),
        type: "button",
        role: "menuitem",
        title: item.title || item.label,
        onclick: function () {
          P.menu.close();
          options.onSelect(item);
        },
      }, [
        item.icon ? P.h("span", { class: "menu-icon", html: P.icon(item.icon, 15) }) : null,
        P.h("span", { class: "menu-item-text" }, [
          P.h("span", { text: item.label }),
          item.hint ? P.h("small", { text: item.hint }) : null,
        ]),
        P.h("span", { class: "menu-check", html: P.icon("check", 14) }),
      ]));
    });
  }

  function position(menu, anchor, placement) {
    // Размещает меню рядом с элементом
    const rect = anchor.getBoundingClientRect();
    const width = menu.offsetWidth;
    const left = Math.max(8, Math.min(rect.left, window.innerWidth - width - 8));
    menu.style.left = left + "px";
    if (placement === "top") {
      menu.style.bottom = window.innerHeight - rect.top + 6 + "px";
    } else {
      menu.style.top = rect.bottom + 6 + "px";
    }
  }

  function outside(event) {
    // Закрывает меню по клику снаружи
    if (current && !current.menu.contains(event.target) && !current.anchor.contains(event.target)) {
      P.menu.close();
    }
  }

  function escape(event) {
    // Закрывает меню по клавише Escape
    if (event.key === "Escape") {
      event.stopPropagation();
      P.menu.close();
    }
  }
})();
