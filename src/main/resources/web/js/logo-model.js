(function () {
  "use strict";
  const P = window.Perun;
  const CORAL = [228, 138, 122];
  const YELLOW = [234, 182, 93];
  const PIXELS = ["ccyy", "c..y", "c..y", "c..y"];
  const COLORS = { c: CORAL, y: YELLOW };
  const DEPTH = 0.7;
  const COLUMNS = 4;
  const ROWS = 4;
  const STEPS = 12;
  const MOVES = [[1, 0, 0], [-1, 0, 0], [0, 1, 0], [0, -1, 0], [1, 0, 0], [-1, 0, 0], [0, 1, 0], [0, -1, 0],
    [0, 0, 1], [0, 0, -1]];
  const blocks = [];

  PIXELS.forEach(function (line, row) {
    line.split("").forEach(function (key, column) {
      if (COLORS[key]) {
        blocks.push({ home: [column, row, 0], color: COLORS[key] });
      }
    });
  });

  P.logoMath = {
    clamp01: function (value) {
      // Ограничивает число отрезком от 0 до 1
      return value < 0 ? 0 : value > 1 ? 1 : value;
    },

    smooth: function (value) {
      // Плавно сглаживает переход по кривой
      const t = P.logoMath.clamp01(value);
      return t * t * t * (t * (t * 6 - 15) + 10);
    },

    easeOut: function (value) {
      // Замедляет движение к концу
      const t = 1 - P.logoMath.clamp01(value);
      return 1 - t * t * t;
    },

    hash: function (value) {
      // Дает детерминированное случайное число
      let x = Math.imul(value ^ 0x9e3779b9, 0x85ebca6b);
      x ^= x >>> 13;
      x = Math.imul(x, 0xc2b2ae35);
      x ^= x >>> 16;
      return (x >>> 0) / 0x100000000;
    },
  };

  P.logoModel = {
    blocks: blocks,
    depth: DEPTH,
    steps: STEPS,
    cameraDistance: 10,
    radius: Math.hypot(COLUMNS / 2, ROWS / 2, 1 + DEPTH / 2),

    boxes: function (offsets) {
      // Строит кубики модели со смещениями
      return blocks.map(function (block, index) {
        const offset = offsets ? offsets[index] : [0, 0, 0];
        const x = block.home[0] - COLUMNS / 2 + offset[0];
        const y = block.home[1] - ROWS / 2 + offset[1];
        return { min: [x, y, offset[2] - DEPTH / 2], max: [x + 1, y + 1, offset[2] + DEPTH / 2], color: block.color };
      });
    },

    shuffle: function (cycle) {
      // Раскладывает пятнашки на один цикл
      let positions = blocks.map(function (block) {
        return block.home;
      });
      const steps = [positions];
      let lastMoved = new Set();
      let seed = cycle * 7919 + 1;
      const next = function () {
        return P.logoMath.hash(seed++);
      };
      for (let step = 0; step < STEPS; step++) {
        const occupied = new Set(positions.map(key));
        const nextPositions = positions.slice();
        const moved = new Set();
        const count = next() < 0.4 ? 2 : 1;
        for (let move = 0; move < count; move++) {
          const candidates = freeMoves(positions, occupied, moved);
          const fresh = candidates.filter(function (candidate) {
            return !lastMoved.has(candidate.block);
          });
          const pool = fresh.length ? fresh : candidates;
          const choice = pool[Math.floor(next() * pool.length)];
          if (!choice) {
            break;
          }
          occupied.add(key(choice.target));
          nextPositions[choice.block] = choice.target;
          moved.add(choice.block);
        }
        lastMoved = moved;
        positions = nextPositions;
        steps.push(positions);
      }
      return steps;
    },
  };

  function freeMoves(positions, occupied, moved) {
    // Ищет кубики со свободной соседней клеткой
    const candidates = [];
    positions.forEach(function (position, block) {
      if (moved.has(block)) {
        return;
      }
      MOVES.forEach(function (delta) {
        const target = [position[0] + delta[0], position[1] + delta[1], position[2] + delta[2]];
        const inside = target[0] >= 0 && target[0] < COLUMNS && target[1] >= 0 && target[1] < ROWS &&
          target[2] >= -1 && target[2] <= 1;
        if (inside && !occupied.has(key(target))) {
          candidates.push({ block: block, target: target });
        }
      });
    });
    return candidates;
  }

  function key(cell) {
    // Кодирует клетку сетки одним числом
    return ((cell[2] + 1) * ROWS + cell[1]) * COLUMNS + cell[0];
  }
})();
