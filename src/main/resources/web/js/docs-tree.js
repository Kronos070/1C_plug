(function () {
  "use strict";
  const P = window.Perun;
  const box = document.getElementById("tree");
  const pending = new Set();

  P.docTree = {
    render: function () {
      // Рисует дерево текущего раздела
      if (P.docs.section === "outdated") {
        P.docOutdated.render(box);
        return;
      }
      box.innerHTML = "";
      const root = items(P.docs.section, "");
      if (!root) {
        box.append(indexing() || loader());
        request(P.docs.section, "");
        return;
      }
      if (!root.length) {
        box.append(P.h("div", { class: "tree-empty", text: P.docKinds.section(P.docs.section).empty }));
        return;
      }
      root.forEach(function (node) {
        renderNode(box, node, 0);
      });
      P.docTree.mark();
    },

    accept: function (key, data) {
      // Кладет ответ дерева в кэш
      pending.delete(key);
      P.docs.cache[key] = data.items || [];
      if (key.split("|")[0] === P.docs.section) {
        P.docTree.render();
      }
    },

    failed: function (key) {
      // Снимает ожидание после ошибки
      pending.delete(key);
    },

    reset: function () {
      // Сбрасывает кэш и перезапрашивает раскрытое
      P.docs.cache = {};
      pending.clear();
      P.docTree.render();
    },

    mark: function () {
      // Подсвечивает открытую запись в дереве
      box.querySelectorAll(".tree-row.active").forEach(function (row) {
        row.classList.remove("active");
      });
      if (!P.docs.current) {
        return;
      }
      box.querySelectorAll(".tree-row").forEach(function (row) {
        if (row.getAttribute("data-doc-id") === P.docs.current) {
          row.classList.add("active");
        }
      });
    },
  };

  function items(section, node) {
    // Возвращает закэшированных потомков узла
    return P.docs.cache[section + "|" + node];
  }

  function expanded() {
    // Возвращает раскрытые узлы раздела
    P.docs.expanded[P.docs.section] = P.docs.expanded[P.docs.section] || new Set();
    return P.docs.expanded[P.docs.section];
  }

  function request(section, node) {
    // Запрашивает потомков узла один раз
    const key = section + "|" + node;
    if (!pending.has(key)) {
      pending.add(key);
      P.post("docsTree", { section: section, node: node });
    }
  }

  function renderNode(parent, node, depth) {
    // Рисует узел и раскрытых потомков
    const open = node.hasChildren && expanded().has(node.id);
    const kind = node.type === "symbol" ? node.kind : node.type === "module" ? "module" : "folder";
    parent.append(P.h("div", { class: "tree-row" + (node.stale ? " stale" : ""), role: "treeitem",
      "data-id": node.id, "data-doc-id": node.docId || "", style: "--depth:" + depth, title: node.label }, [
      P.h("span", { class: "tree-chevron" + (node.hasChildren ? "" : " none") + (open ? " open" : ""),
        html: node.hasChildren ? P.icon("chevronRight", 12) : "" }),
      P.docKinds.icon(kind, 14),
      P.h("span", { class: "tree-label", text: node.label }),
    ].concat(P.docKinds.marks(node), node.type !== "symbol" && node.count ?
      P.h("span", { class: "tree-count", text: String(node.count) }) : null)));
    if (!open) {
      return;
    }
    const children = items(P.docs.section, node.id);
    const group = P.h("div", { class: "tree-children", role: "group" });
    if (!children) {
      group.append(loader(depth + 1));
      request(P.docs.section, node.id);
    } else {
      children.forEach(function (child) {
        renderNode(group, child, depth + 1);
      });
    }
    parent.append(group);
  }

  function loader(depth) {
    // Создает строку загрузки
    return P.h("div", { class: "tree-loading", style: "--depth:" + (depth || 0) }, [P.h("span", { class: "spinner" }),
      "Загрузка…"]);
  }

  function indexing() {
    // Показывает первую индексацию проекта
    const status = P.docs.status;
    if (!status || status.revision || !status.syncing) {
      return null;
    }
    const progress = status.progress || {};
    const share = progress.total ? Math.round(progress.done * 100 / progress.total) : 0;
    return P.h("div", { class: "tree-indexing" }, [P.h("p", { text: "Строим документацию по коду проекта…" }),
      P.h("div", { class: "progress" }, [P.h("i", { style: "width:" + share + "%" })]),
      progress.total ? P.h("span", { class: "muted", text: progress.done + " из " + progress.total }) : null]);
  }

  box.addEventListener("click", function (event) {
    const row = event.target.closest(".tree-row");
    if (!row || !box.contains(row) || P.docs.section === "outdated") {
      return;
    }
    const id = row.getAttribute("data-id");
    const docId = row.getAttribute("data-doc-id");
    const onChevron = event.target.closest(".tree-chevron");
    if (!row.querySelector(".tree-chevron.none")) {
      const set = expanded();
      if (set.has(id) && (onChevron || !docId)) {
        set.delete(id);
      } else {
        set.add(id);
      }
      P.docTree.render();
    }
    if (docId && !onChevron) {
      P.docsApp.navigate(docId);
    }
  });
})();
