(function () {
  "use strict";
  const P = window.Perun;
  const LABELS = { added: "+", modified: "~", deleted: "−" };
  let slot;
  let files = [];
  let open = false;

  P.review = {
    mount: function (target) {
      // Запоминает место плашки над полем ввода
      slot = target;
      render();
    },
  };

  function render() {
    // Перерисовывает плашку с правками агента
    if (!slot) {
      return;
    }
    slot.innerHTML = "";
    if (!files.length) {
      return;
    }
    const totals = { added: 0, modified: 0, deleted: 0 };
    files.forEach(function (file) {
      totals.added += file.added;
      totals.modified += file.modified;
      totals.deleted += file.deleted;
    });
    const busy = Boolean(P.state.streaming);
    const head = P.h("div", { class: "review-head" }, [
      P.h("button", { type: "button", class: "review-toggle" + (open ? " open" : ""), title: "Показать правки",
        onclick: function () {
          open = !open;
          render();
        } }, [P.h("span", { class: "review-chev", html: P.icon("chevronRight", 13) }),
        P.h("span", { class: "review-title", text: "Правки Перуна · " + plural(files.length) }), counts(totals)]),
      P.h("div", { class: "review-actions" }, [
        action("Отменить", "", busy, function () {
          P.post("reviewReject", { path: "" });
        }),
        action("Применить", "primary", busy, function () {
          P.post("reviewAccept", { path: "" });
        }),
      ]),
    ]);
    const box = P.h("div", { class: "review" + (open ? " open" : "") }, [head]);
    if (open) {
      box.append(P.h("div", { class: "review-files" }, files.map(function (file) {
        return fileBlock(file, busy);
      })));
    }
    slot.append(box);
  }

  function fileBlock(file, busy) {
    // Строит строку файла с предпросмотром правок
    const row = P.h("div", { class: "review-file" }, [
      P.h("button", { type: "button", class: "review-name", title: file.path, onclick: function () {
        P.post("reviewOpen", { path: file.path, line: file.line });
      } }, [P.h("span", { html: P.icon("file", 13) }), P.h("strong", { text: file.name }),
        file.folder ? P.h("span", { class: "muted", text: file.folder }) : null]),
      counts(file),
      P.iconButton("x", file.revertible ? "Отменить правки файла" : "Нет исходного текста, откат недоступен",
        function () {
          P.post("reviewReject", { path: file.path });
        }, "review-mini"),
      P.iconButton("check", "Применить правки файла", function () {
        P.post("reviewAccept", { path: file.path });
      }, "review-mini"),
    ]);
    row.querySelectorAll(".review-mini").forEach(function (button, index) {
      button.disabled = busy || (index === 0 && !file.revertible);
    });
    const diff = P.h("div", { class: "review-diff" });
    file.hunks.forEach(function (hunk) {
      diff.append(hunkBlock(file, hunk));
    });
    if (file.more) {
      diff.append(P.h("div", { class: "review-more", text: "и ещё блоков: " + file.more }));
    }
    return P.h("div", {}, [row, diff]);
  }

  function hunkBlock(file, hunk) {
    // Показывает удаленные и новые строки блока
    const block = P.h("div", { class: "review-hunk " + hunk.kind, title: "Открыть строку " + hunk.line,
      onclick: function () {
        P.post("reviewOpen", { path: file.path, line: hunk.line });
      } });
    hunk.old.forEach(function (text) {
      block.append(line("deleted", "−", text));
    });
    if (hunk.oldMore) {
      block.append(line("deleted", " ", "… ещё " + hunk.oldMore));
    }
    hunk.new.forEach(function (text) {
      block.append(line(hunk.kind === "modified" ? "modified" : "added", hunk.kind === "modified" ? "~" : "+", text));
    });
    if (hunk.newMore) {
      block.append(line(hunk.kind, " ", "… ещё " + hunk.newMore));
    }
    return block;
  }

  function line(kind, sign, text) {
    // Создает строку предпросмотра
    return P.h("div", { class: "review-line " + kind }, [P.h("span", { class: "review-sign", text: sign }),
      P.h("code", { text: text || " " })]);
  }

  function counts(data) {
    // Показывает число строк по видам
    return P.h("span", { class: "review-counts" }, ["added", "modified", "deleted"].map(function (kind) {
      return data[kind] ? P.h("span", { class: "review-count " + kind, text: LABELS[kind] + data[kind] }) : null;
    }));
  }

  function action(text, tone, disabled, onClick) {
    // Создает кнопку плашки
    const button = P.h("button", { type: "button", class: "btn review-btn" + (tone ? " " + tone : ""), text: text,
      title: disabled ? "Дождитесь окончания ответа" : "", onclick: onClick });
    button.disabled = disabled;
    return button;
  }

  function plural(count) {
    // Склоняет слово «файл»
    const tail = count % 10;
    const word = count % 100 >= 11 && count % 100 <= 14 ? "файлов" : tail === 1 ? "файл"
      : tail >= 2 && tail <= 4 ? "файла" : "файлов";
    return count + " " + word;
  }

  P.on("review", function (event) {
    files = event.files || [];
    if (!files.length) {
      open = false;
    }
    render();
  });

  P.on("streaming", render);
})();
