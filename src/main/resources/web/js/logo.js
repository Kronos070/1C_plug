(function () {
  "use strict";
  const P = window.Perun;
  const TURN = Math.PI * 2;
  const LINGER = 0.6;
  const RAMP = 0.8;
  const SPEEDS = { work: TURN / 2.4, idle: TURN / 7 };
  const live = new Set();
  const reduced = Boolean(window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches);
  let frame = 0;
  let last = 0;
  let background = null;

  P.logo = {
    create: function (size, options) {
      // Создает объемный логотип ПерУНа
      const pad = Math.ceil(size * 0.3);
      const canvas = P.h("canvas", { class: "plogo-canvas" });
      const el = P.h("span", { class: "plogo" + (options && options.egg ? " clickable" : ""), role: "img",
        "aria-label": "ПерУН" }, [canvas]);
      el.style.width = size + "px";
      el.style.height = size + "px";
      canvas.style.left = canvas.style.top = -pad + "px";
      canvas.style.width = canvas.style.height = size + pad * 2 + "px";
      const logo = { el: el, canvas: canvas, size: size, pad: pad, phase: 0, speed: 0, mode: "", stop: null };
      logo.spin = function (active) {
        setMode(logo, active ? "work" : "");
      };
      logo.idle = function (active) {
        setMode(logo, active ? "idle" : "");
      };
      if (options && options.egg) {
        el.title = "ПерУН";
        el.addEventListener("click", function () {
          P.logoEgg.play(logo);
        });
      }
      render(logo);
      return logo;
    },

    background: function () {
      // Возвращает цвет фона чата в RGB
      if (!background) {
        const match = /(\d+)\D+(\d+)\D+(\d+)/.exec(getComputedStyle(document.body).backgroundColor);
        background = match ? [Number(match[1]), Number(match[2]), Number(match[3])] : [30, 30, 30];
      }
      return background;
    },

    reduced: function () {
      // Сообщает, что анимации отключены в системе
      return reduced;
    },
  };

  function setMode(logo, mode) {
    // Включает или останавливает вращение
    if (reduced || logo.mode === mode) {
      return;
    }
    if (logo.stop && mode) {
      const t = P.logoMath.clamp01(logo.stop.time / logo.stop.duration);
      logo.speed = 2 * (logo.stop.to - logo.stop.from) / logo.stop.duration * (1 - t);
      logo.stop = null;
    }
    logo.mode = mode;
    live.add(logo);
    if (!frame) {
      last = performance.now();
      frame = requestAnimationFrame(tick);
    }
  }

  function tick(now) {
    // Продвигает анимацию всех логотипов
    const dt = Math.min(0.05, (now - last) / 1000);
    last = now;
    live.forEach(function (logo) {
      if (!logo.el.isConnected) {
        live.delete(logo);
        return;
      }
      if (!advance(logo, dt)) {
        live.delete(logo);
      }
      render(logo);
    });
    frame = live.size ? requestAnimationFrame(tick) : 0;
  }

  function advance(logo, dt) {
    // Поворачивает логотип на один кадр
    if (logo.mode) {
      const target = SPEEDS[logo.mode];
      const step = target * dt / RAMP;
      logo.speed = logo.speed < target ? Math.min(target, logo.speed + step) : Math.max(target, logo.speed - step);
      logo.phase += logo.speed * dt;
      return true;
    }
    if (!logo.stop) {
      const to = Math.ceil(logo.phase / TURN - 1e-6) * TURN;
      const speed = Math.max(logo.speed, SPEEDS.idle);
      logo.stop = { from: logo.phase, to: to, time: 0,
        duration: Math.min(1.8, Math.max(0.3, 2 * (to - logo.phase) / speed)) };
    }
    logo.stop.time += dt;
    const t = P.logoMath.clamp01(logo.stop.time / logo.stop.duration);
    logo.phase = logo.stop.from + (logo.stop.to - logo.stop.from) * (1 - (1 - t) * (1 - t));
    if (t < 1) {
      return true;
    }
    logo.phase = 0;
    logo.speed = 0;
    logo.stop = null;
    return false;
  }

  function render(logo) {
    // Перерисовывает логотип в текущей позе
    const ratio = window.devicePixelRatio || 1;
    const css = logo.size + logo.pad * 2;
    const pixels = Math.round(css * ratio);
    if (logo.canvas.width !== pixels) {
      logo.canvas.width = logo.canvas.height = pixels;
    }
    const ctx = logo.canvas.getContext("2d");
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.clearRect(0, 0, pixels, pixels);
    ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
    const moving = logo.phase % TURN !== 0;
    const camera = P.logoModel.cameraDistance;
    P.logoRender.draw(ctx, {
      centerX: css / 2,
      centerY: css / 2,
      scale: logo.size / 4 * (camera - P.logoModel.depth / 2) / camera,
      yaw: logo.phase - LINGER * Math.sin(logo.phase),
      pitch: 0.28 * Math.sin(logo.phase),
      roll: 0.06 * Math.sin(2 * logo.phase),
      inflate: moving ? 0.35 : 0,
    }, P.logoModel.boxes(null), P.logo.background());
  }

  P.on("theme", function () {
    background = null;
  });
})();
