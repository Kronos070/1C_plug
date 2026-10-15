(function () {
  "use strict";
  const P = window.Perun;
  const M = P.logoMath;
  const TURN = Math.PI * 2;
  const FLY_START = 0.1;
  const FLY_DURATION = 1.3;
  const PUZZLE_START = FLY_START + FLY_DURATION + 3;
  const PUZZLE_STEP = 0.3;
  const PUZZLE_RETURN = 1;
  const PUZZLE_HOLD = 0.6;
  const STEPS = P.logoModel.steps;
  const PUZZLE_CYCLE = PUZZLE_STEP * STEPS + PUZZLE_RETURN + PUZZLE_HOLD;
  const SPIN_RAMP = 1.4;
  const SPIN_SPEED = TURN / PUZZLE_CYCLE;
  const SPIN_LINGER = 0.6;
  const FIRST_FRONT_VIEW = PUZZLE_START + PUZZLE_STEP * STEPS + PUZZLE_RETURN + PUZZLE_HOLD / 2;
  const SPIN_ALIGNMENT = TURN - (baseSpin(FIRST_FRONT_VIEW) % TURN);
  let shuffle = null;

  P.logoTimeline = {
    fly: function (time) {
      // Возвращает долю пути логотипа к центру
      return M.smooth((time - FLY_START) / FLY_DURATION);
    },

    phase: function (time) {
      // Возвращает фазу вращения с выравниванием
      return baseSpin(time) + SPIN_ALIGNMENT * M.smooth((time - FLY_START) / FLY_DURATION);
    },

    yaw: function (time) {
      // Возвращает поворот с задержкой анфас
      const phase = P.logoTimeline.phase(time);
      return phase - SPIN_LINGER * Math.sin(phase);
    },

    hint: function (time) {
      // Возвращает прозрачность подсказки выхода
      return M.smooth((time - FLY_START - FLY_DURATION) / 0.5);
    },

    stars: function (time) {
      // Возвращает яркость звездного неба
      return M.smooth((time - 2.5) / 2);
    },

    offsets: function (time) {
      // Возвращает смещения кубиков в пятнашках
      const blocks = P.logoModel.blocks;
      const home = blocks.map(function () {
        return [0, 0, 0];
      });
      if (time < PUZZLE_START) {
        return home;
      }
      const cycle = Math.floor((time - PUZZLE_START) / PUZZLE_CYCLE);
      const local = time - PUZZLE_START - cycle * PUZZLE_CYCLE;
      if (!shuffle || shuffle.cycle !== cycle) {
        shuffle = { cycle: cycle, steps: P.logoModel.shuffle(cycle) };
      }
      const steps = shuffle.steps;
      const shuffleEnd = PUZZLE_STEP * STEPS;
      if (local < shuffleEnd) {
        const step = Math.floor(local / PUZZLE_STEP);
        const progress = M.smooth((local - step * PUZZLE_STEP) / PUZZLE_STEP);
        return steps[step].map(function (from, index) {
          const a = offset(from, index);
          const b = offset(steps[step + 1][index], index);
          return [0, 1, 2].map(function (axis) {
            return a[axis] + (b[axis] - a[axis]) * progress;
          });
        });
      }
      if (local < shuffleEnd + PUZZLE_RETURN) {
        const u = (local - shuffleEnd) / PUZZLE_RETURN;
        const remaining = 1 - M.smooth(u);
        const arc = Math.sin(Math.PI * M.clamp01(u)) * 0.8;
        return steps[STEPS].map(function (position, index) {
          const value = offset(position, index);
          return [value[0] * remaining, value[1] * remaining, value[2] * remaining + (index % 2 === 0 ? arc : -arc)];
        });
      }
      return home;
    },
  };

  function offset(position, index) {
    // Считает смещение кубика от его места
    const home = P.logoModel.blocks[index].home;
    return [position[0] - home[0], position[1] - home[1], position[2] - home[2]];
  }

  function baseSpin(time) {
    // Равномерное вращение с плавным разгоном
    const elapsed = time - FLY_START;
    if (elapsed <= 0) {
      return 0;
    }
    const u = elapsed / SPIN_RAMP;
    if (u < 1) {
      return SPIN_SPEED * SPIN_RAMP * (Math.pow(u, 6) - 3 * Math.pow(u, 5) + 2.5 * Math.pow(u, 4));
    }
    return SPIN_SPEED * (SPIN_RAMP * 0.5 + elapsed - SPIN_RAMP);
  }
})();
