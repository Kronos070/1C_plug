(function () {
  "use strict";
  const P = window.Perun;
  const NS = "http://www.w3.org/2000/svg";
  const MAX = 7;
  const ROW = 34;
  const BOX = 26;
  const COLUMNS = { left: 0, center: 270, right: 540, width: 200 };
  let current = null;

  P.docGraph = {
    section: function (page) {
      // Строит блок связей метода
      if (page.kind !== "procedure" && page.kind !== "function") {
        return null;
      }
      const holder = P.h("div", { class: "graph" }, [P.h("div", { class: "tree-loading" }, [
        P.h("span", { class: "spinner" }), "Ищу, кто вызывает метод…"])]);
      current = { id: page.id, page: page, holder: holder };
      P.post("docsCallers", { id: page.id, exported: Boolean(page.isExport) });
      return P.h("section", { class: "page-section" }, [P.h("h2", {}, ["Связи",
        P.h("span", { class: "section-meta", text: "кто вызывает → метод → что вызывает" })]), holder]);
    },

    accept: function (key, data) {
      // Рисует граф после поиска вызовов
      if (!current || current.id !== key) {
        return;
      }
      const callers = (data.items || []).map(function (item) {
        return { label: item.name, hint: item.path + ":" + item.line, id: item.id };
      });
      const calls = (current.page.calls || []).map(function (name) {
        return { label: name, hint: "Найти в документации", search: name };
      });
      current.holder.innerHTML = "";
      current.holder.append(draw(callers, current.page.title, calls));
    },
  };

  function draw(left, title, right) {
    // Рисует схему из трех колонок
    const shownLeft = trim(left);
    const shownRight = trim(right);
    const rows = Math.max(shownLeft.length, shownRight.length, 1);
    const height = rows * ROW + 8;
    const middle = height / 2;
    const svg = node("svg", { viewBox: "0 0 " + (COLUMNS.right + COLUMNS.width) + " " + height, class: "graph-svg" });
    column(svg, shownLeft, COLUMNS.left, height, middle, true, "никто не вызывает");
    column(svg, shownRight, COLUMNS.right, height, middle, false, "ничего не вызывает");
    box(svg, { label: title }, COLUMNS.center, middle - BOX / 2, "center");
    return svg;
  }

  function column(svg, items, x, height, middle, incoming, empty) {
    // Рисует колонку узлов и связи с центром
    if (!items.length) {
      svg.append(node("text", { x: x + COLUMNS.width / 2, y: middle + 4, class: "graph-empty",
        "text-anchor": "middle" }, empty));
      return;
    }
    const top = (height - items.length * ROW) / 2 + (ROW - BOX) / 2;
    items.forEach(function (item, index) {
      const y = top + index * ROW;
      const from = incoming ? x + COLUMNS.width : COLUMNS.center + COLUMNS.width;
      const to = incoming ? COLUMNS.center : x;
      const fromY = incoming ? y + BOX / 2 : middle;
      const toY = incoming ? middle : y + BOX / 2;
      const bend = (to - from) / 2;
      svg.append(node("path", { class: "graph-edge", d: "M" + from + " " + fromY + " C" + (from + bend) + " " +
        fromY + " " + (to - bend) + " " + toY + " " + to + " " + toY }));
      box(svg, item, x, y, item.more ? "more" : incoming ? "caller" : "callee");
    });
  }

  function box(svg, item, x, y, tone) {
    // Рисует узел графа с подписью
    const group = node("g", { class: "graph-node " + tone });
    group.append(node("title", {}, item.hint || item.label));
    group.append(node("rect", { x: x, y: y, width: COLUMNS.width, height: BOX, rx: 7 }));
    group.append(node("text", { x: x + COLUMNS.width / 2, y: y + BOX / 2 + 4, "text-anchor": "middle" },
      shorten(item.label)));
    if (item.id || item.search) {
      group.addEventListener("click", function () {
        if (item.id) {
          P.docsApp.navigate(item.id);
        } else {
          P.docSearch.find(item.search);
        }
      });
    }
    svg.append(group);
  }

  function trim(items) {
    // Оставляет первые узлы и счетчик остальных
    if (items.length <= MAX) {
      return items;
    }
    return items.slice(0, MAX - 1).concat([{ label: "ещё " + (items.length - MAX + 1), more: true }]);
  }

  function shorten(text) {
    // Обрезает длинную подпись узла
    return text.length > 26 ? text.slice(0, 25) + "…" : text;
  }

  function node(tag, attrs, text) {
    // Создает SVG-элемент с атрибутами
    const element = document.createElementNS(NS, tag);
    Object.keys(attrs).forEach(function (key) {
      element.setAttribute(key, attrs[key]);
    });
    if (text) {
      element.textContent = text;
    }
    return element;
  }
})();
