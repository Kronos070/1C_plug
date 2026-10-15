(function () {
  "use strict";
  const P = window.Perun;
  const paths = {
    send: '<path d="M12 19V5"/><path d="m5.5 11.5 6.5-6.5 6.5 6.5"/>',
    stop: '<rect x="7" y="7" width="10" height="10" rx="2.2" fill="currentColor" stroke="none"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    history: '<path d="M3.5 12a8.5 8.5 0 1 0 2.6-6.1L3.5 8.5"/><path d="M3.5 3.5v5h5"/><path d="M12 7.5V12l3 2"/>',
    settings: '<path d="M4 7h9M17 7h3M4 17h3M11 17h9"/><circle cx="15" cy="7" r="2.2"/><circle cx="9" cy="17" r="2.2"/>',
    insert: '<path d="M12 4v10"/><path d="m8 10 4 4 4-4"/><path d="M5 19h14"/>',
    thumbUp: '<path d="M7.5 10.5v9H5a1 1 0 0 1-1-1v-7a1 1 0 0 1 1-1z"/><path d="M7.5 10.5 11 4a2 2 0 0 1 2.5 2.4L12.8 9.5h5.2a2 2 0 0 1 2 2.3l-1.1 6.5a2 2 0 0 1-2 1.7H7.5"/>',
    thumbDown: '<path d="M7.5 13.5v-9H5a1 1 0 0 0-1 1v7a1 1 0 0 0 1 1z"/><path d="M7.5 13.5 11 20a2 2 0 0 0 2.5-2.4l-.7-3.1h5.2a2 2 0 0 0 2-2.3l-1.1-6.5a2 2 0 0 0-2-1.7H7.5"/>',
    copy: '<rect x="8.5" y="8.5" width="11.5" height="11.5" rx="2.6"/><path d="M15.5 8.5V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v7.5a2 2 0 0 0 2 2h2.5"/>',
    check: '<path d="m5 12.5 4.5 4.5L19 7.5"/>',
    chevronDown: '<path d="m7 10 5 5 5-5"/>',
    chevronRight: '<path d="m10 7 5 5-5 5"/>',
    tool: '<path d="M14.5 6.5a4 4 0 0 0-5.3 5.3L4 17v3h3l5.2-5.2a4 4 0 0 0 5.3-5.3l-2.6 2.6-2.3-.6-.6-2.3z"/>',
    sparkle: '<path d="M12 3.5l1.8 4.9 4.9 1.8-4.9 1.8L12 16.9l-1.8-4.9-4.9-1.8 4.9-1.8z"/><path d="M18.5 15.5l.7 1.8 1.8.7-1.8.7-.7 1.8-.7-1.8-1.8-.7 1.8-.7z"/>',
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
    code: '<path d="m8.5 7.5-4.5 4.5 4.5 4.5M15.5 7.5l4.5 4.5-4.5 4.5"/>',
    file: '<path d="M13.5 3.5H7a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V9z"/><path d="M13.5 3.5V9H19"/>',
    chat: '<path d="M5 18.5V7a2.5 2.5 0 0 1 2.5-2.5h9A2.5 2.5 0 0 1 19 7v6.5a2.5 2.5 0 0 1-2.5 2.5H9z"/>',
    docs: '<path d="M12 6.5C10.5 5 8 4.5 4 4.5v13c4 0 6.5.5 8 2 1.5-1.5 4-2 8-2v-13c-4 0-6.5.5-8 2z"/><path d="M12 6.5v13"/>',
    folder: '<path d="M3.5 7.5a2 2 0 0 1 2-2h4l2 2h7a2 2 0 0 1 2 2v7.5a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2z"/>',
    catalog: '<path d="M9 6.5h11M9 12h11M9 17.5h11"/><path d="M4.5 6.5h.01M4.5 12h.01M4.5 17.5h.01"/>',
    register: '<rect x="4" y="4.5" width="16" height="15" rx="2"/><path d="M4 9.5h16M4 14.5h16M10 9.5v10"/>',
    table: '<rect x="4" y="5" width="16" height="14" rx="2"/><path d="M4 10h16M4 14.5h16"/>',
    form: '<rect x="3.5" y="4.5" width="17" height="15" rx="2"/><path d="M3.5 9h17M7 13h6M7 16h4"/>',
    procedure: '<path d="M9 4.5c-2 0-2.5 1-2.5 2.5v2.5c0 1.2-.8 2.5-2 2.5 1.2 0 2 1.3 2 2.5V17c0 1.5.5 2.5 2.5 2.5M15 4.5c2 0 2.5 1 2.5 2.5v2.5c0 1.2.8 2.5 2 2.5-1.2 0-2 1.3-2 2.5V17c0 1.5-.5 2.5-2.5 2.5"/>',
    function: '<path d="M16 4.5c-2.5-.5-4 .5-4.5 3l-2 10c-.5 2.5-2 3.5-4.5 3M7.5 10.5h8"/>',
    variable: '<rect x="4" y="4" width="16" height="16" rx="3.5"/><path d="m9 9 6 6M15 9l-6 6"/>',
    attribute: '<path d="M4 4.5h7.5l8.5 8.5-7 7-8.5-8.5z"/><circle cx="8.5" cy="9" r="1.2"/>',
    back: '<path d="M14.5 6 8.5 12l6 6"/>',
    external: '<path d="M14 4.5h5.5V10M19.5 4.5 11 13"/><path d="M18 14v4a1.5 1.5 0 0 1-1.5 1.5h-10A1.5 1.5 0 0 1 5 18V8a1.5 1.5 0 0 1 1.5-1.5H10"/>',
    sidebar: '<rect x="3.5" y="4.5" width="17" height="15" rx="2.5"/><path d="M9.5 4.5v15"/>',
    commit: '<circle cx="12" cy="12" r="3"/><path d="M12 3.5V9M12 15v5.5"/>',
    clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
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
