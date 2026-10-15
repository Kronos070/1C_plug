(function () {
  "use strict";
  const P = window.Perun;
  const paths = {
    send: '<path d="M12 19V5"/><path d="m5.5 11.5 6.5-6.5 6.5 6.5"/>',
    stop: '<rect x="7" y="7" width="10" height="10" rx="2.2" fill="currentColor" stroke="none"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    history: '<path d="M3.5 12a8.5 8.5 0 1 0 2.6-6.1L3.5 8.5"/><path d="M3.5 3.5v5h5"/><path d="M12 7.5V12l3 2"/>',
    settings: '<path d="M4 7h9M17 7h3M4 17h3M11 17h9"/><circle cx="15" cy="7" r="2.2"/><circle cx="9" cy="17" r="2.2"/>',
    copy: '<rect x="8.5" y="8.5" width="11.5" height="11.5" rx="2.6"/><path d="M15.5 8.5V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v7.5a2 2 0 0 0 2 2h2.5"/>',
    check: '<path d="m5 12.5 4.5 4.5L19 7.5"/>',
    chevronDown: '<path d="m7 10 5 5 5-5"/>',
    chevronRight: '<path d="m10 7 5 5-5 5"/>',
    tool: '<path d="M14.5 6.5a4 4 0 0 0-5.3 5.3L4 17v3h3l5.2-5.2a4 4 0 0 0 5.3-5.3l-2.6 2.6-2.3-.6-.6-2.3z"/>',
    sparkle: '<path d="M12 3.5l1.8 4.9 4.9 1.8-4.9 1.8L12 16.9l-1.8-4.9-4.9-1.8 4.9-1.8z"/><path d="M18.5 15.5l.7 1.8 1.8.7-1.8.7-.7 1.8-.7-1.8-1.8-.7 1.8-.7z"/>',
    bolt: '<path d="M13 2.5 4.5 13.5h6.5l-1 8 8.5-11h-6.5z" fill="currentColor" stroke="none"/>',
    x: '<path d="M17.5 6.5l-11 11M6.5 6.5l11 11"/>',
    alert: '<circle cx="12" cy="12" r="8.5"/><path d="M12 8v4.5M12 15.8v.2"/>',
    info: '<circle cx="12" cy="12" r="8.5"/><path d="M12 11v5M12 8v.2"/>',
    search: '<circle cx="11" cy="11" r="6.5"/><path d="m20 20-4.2-4.2"/>',
    arrowDown: '<path d="M12 5v14M18 13l-6 6-6-6"/>',
    refresh: '<path d="M20 12a8 8 0 1 1-2.4-5.7L20 8.5"/><path d="M20 3.5v5h-5"/>',
    find: '<circle cx="11" cy="11" r="6.5"/><path d="m20 20-4.2-4.2"/>',
    explain: '<path d="M5 4.5h10.5a3 3 0 0 1 3 3V20H8a3 3 0 0 1-3-3z"/><path d="M5 17a3 3 0 0 1 3-3h10.5"/>',
    write: '<path d="M4.5 19.5h4l10-10-4-4-10 10z"/><path d="m13 7 4 4"/>',
    plan: '<path d="M9.5 6.5h10M9.5 12h10M9.5 17.5h10"/><circle cx="5" cy="6.5" r="1" fill="currentColor"/><circle cx="5" cy="12" r="1" fill="currentColor"/><circle cx="5" cy="17.5" r="1" fill="currentColor"/>',
    model: '<rect x="5.5" y="5.5" width="13" height="13" rx="3"/><path d="M9.5 2.5v3M14.5 2.5v3M9.5 18.5v3M14.5 18.5v3M2.5 9.5h3M2.5 14.5h3M18.5 9.5h3M18.5 14.5h3"/>',
    brain: '<path d="M12 5.5a3 3 0 0 0-5.6 1.4A3 3 0 0 0 5 12a3 3 0 0 0 2 4.9A3 3 0 0 0 12 18.5z"/><path d="M12 5.5a3 3 0 0 1 5.6 1.4A3 3 0 0 1 19 12a3 3 0 0 1-2 4.9 3 3 0 0 1-5 1.6"/>',
    shield: '<path d="M12 3.5 5 6.5v5.2c0 4.3 2.9 7.3 7 8.8 4.1-1.5 7-4.5 7-8.8V6.5z"/><path d="m9 12 2 2 4-4"/>',
    chat: '<path d="M5 18.5V7a2.5 2.5 0 0 1 2.5-2.5h9A2.5 2.5 0 0 1 19 7v6.5a2.5 2.5 0 0 1-2.5 2.5H9z"/>',
  };

  P.icon = function (name, size) {
    // Возвращает SVG-разметку иконки
    const s = size || 16;
    return '<svg width="' + s + '" height="' + s + '" viewBox="0 0 24 24" fill="none" stroke="currentColor" ' +
      'stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
      (paths[name] || "") + "</svg>";
  };

  P.iconButton = function (name, title, onClick, extraClass) {
    // Создает кнопку с иконкой
    return P.h("button", {
      class: "icon-btn" + (extraClass ? " " + extraClass : ""),
      type: "button",
      title: title,
      "aria-label": title,
      html: P.icon(name, 16),
      onclick: onClick,
    });
  };
})();
