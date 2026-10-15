(function () {
  "use strict";
  const P = window.Perun;
  const TOOLS = { "1c_docs_read": "read", "1c_docs_write": "write" };
  const pending = {};
  let counter = 0;

  P.sources = {
    fromTool: function (name, args) {
      // Распознает обращение к документации
      const action = TOOLS[name];
      const id = String(args || "").trim();
      if (!action || !id || id.endsWith("…") || id.charAt(0) === "{") {
        return null;
      }
      return { action: action, id: id };
    },

    render: function (parts) {
      // Строит плашки источников под ответом
      const seen = {};
      parts.forEach(function (part) {
        if (part.doc && (!seen[part.doc.id] || part.doc.action === "write")) {
          seen[part.doc.id] = part.doc.action;
        }
      });
      const ids = Object.keys(seen);
      if (!ids.length) {
        return null;
      }
      const key = "s" + (++counter);
      const chips = {};
      const box = P.h("div", { class: "sources" }, [P.h("span", { class: "sources-title", html: P.icon("docs", 13) +
        "<span>Источники</span>" })]);
      ids.forEach(function (id) {
        chips[id] = chip(id, seen[id], null);
        box.append(chips[id]);
      });
      pending[key] = chips;
      P.post("docsPeek", { ids: ids, key: key });
      return box;
    },
  };

  function chip(id, action, info) {
    // Создает плашку записи документации
    const name = info && info.title ? info.title : label(id);
    const parts = [action === "write" ? "описание обновлено" : null];
    if (info && info.updatedAt && action !== "write") {
      parts.push((info.author === "user" ? "описал разработчик " : "описал агент ") +
        P.relativeTime(Date.parse(info.updatedAt)));
    }
    if (info && info.stale) {
      parts.push("устарело");
    }
    const meta = parts.filter(Boolean).join(" · ");
    return P.h("a", { class: "source" + (action === "write" ? " written" : "") + (info && info.stale ? " stale" : ""),
      "data-doc": id, title: id }, [
      P.h("span", { html: P.icon(action === "write" ? "write" : "docs", 12) }),
      P.h("strong", { text: name }),
      meta ? P.h("span", { class: "source-meta", text: meta }) : null,
    ]);
  }

  function label(id) {
    // Достает имя из идентификатора записи
    const hash = id.indexOf("#");
    if (hash >= 0) {
      return id.slice(hash + 1);
    }
    const parts = id.split("/");
    return parts.length > 1 ? parts[parts.length - 2] : id;
  }

  P.on("docsPeek", function (event) {
    const chips = pending[event.key];
    delete pending[event.key];
    if (!chips) {
      return;
    }
    (event.items || []).forEach(function (info) {
      const old = chips[info.id];
      if (!old || info.missing) {
        return;
      }
      old.replaceWith(chip(info.id, old.classList.contains("written") ? "write" : "read", info));
    });
  });
})();
