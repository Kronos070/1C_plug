(function () {
  "use strict";
  const P = window.Perun;
  const TOOL_LABELS = {
    read: "Чтение файла",
    write: "Запись файла",
    edit: "Правка файла",
    bash: "Терминал",
    grep: "Поиск по коду",
    find: "Поиск файлов",
    ls: "Список файлов",
    "1c_ast_symbols": "Структура модуля",
    "1c_call_hierarchy": "Иерархия вызовов",
    "1c_impact_analysis": "Анализ влияния",
    "1c_vanessa": "Vanessa Automation",
    "1c_subagent": "Субагент 1С",
    bsl_fix_standards: "Стандарты ИТС",
  };

  P.parts = {
    text: function (initial) {
      // Создает текстовую часть с Markdown
      const node = P.h("div", { class: "md" });
      let raw = "";
      let frame = 0;
      const part = {
        node: node,
        kind: "text",
        raw: function () {
          // Возвращает исходный Markdown части
          return raw;
        },
        set: function (value) {
          // Заменяет текст и перерисовывает
          raw = value || "";
          if (!frame) {
            frame = requestAnimationFrame(function () {
              frame = 0;
              const stick = P.thread.nearBottom();
              node.innerHTML = P.markdown(raw);
              P.thread.scroll(stick);
            });
          }
        },
        append: function (delta) {
          // Дописывает потоковую дельту текста
          part.set(raw + delta);
        },
      };
      part.set(initial);
      return part;
    },

    thinking: function (initial) {
      // Создает сворачиваемый блок рассуждений
      const body = P.h("div", { class: "block-body" });
      const node = P.h("div", { class: "block thinking" });
      const head = P.h("button", { class: "block-head", type: "button", onclick: function () {
        node.classList.toggle("open");
      } }, [
        P.h("span", { html: P.icon("sparkle", 14) }),
        P.h("span", { class: "name", text: "Размышления" }),
        P.h("span", { class: "args" }),
        P.h("span", { class: "chev", html: P.icon("chevronRight", 14) }),
      ]);
      node.append(head, body);
      const part = {
        node: node,
        kind: "thinking",
        set: function (value) {
          // Заменяет текст рассуждений
          body.textContent = value || "";
          head.querySelector(".args").textContent = (value || "").replace(/\s+/g, " ").slice(0, 120);
        },
        append: function (delta) {
          // Дописывает дельту рассуждений
          part.set(body.textContent + delta);
        },
      };
      part.set(initial);
      return part;
    },

    tool: function (data) {
      // Создает блок вызова инструмента
      const output = P.h("pre");
      const state = P.h("span", { class: "tool-state" });
      const node = P.h("div", { class: "block tool" });
      node.append(
        P.h("button", { class: "block-head", type: "button", onclick: function () {
          node.classList.toggle("open");
        } }, [
          P.h("span", { html: P.icon("tool", 14) }),
          P.h("span", { class: "name", text: TOOL_LABELS[data.name] || data.name }),
          P.h("span", { class: "args", text: data.args || "", title: data.args || "" }),
          state,
          P.h("span", { class: "chev", html: P.icon("chevronRight", 14) }),
        ]),
        P.h("div", { class: "block-body" }, [output])
      );
      const part = {
        node: node,
        kind: "tool",
        finish: function (text, failed) {
          // Показывает результат инструмента
          output.textContent = text || (failed ? "Ошибка без описания" : "Готово");
          state.className = "tool-state " + (failed ? "fail" : "ok");
          state.innerHTML = P.icon(failed ? "x" : "check", 14);
        },
        running: function () {
          // Показывает индикатор выполнения
          state.className = "tool-state";
          state.innerHTML = '<span class="spinner"></span>';
        },
      };
      if (data.running) {
        part.running();
      } else {
        part.finish(data.output, data.failed);
      }
      return part;
    },
  };
})();
