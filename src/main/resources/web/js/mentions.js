(function () {
  "use strict";
  const P = window.Perun;
  const TOKEN = /(?:^|\s)@([^\s@]*)$/;
  let timer = 0;
  let pending = null;

  P.mentions = {
    query: function (input) {
      // Возвращает запрос после символа @
      const match = TOKEN.exec(input.value.slice(0, input.selectionStart));
      return match ? match[1] : null;
    },

    request: function (query, onItems) {
      // Запрашивает файлы с задержкой ввода
      clearTimeout(timer);
      pending = { query: query, onItems: onItems };
      timer = setTimeout(function () {
        P.post("files", { query: query });
      }, 120);
    },

    insert: function (input, path) {
      // Заменяет запрос @ выбранным путем
      const caret = input.selectionStart;
      const before = input.value.slice(0, caret).replace(/@([^\s@]*)$/, "@" + path + " ");
      input.value = before + input.value.slice(caret);
      input.setSelectionRange(before.length, before.length);
    },
  };

  P.on("files", function (event) {
    if (!pending || pending.query !== event.query) {
      return;
    }
    pending.onItems((event.items || []).map(function (path) {
      const slash = path.lastIndexOf("/");
      return { title: "@" + path.slice(slash + 1), desc: slash > 0 ? path.slice(0, slash) : "", value: path };
    }));
  });
})();
