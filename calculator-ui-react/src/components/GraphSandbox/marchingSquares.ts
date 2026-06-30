import * as math from 'mathjs';

export interface GridLine {
  x1: number;
  y1: number;
  x2: number;
  y2: number;
}

export function plotImplicitCurve(
  lhsStr: string,
  rhsStr: string,
  xMin: number,
  xMax: number,
  yMin: number,
  yMax: number,
  params: Record<string, number>,
  gridResolution: number = 80
): GridLine[] {
  const diffExpr = `(${lhsStr}) - (${rhsStr})`;
  let compiled: math.EvalFunction;
  try {
    compiled = math.compile(diffExpr);
  } catch (e) {
    return [];
  }

  // 1. Generate grid values
  const xVals: number[] = [];
  const yVals: number[] = [];
  for (let i = 0; i <= gridResolution; i++) {
    xVals.push(xMin + (i / gridResolution) * (xMax - xMin));
    yVals.push(yMin + (i / gridResolution) * (yMax - yMin));
  }

  const values: number[][] = [];
  for (let i = 0; i <= gridResolution; i++) {
    values[i] = [];
    const x = xVals[i];
    for (let j = 0; j <= gridResolution; j++) {
      const y = yVals[j];
      try {
        const val = compiled.evaluate({ x, y, ...params });
        values[i][j] = typeof val === 'number' ? val : NaN;
      } catch (err) {
        values[i][j] = NaN;
      }
    }
  }

  const lines: GridLine[] = [];

  // Helper for linear interpolation on cell edges
  const lerp = (
    xA: number,
    yA: number,
    vA: number,
    xB: number,
    yB: number,
    vB: number
  ) => {
    if (Math.abs(vB - vA) < 1e-12) return { x: xA, y: yA };
    const t = -vA / (vB - vA);
    // Clamp t between 0 and 1
    const ct = Math.max(0, Math.min(1, t));
    return {
      x: xA + ct * (xB - xA),
      y: yA + ct * (yB - yA),
    };
  };

  // 2. Marching Squares grid traversal
  for (let i = 0; i < gridResolution; i++) {
    const x0 = xVals[i];
    const x1 = xVals[i + 1];
    for (let j = 0; j < gridResolution; j++) {
      const y0 = yVals[j];
      const y1 = yVals[j + 1];

      const v0 = values[i][j];       // Bottom-left
      const v1 = values[i + 1][j];   // Bottom-right
      const v2 = values[i + 1][j + 1]; // Top-right
      const v3 = values[i][j + 1];   // Top-left

      // Skip cells containing NaN values
      if (isNaN(v0) || isNaN(v1) || isNaN(v2) || isNaN(v3)) {
        continue;
      }

      // Compute standard binary case index
      let caseIdx = 0;
      if (v0 >= 0) caseIdx |= 1;
      if (v1 >= 0) caseIdx |= 2;
      if (v2 >= 0) caseIdx |= 4;
      if (v3 >= 0) caseIdx |= 8;

      if (caseIdx === 0 || caseIdx === 15) continue;

      // Edge crossing points
      const pBottom = () => lerp(x0, y0, v0, x1, y0, v1);
      const pRight = () => lerp(x1, y0, v1, x1, y1, v2);
      const pTop = () => lerp(x0, y1, v3, x1, y1, v2);
      const pLeft = () => lerp(x0, y0, v0, x0, y1, v3);

      const addLine = (pA: { x: number; y: number }, pB: { x: number; y: number }) => {
        lines.push({ x1: pA.x, y1: pA.y, x2: pB.x, y2: pB.y });
      };

      switch (caseIdx) {
        case 1:
        case 14:
          addLine(pBottom(), pLeft());
          break;
        case 2:
        case 13:
          addLine(pBottom(), pRight());
          break;
        case 3:
        case 12:
          addLine(pLeft(), pRight());
          break;
        case 4:
        case 11:
          addLine(pTop(), pRight());
          break;
        case 5: {
          const centerVal = (v0 + v1 + v2 + v3) / 4;
          if (centerVal >= 0) {
            addLine(pLeft(), pTop());
            addLine(pBottom(), pRight());
          } else {
            addLine(pLeft(), pBottom());
            addLine(pTop(), pRight());
          }
          break;
        }
        case 6:
        case 9:
          addLine(pBottom(), pTop());
          break;
        case 7:
        case 8:
          addLine(pLeft(), pTop());
          break;
        case 10: {
          const centerVal = (v0 + v1 + v2 + v3) / 4;
          if (centerVal >= 0) {
            addLine(pLeft(), pBottom());
            addLine(pTop(), pRight());
          } else {
            addLine(pLeft(), pTop());
            addLine(pBottom(), pRight());
          }
          break;
        }
      }
    }
  }

  return lines;
}
