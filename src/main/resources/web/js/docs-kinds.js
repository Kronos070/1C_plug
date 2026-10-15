(function () {
  "use strict";
  const P = window.Perun;
  const KINDS = {
    module: { label: "Модуль", icon: "code" },
    procedure: { label: "Процедура", icon: "procedure" },
    function: { label: "Функция", icon: "function" },
    variable: { label: "Переменная", icon: "variable" },
    folder: { label: "Папка", icon: "folder" },
  };
  const SECTIONS = [
    { id: "module", label: "Модули", empty: "В проекте нет модулей" },
    { id: "procedure", label: "Процедуры", empty: "В проекте нет процедур" },
    { id: "function", label: "Функции", empty: "В проекте нет функций" },
    { id: "variable", label: "Переменные", empty: "В проекте нет переменных модулей" },
    { id: "outdated", label: "Требуют внимания", empty: "Всё описано и актуально" },
  ];
  const REASONS = { new: "новый", changed: "изменён", stale: "устарело" };

  P.docs = {
    section: "function",
    current: "",
    page: null,
    status: null,
    supported: true,
    cache: {},
    expanded: {},
  };

  P.docKinds = {
    info: function (kind) {
      // Возвращает подпись и иконку вида
      return KINDS[kind] || { label: kind || "Запись", icon: "file" };
    },

    icon: function (kind, size) {
      // Возвращает иконку вида записи
      return P.h("span", { class: "kind-icon kind-" + (kind || "other"),
        html: P.icon(P.docKinds.info(kind).icon, size || 15) });
    },

    sections: function () {
      // Возвращает вкладки разделов сайдбара
      return SECTIONS;
    },

    section: function (id) {
      // Возвращает описание раздела
      return SECTIONS.find(function (item) {
        return item.id === id;
      }) || SECTIONS[0];
    },

    reason: function (value) {
      // Возвращает подпись причины внимания
      return REASONS[value] || value || "";
    },

    marks: function (node) {
      // Создает метки экспорта и описания
      return [
        node.isExport ? P.h("span", { class: "badge export", text: "Экспорт" }) : null,
        node.stale ? P.h("span", { class: "mark stale", title: "Описание устарело", html: P.icon("alert", 13) })
          : node.documented ? P.h("span", { class: "mark ok", title: "Описано" }) : null,
      ];
    },
  };
})();
