(function () {
  "use strict";
  const P = window.Perun;
  let sessions = null;
  let current = null;
  let anchor = null;

  P.history = {
    toggle: function (button) {
      // Открывает или закрывает историю чатов
      if (P.menu.isOpen(button)) {
        P.menu.close();
        return;
      }
      anchor = button;
      render();
      P.post("sessions");
    },
  };

  function render() {
    // Рисует панель истории сессий
    if (!anchor) {
      return;
    }
    P.menu.open(anchor, {
      className: "history",
      title: "История чатов",
      placement: "bottom",
      search: Boolean(sessions && sessions.length > 6),
      searchPlaceholder: "Поиск по истории",
      empty: sessions === null ? '<span class="spinner" style="display:inline-block"></span>'
        : "Здесь появятся прошлые диалоги",
      items: items(),
      onSelect: function (item) {
        if (item.path !== current) {
          P.post("openSession", { path: item.path });
        }
      },
      onClose: function () {
        anchor = null;
      },
    });
  }

  function items() {
    // Группирует сессии по датам
    const result = [];
    let lastGroup = "";
    (sessions || []).forEach(function (session) {
      const group = groupOf(session.modified);
      if (group !== lastGroup) {
        result.push({ group: group });
        lastGroup = group;
      }
      result.push({
        label: session.title,
        hint: P.relativeTime(session.modified) + " · " + session.count + " сообщ.",
        path: session.path,
        icon: "chat",
        active: session.path === current,
      });
    });
    return result;
  }

  function groupOf(millis) {
    // Определяет группу даты сессии
    const day = 86400000;
    const start = new Date();
    start.setHours(0, 0, 0, 0);
    if (millis >= start.getTime()) {
      return "Сегодня";
    }
    if (millis >= start.getTime() - day) {
      return "Вчера";
    }
    if (millis >= start.getTime() - 7 * day) {
      return "На этой неделе";
    }
    return "Ранее";
  }

  P.on("sessions", function (event) {
    sessions = event.items || [];
    current = event.current;
    if (anchor) {
      const button = anchor;
      P.menu.close();
      anchor = button;
      render();
    }
  });
})();
