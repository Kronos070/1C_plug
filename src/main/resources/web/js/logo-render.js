(function () {
  "use strict";
  const P = window.Perun;
  const LIGHT = normalize([-0.45, -0.6, 0.75]);
  const HALF = normalize([LIGHT[0], LIGHT[1], LIGHT[2] + 1]);
  const FRONT = { dark: shade(0, 0, 1, false), light: shade(0, 0, 1, true) };

  P.logoRender = {
    draw: function (ctx, pose, boxes, background) {
      // Рисует кубики логотипа со светом и тенью
      const m = rotation(pose.yaw, pose.pitch, pose.roll);
      const camera = P.logoModel.cameraDistance;
      const origin = [m[6] * camera, m[7] * camera, m[8] * camera];
      const light = 0.2126 * background[0] + 0.7152 * background[1] + 0.0722 * background[2] > 128;
      boxes.map(function (box) {
        const center = [0, 1, 2].map(function (axis) {
          return (box.min[axis] + box.max[axis]) / 2 - origin[axis];
        });
        return { box: box, distance: Math.hypot(center[0], center[1], center[2]) };
      }).sort(function (a, b) {
        return b.distance - a.distance;
      }).forEach(function (entry) {
        faces(entry.box, boxes, m, origin).forEach(function (face) {
          fill(ctx, face, m, pose, light, background);
        });
      });
    },
  };

  function faces(box, boxes, m, origin) {
    // Собирает видимые и неприкрытые грани кубика
    const result = [];
    for (let a = 0; a < 3; a++) {
      const u = (a + 1) % 3;
      const v = (a + 2) % 3;
      [-1, 1].forEach(function (side) {
        const plane = side > 0 ? box.max[a] : box.min[a];
        if ((origin[a] - plane) * side <= 0) {
          return;
        }
        const covered = boxes.some(function (other) {
          return other !== box && (side > 0 ? other.min[a] : other.max[a]) === plane &&
            other.min[u] <= box.min[u] && other.max[u] >= box.max[u] &&
            other.min[v] <= box.min[v] && other.max[v] >= box.max[v];
        });
        if (!covered) {
          result.push({ box: box, axis: a, side: side, plane: plane, u: u, v: v });
        }
      });
    }
    return result;
  }

  function fill(ctx, face, m, pose, light, background) {
    // Проецирует грань и закрашивает ее
    const camera = P.logoModel.cameraDistance;
    const box = face.box;
    const corners = [[box.min[face.u], box.min[face.v]], [box.max[face.u], box.min[face.v]],
      [box.max[face.u], box.max[face.v]], [box.min[face.u], box.max[face.v]]];
    let depth = 0;
    const points = corners.map(function (corner) {
      const p = [0, 0, 0];
      p[face.axis] = face.plane;
      p[face.u] = corner[0];
      p[face.v] = corner[1];
      const x = m[0] * p[0] + m[1] * p[1] + m[2] * p[2];
      const y = m[3] * p[0] + m[4] * p[1] + m[5] * p[2];
      const z = m[6] * p[0] + m[7] * p[1] + m[8] * p[2];
      const perspective = pose.scale * camera / (camera - z);
      depth += z / 4;
      return [pose.centerX + x * perspective, pose.centerY + y * perspective];
    });
    const n = [m[face.axis] * face.side, m[3 + face.axis] * face.side, m[6 + face.axis] * face.side];
    const front = light ? FRONT.light : FRONT.dark;
    const lit = shade(n[0], n[1], n[2], light);
    const gain = light ? Math.min(1, lit.light / front.light) : lit.light / front.light;
    const glow = Math.max(0, lit.specular - front.specular);
    const fog = P.logoMath.clamp01((P.logoModel.depth / 2 - depth) * 0.12) * (light ? 0.5 : 1);
    const color = box.color.map(function (channel, index) {
      const value = Math.min(255, channel * gain + glow);
      return Math.round(value + (background[index] - value) * fog);
    });
    ctx.fillStyle = "rgb(" + color.join(",") + ")";
    ctx.beginPath();
    inflate(points, pose.inflate || 0).forEach(function (point, index) {
      if (index === 0) {
        ctx.moveTo(point[0], point[1]);
      } else {
        ctx.lineTo(point[0], point[1]);
      }
    });
    ctx.closePath();
    ctx.fill();
  }

  function inflate(points, amount) {
    // Расширяет грань, чтобы скрыть швы
    if (!amount) {
      return points;
    }
    const cx = (points[0][0] + points[1][0] + points[2][0] + points[3][0]) / 4;
    const cy = (points[0][1] + points[1][1] + points[2][1] + points[3][1]) / 4;
    return points.map(function (point) {
      const dx = point[0] - cx;
      const dy = point[1] - cy;
      const length = Math.hypot(dx, dy) || 1;
      return [point[0] + dx / length * amount, point[1] + dy / length * amount];
    });
  }

  function shade(nx, ny, nz, light) {
    // Считает освещенность грани по нормали
    const diffuse = Math.max(0, nx * LIGHT[0] + ny * LIGHT[1] + nz * LIGHT[2]);
    const rim = Math.max(0, nx * 0.8 - nz * 0.3);
    const specular = light ? 0 : Math.pow(Math.max(0, nx * HALF[0] + ny * HALF[1] + nz * HALF[2]), 24) * 0.6 * 255;
    return { light: light ? Math.min(1, 0.55 + diffuse * 0.45 + rim * 0.1) : 0.45 + diffuse * 0.78 + rim * 0.25,
      specular: specular };
  }

  function rotation(yaw, pitch, roll) {
    // Строит матрицу поворота модели
    const sy = Math.sin(yaw);
    const cy = Math.cos(yaw);
    const sx = Math.sin(pitch);
    const cx = Math.cos(pitch);
    const sz = Math.sin(roll);
    const cz = Math.cos(roll);
    return multiply([cz, -sz, 0, sz, cz, 0, 0, 0, 1], multiply([1, 0, 0, 0, cx, -sx, 0, sx, cx],
      [cy, 0, sy, 0, 1, 0, -sy, 0, cy]));
  }

  function multiply(a, b) {
    // Перемножает матрицы три на три
    const result = new Array(9);
    for (let row = 0; row < 3; row++) {
      for (let column = 0; column < 3; column++) {
        result[row * 3 + column] = a[row * 3] * b[column] + a[row * 3 + 1] * b[3 + column] +
          a[row * 3 + 2] * b[6 + column];
      }
    }
    return result;
  }

  function normalize(vector) {
    // Приводит вектор к единичной длине
    const length = Math.hypot(vector[0], vector[1], vector[2]);
    return [vector[0] / length, vector[1] / length, vector[2] / length];
  }
})();
