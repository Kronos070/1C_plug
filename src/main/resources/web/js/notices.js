(function () {
  "use strict";
  const P = window.Perun;
  const ICONS = { info: "info", warning: "alert", error: "alert" };

  P.toast = function (level, text) {
    // Показывает всплывающее уведомление
    if (!text) {
      return;
    }
    if (text.length > 180 || text.indexOf("\n") !== -1) {
      P.reportCard(level, text);
      return;
    }
    const kind = ICONS[level] ? level : "info";
    const toast = P.h("div", { class: "toast " + kind }, [
      P.h("span", { html: P.icon(ICONS[kind], 15) }),
      P.h("span", { text: text }),
    ]);
    const box = document.getElementById("toasts");
    box.append(toast);
    while (box.children.length > 3) {
      box.firstElementChild.remove();
    }
    setTimeout(function () {
      toast.style.opacity = "0";
      setTimeout(function () {
        toast.remove();
      }, 300);
    }, level === "error" ? 6000 : 3500);
  };

  P.reportCard = function (level, text) {
    // Выводит длинный отчет карточкой в чате
    const lines = text.trim().split("\n");
    const title = lines[0].replace(/^=+\s*|\s*=+$/g, "") || "Сообщение агента";
    const card = P.h("div", { class: "card report " + level }, [
      P.h("div", { class: "card-title" }, [P.h("span", { html: P.icon(ICONS[level] || "info", 16) }),
        P.h("span", { text: title })]),
      P.h("pre", { class: "report-body", text: lines.slice(1).join("\n").trim() || text }),
    ]);
    P.stream.current().append(card);
    P.thread.scroll(true);
  };

  P.errorCard = function (title, details, actions) {
    // Создает карточку ошибки с действиями
    return P.h("div", { class: "error-card", role: "alert" }, [
      P.h("span", { html: P.icon("alert", 16) }),
      P.h("div", { style: "flex:1;min-width:0" }, [
        P.h("strong", { text: title }),
        details ? P.h("pre", { text: details }) : null,
        actions && actions.length ? P.h("div", { class: "card-actions" }, actions) : null,
      ]),
    ]);
  };

  P.on("notice", function (event) {
    P.toast(event.level, event.text);
  });
})();
