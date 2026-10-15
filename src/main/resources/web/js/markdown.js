(function () {
  "use strict";
  const P = window.Perun;

  P.markdown = function (source) {
    // Превращает Markdown в безопасный HTML
    const lines = String(source || "").replace(/\r\n?/g, "\n").split("\n");
    const out = [];
    let i = 0;
    while (i < lines.length) {
      const line = lines[i];
      const fence = line.match(/^\s*(```|~~~)\s*([\w#+.\-а-яА-Я]*)/);
      if (fence) {
        const body = [];
        i++;
        while (i < lines.length && !lines[i].trim().startsWith(fence[1])) {
          body.push(lines[i++]);
        }
        i++;
        out.push(codeBlock(body.join("\n"), fence[2]));
      } else if (/^\s*$/.test(line)) {
        i++;
      } else if (/^#{1,4}\s/.test(line)) {
        const level = line.match(/^#+/)[0].length;
        out.push("<h" + level + ">" + inline(line.replace(/^#+\s*/, "")) + "</h" + level + ">");
        i++;
      } else if (/^\s*([-*_])\s*\1\s*\1[\s\-*_]*$/.test(line)) {
        out.push("<hr>");
        i++;
      } else if (/^\s*>/.test(line)) {
        const quote = [];
        while (i < lines.length && /^\s*>/.test(lines[i])) {
          quote.push(lines[i++].replace(/^\s*>\s?/, ""));
        }
        out.push("<blockquote>" + P.markdown(quote.join("\n")) + "</blockquote>");
      } else if (/^\s*\|.*\|\s*$/.test(line) && /^\s*\|?\s*:?-{2,}/.test(lines[i + 1] || "")) {
        const rows = [];
        while (i < lines.length && /^\s*\|.*\|\s*$/.test(lines[i])) {
          rows.push(lines[i++]);
        }
        out.push(table(rows));
      } else if (/^\s*([-*+]|\d+[.)])\s+/.test(line)) {
        const ordered = /^\s*\d/.test(line);
        const items = [];
        while (i < lines.length && /^\s*([-*+]|\d+[.)])\s+/.test(lines[i])) {
          let item = lines[i++].replace(/^\s*([-*+]|\d+[.)])\s+/, "");
          while (i < lines.length && /^\s{2,}\S/.test(lines[i]) && !/^\s*([-*+]|\d+[.)])\s+/.test(lines[i])) {
            item += " " + lines[i++].trim();
          }
          items.push("<li>" + inline(item) + "</li>");
        }
        out.push((ordered ? "<ol>" : "<ul>") + items.join("") + (ordered ? "</ol>" : "</ul>"));
      } else {
        const para = [];
        while (i < lines.length && lines[i].trim() && !/^\s*(```|~~~|#{1,4}\s|>|([-*+]|\d+[.)])\s+)/.test(lines[i])) {
          para.push(lines[i++]);
        }
        if (!para.length) {
          para.push(lines[i++]);
        }
        out.push("<p>" + inline(para.join("\n")).replace(/\n/g, "<br>") + "</p>");
      }
    }
    return out.join("");
  };

  function codeBlock(code, lang) {
    // Формирует блок кода с заголовком
    const label = lang || "код";
    return '<div class="code"><div class="code-head"><span>' + P.escape(label) + "</span>" +
      '<span class="code-tools"><button class="icon-btn" type="button" data-insert-code ' +
      'title="Вставить в редактор">' + P.icon("insert", 14) + "</button>" +
      '<button class="icon-btn" type="button" data-copy-code title="Копировать код">' + P.icon("copy", 14) +
      "</button></span></div><pre><code>" + P.highlight(code, lang) + "</code></pre></div>";
  }

  function table(rows) {
    // Формирует HTML-таблицу из строк
    const cells = function (row) {
      return row.trim().replace(/^\||\|$/g, "").split("|").map(function (cell) {
        return cell.trim();
      });
    };
    const head = cells(rows[0]).map(function (cell) {
      return "<th>" + inline(cell) + "</th>";
    }).join("");
    const body = rows.slice(2).map(function (row) {
      return "<tr>" + cells(row).map(function (cell) {
        return "<td>" + inline(cell) + "</td>";
      }).join("") + "</tr>";
    }).join("");
    return "<table><thead><tr>" + head + "</tr></thead><tbody>" + body + "</tbody></table>";
  }

  function inline(text) {
    // Обрабатывает строчную разметку Markdown
    const codes = [];
    let html = P.escape(text).replace(/`([^`]+)`/g, function (_m, code) {
      codes.push("<code>" + code + "</code>");
      return "\u0000" + (codes.length - 1) + "\u0000";
    });
    html = html
      .replace(/\[\[([^\]|]+)(?:\|([^\]]+))?\]\]/g, function (_m, id, label) {
        return '<a class="doc-link" data-doc="' + id.trim() + '">' + (label || id).trim() + "</a>";
      })
      .replace(/\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)/g, '<a data-href="$2">$1</a>')
      .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
      .replace(/__([^_]+)__/g, "<strong>$1</strong>")
      .replace(/(^|[^*\w])\*([^*\n]+)\*(?!\*)/g, "$1<em>$2</em>")
      .replace(/~~([^~]+)~~/g, "<del>$1</del>");
    return html.replace(/\u0000(\d+)\u0000/g, function (_m, index) {
      return codes[Number(index)];
    });
  }
})();
