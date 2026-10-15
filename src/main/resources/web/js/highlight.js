(function () {
  "use strict";
  const P = window.Perun;
  const BSL = new Set((
    "процедура функция конецпроцедуры конецфункции если тогда иначеесли иначе конецесли для каждого из по цикл " +
    "конеццикла пока возврат перем знач экспорт попытка исключение конецпопытки вызватьисключение новый и или не " +
    "истина ложь неопределено null продолжить прервать выполнить асинх ждать " +
    "procedure function endprocedure endfunction if then elsif else endif for each in to do enddo while return " +
    "var val export try except endtry raise new and or not true false undefined continue break execute async await " +
    "выбрать различные первые как где соединение левое правое внутреннее полное сгруппировать упорядочить итоги " +
    "объединить все поместить уничтожить из имеющие убыв возр"
  ).split(" "));
  const GENERIC = new Set((
    "function return if else for while const let var class public private protected static new import from " +
    "export default true false null undefined this void int string boolean select where join order group by " +
    "def self none in and or not try catch finally throw async await interface extends implements package"
  ).split(" "));
  const TOKEN = /(\/\/[^\n]*|#[^\n]*|--[^\n]*|"(?:[^"\n\\]|\\.|"")*"?|'[^'\n]*'?|&[A-Za-zА-Яа-яЁё]+|\d+(?:\.\d+)?|[A-Za-zА-Яа-яЁё_][A-Za-zА-Яа-яЁё0-9_]*)/g;

  P.highlight = function (code, lang) {
    // Подсвечивает синтаксис фрагмента кода
    const bsl = /^(bsl|1c|1с|bsl1c|onescript|os)$/i.test(lang || "");
    const keywords = bsl ? BSL : GENERIC;
    let html = "";
    let last = 0;
    code.replace(TOKEN, function (token, _group, offset) {
      html += P.escape(code.slice(last, offset)) + wrap(token, keywords, bsl, lang);
      last = offset + token.length;
      return token;
    });
    return html + P.escape(code.slice(last));
  };

  function wrap(token, keywords, bsl, lang) {
    // Оборачивает токен в класс подсветки
    const first = token.charAt(0);
    const text = P.escape(token);
    if (token.startsWith("//") || (token.startsWith("--") && /sql/i.test(lang || ""))) {
      return '<span class="syn-c">' + text + "</span>";
    }
    if (first === "#") {
      return bsl ? '<span class="syn-m">' + text + "</span>" : /^(py|python|sh|bash|yaml|yml)$/i.test(lang || "")
        ? '<span class="syn-c">' + text + "</span>" : text;
    }
    if (first === "-") {
      return text;
    }
    if (first === '"' || first === "'") {
      return '<span class="syn-s">' + text + "</span>";
    }
    if (first === "&") {
      return '<span class="syn-m">' + text + "</span>";
    }
    if (/\d/.test(first)) {
      return '<span class="syn-n">' + text + "</span>";
    }
    return keywords.has(token.toLowerCase()) ? '<span class="syn-k">' + text + "</span>" : text;
  }
})();
