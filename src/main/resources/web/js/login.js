(function () {
  "use strict";
  const P = window.Perun;
  const PROVIDERS = [
    ["anthropic", "Anthropic (Claude)"],
    ["openai", "OpenAI"],
    ["google", "Google Gemini"],
    ["openrouter", "OpenRouter"],
    ["deepseek", "DeepSeek"],
    ["xai", "xAI (Grok)"],
    ["mistral", "Mistral"],
    ["groq", "Groq"],
    ["moonshotai", "Moonshot (Kimi)"],
    ["zai", "Z.ai (GLM)"],
    ["", "Другой провайдер…"],
  ];

  P.login = {
    open: function (preset) {
      // Показывает карточку подключения провайдера
      const known = PROVIDERS.some(function (item) {
        return item[0] && item[0] === preset;
      });
      const select = P.h("select", { class: "field" }, PROVIDERS.map(function (item) {
        return P.h("option", { value: item[0], text: item[1] });
      }));
      const custom = P.h("input", { class: "field", type: "text", placeholder: "Идентификатор, например ollama",
        value: known ? "" : preset || "" });
      const key = P.h("input", { class: "field", type: "password", placeholder: "API-ключ", autocomplete: "off" });
      const card = P.h("div", { class: "card login-card" });
      const syncCustom = function () {
        // Показывает поле своего провайдера
        custom.style.display = select.value ? "none" : "";
      };
      select.value = known ? preset : preset ? "" : "anthropic";
      select.addEventListener("change", syncCustom);
      syncCustom();
      card.append(
        P.h("div", { class: "card-title" }, [P.h("span", { html: P.icon("shield", 16) }),
          P.h("span", { text: "Подключение провайдера" })]),
        P.h("div", { class: "card-text", text: "Ключ сохранится в настройках агента ПерУН, агент перезапустится." }),
        P.h("div", { class: "form-row" }, [select, custom, key]),
        P.h("div", { class: "card-actions" }, [
          P.h("button", { class: "btn primary", type: "button", text: "Сохранить", onclick: function () {
            save(card, select.value || custom.value.trim(), key.value.trim());
          } }),
          P.h("button", { class: "btn", type: "button", text: "Отмена", onclick: function () {
            card.remove();
          } }),
        ])
      );
      key.addEventListener("keydown", function (event) {
        if (event.key === "Enter") {
          save(card, select.value || custom.value.trim(), key.value.trim());
        }
      });
      P.stream.current().append(card);
      P.thread.scroll(true);
      setTimeout(function () {
        (select.value ? key : custom).focus();
      }, 0);
    },
  };

  function save(card, provider, key) {
    // Отправляет ключ провайдера в плагин
    if (!/^[a-z0-9][a-z0-9._\-]*$/.test(provider) || !key) {
      P.toast("warning", "Укажите провайдера латиницей и API-ключ");
      return;
    }
    P.post("login", { provider: provider, key: key });
    card.className = "card resolved";
    card.textContent = "Ключ для «" + provider + "» отправлен, агент перезапускается…";
  }
})();
