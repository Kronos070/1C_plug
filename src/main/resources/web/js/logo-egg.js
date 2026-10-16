(function () {
  "use strict";
  const P = window.Perun;
  const M = P.logoMath;
  const T = P.logoTimeline;
  const TURN = Math.PI * 2;
  const EXIT_DURATION = 1.1;
  const DARK_STARS = [[255, 255, 255], [228, 138, 122], [234, 182, 93], [176, 196, 255]];
  const LIGHT_STARS = [[96, 100, 120], [214, 112, 96], [206, 150, 58], [110, 130, 200]];
  let egg = null;

  P.logoEgg = {
    play: function (logo) {
      // Запускает полноэкранную пасхалку логотипа
      if (egg || P.logo.reduced()) {
        return;
      }
      const canvas = P.h("canvas");
      const hint = P.h("div", { class: "egg-hint", html: "<b>Esc</b> или клик — вернуться" });
      const overlay = P.h("div", { class: "egg" }, [canvas, hint]);
      egg = { logo: logo, overlay: overlay, canvas: canvas, hint: hint, start: performance.now(), exit: null,
        rect: logo.el.getBoundingClientRect(), stars: [], width: 0, height: 0 };
      overlay.addEventListener("click", close);
      document.addEventListener("keydown", onKey, true);
      document.body.append(overlay);
      document.getElementById("app").classList.add("egg-under");
      logo.el.style.visibility = "hidden";
      requestAnimationFrame(frame);
    },
  };

  function frame() {
    // Рисует очередной кадр пасхалки
    if (!egg) {
      return;
    }
    const now = performance.now();
    const background = P.logo.background();
    resize(background);
    let state;
    if (egg.exit) {
      const progress = M.clamp01((now - egg.exit.start) / 1000 / EXIT_DURATION);
      if (progress >= 1) {
        finish();
        return;
      }
      state = leaving(progress);
    } else {
      const time = (now - egg.start) / 1000;
      state = { time: time, pose: pose(T.fly(time), time), offsets: T.offsets(time), hint: T.hint(time),
        stars: T.stars(time), backdrop: M.smooth(time / 0.5) };
    }
    egg.overlay.style.backgroundColor = "rgba(" + background.join(",") + "," + (state.backdrop * 0.9) + ")";
    egg.hint.style.opacity = String(state.hint);
    const ratio = window.devicePixelRatio || 1;
    const ctx = egg.canvas.getContext("2d");
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.clearRect(0, 0, egg.canvas.width, egg.canvas.height);
    ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
    stars(ctx, state.stars, state.time);
    P.logoRender.draw(ctx, state.pose, P.logoModel.boxes(state.offsets), background);
    requestAnimationFrame(frame);
  }

  function leaving(progress) {
    // Считает кадр возврата логотипа на место
    const exit = egg.exit;
    const landing = M.clamp01(progress / 0.8);
    const settle = 1 - M.smooth(landing);
    const gather = 1 - M.smooth(progress / 0.5);
    const result = pose(T.fly(exit.time) * settle, exit.time);
    result.yaw = exit.yaw + (exit.target - exit.yaw) * M.easeOut(landing);
    result.pitch *= settle;
    result.roll *= settle;
    return {
      time: exit.time,
      pose: result,
      offsets: exit.offsets.map(function (value) {
        return [value[0] * gather, value[1] * gather, value[2] * gather];
      }),
      hint: T.hint(exit.time) * (1 - M.smooth(progress / 0.2)),
      stars: T.stars(exit.time) * (1 - M.smooth(progress / 0.3)),
      backdrop: M.smooth(exit.time / 0.5) * (1 - M.smooth(progress / 0.85)),
    };
  }

  function pose(progress, time) {
    // Считает положение и масштаб логотипа
    if (egg.logo.el.isConnected) {
      egg.rect = egg.logo.el.getBoundingClientRect();
    }
    const camera = P.logoModel.cameraDistance;
    const radius = P.logoModel.radius;
    const startScale = egg.logo.size / 4 * (camera - P.logoModel.depth / 2) / camera;
    const reach = radius * camera / (camera - radius);
    const endScale = Math.max(startScale, Math.min(egg.width * 0.35, egg.height * 0.48 - 4) / reach);
    const startX = egg.rect.left + egg.rect.width / 2;
    const startY = egg.rect.top + egg.rect.height / 2;
    const phase = T.phase(time);
    return {
      centerX: startX + (egg.width / 2 - startX) * progress,
      centerY: startY + (egg.height / 2 - startY) * progress,
      scale: startScale * Math.pow(endScale / startScale, progress),
      yaw: T.yaw(time),
      pitch: 0.3 * Math.sin(phase) * progress,
      roll: 0.06 * Math.sin(2 * phase) * progress,
      inflate: 0.5 * progress,
    };
  }

  function resize(background) {
    // Подгоняет холст и звезды под окно
    const width = window.innerWidth;
    const height = window.innerHeight;
    const ratio = window.devicePixelRatio || 1;
    if (width === egg.width && height === egg.height) {
      return;
    }
    egg.width = width;
    egg.height = height;
    egg.canvas.width = Math.round(width * ratio);
    egg.canvas.height = Math.round(height * ratio);
    const light = 0.2126 * background[0] + 0.7152 * background[1] + 0.0722 * background[2] > 128;
    const tints = light ? LIGHT_STARS : DARK_STARS;
    const count = Math.round(width * height / 2400);
    egg.stars = [];
    for (let index = 0; index < count; index++) {
      const seed = index * 8;
      egg.stars.push({ x: M.hash(seed + 1) * width, y: M.hash(seed + 2) * height,
        size: 0.8 + M.hash(seed + 3) * 1.2, tint: "rgb(" + tints[Math.floor(M.hash(seed + 4) * tints.length)] + ")",
        speed: 0.6 + M.hash(seed + 5) * 1.8, offset: M.hash(seed + 6) * TURN });
    }
  }

  function stars(ctx, alpha, time) {
    // Рисует мерцающее звездное небо
    if (alpha <= 0) {
      return;
    }
    egg.stars.forEach(function (star) {
      ctx.globalAlpha = alpha * (0.55 + 0.45 * Math.sin(time * star.speed + star.offset));
      ctx.fillStyle = star.tint;
      ctx.fillRect(star.x, star.y, star.size, star.size);
    });
    ctx.globalAlpha = 1;
  }

  function close() {
    // Запускает возврат логотипа на место
    if (!egg) {
      return;
    }
    if (egg.exit) {
      finish();
      return;
    }
    const time = (performance.now() - egg.start) / 1000;
    const yaw = T.yaw(time);
    egg.exit = { start: performance.now(), time: time, yaw: yaw, target: Math.ceil(yaw / TURN) * TURN,
      offsets: T.offsets(time) };
    document.getElementById("app").classList.remove("egg-under");
  }

  function finish() {
    // Убирает пасхалку и показывает логотип
    document.removeEventListener("keydown", onKey, true);
    document.getElementById("app").classList.remove("egg-under");
    egg.overlay.remove();
    egg.logo.el.style.visibility = "";
    egg = null;
  }

  function onKey(event) {
    // Закрывает пасхалку по Escape
    if (event.key === "Escape") {
      event.preventDefault();
      event.stopPropagation();
      close();
    }
  }
})();
