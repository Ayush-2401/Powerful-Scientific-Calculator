export interface RegressionResult {
  type: 'linear' | 'quadratic';
  equation: string;
  r2: number;
  evaluate: (x: number) => number;
}

export interface Point {
  x: number;
  y: number;
}

function det3x3(
  a: number, b: number, c: number,
  d: number, e: number, f: number,
  g: number, h: number, i: number
): number {
  return a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
}

export function fitLinear(points: Point[]): RegressionResult | null {
  const n = points.length;
  if (n < 2) return null;

  let sumX = 0;
  let sumY = 0;
  let sumXX = 0;
  let sumXY = 0;

  for (const p of points) {
    sumX += p.x;
    sumY += p.y;
    sumXX += p.x * p.x;
    sumXY += p.x * p.y;
  }

  const denom = n * sumXX - sumX * sumX;
  if (Math.abs(denom) < 1e-12) return null;

  const m = (n * sumXY - sumX * sumY) / denom;
  const c = (sumY - m * sumX) / n;

  // Calculate R2
  const meanY = sumY / n;
  let ssTot = 0;
  let ssRes = 0;
  for (const p of points) {
    ssTot += (p.y - meanY) ** 2;
    ssRes += (p.y - (m * p.x + c)) ** 2;
  }

  const r2 = ssTot > 0 ? 1 - ssRes / ssTot : 1;

  // Build equation label
  const mLabel = m >= 0 ? `${m.toFixed(4)}` : `-${Math.abs(m).toFixed(4)}`;
  const cLabel = c >= 0 ? ` + ${c.toFixed(4)}` : ` - ${Math.abs(c).toFixed(4)}`;
  const equation = `y = ${mLabel}x${cLabel}`;

  return {
    type: 'linear',
    equation,
    r2,
    evaluate: (x: number) => m * x + c,
  };
}

export function fitQuadratic(points: Point[]): RegressionResult | null {
  const n = points.length;
  if (n < 3) return null;

  let sumX = 0;
  let sumY = 0;
  let sumXX = 0;
  let sumXXX = 0;
  let sumXXXX = 0;
  let sumXY = 0;
  let sumXXY = 0;

  for (const p of points) {
    sumX += p.x;
    sumY += p.y;
    sumXX += p.x * p.x;
    sumXXX += p.x * p.x * p.x;
    sumXXXX += p.x * p.x * p.x * p.x;
    sumXY += p.x * p.y;
    sumXXY += p.x * p.x * p.y;
  }

  // Solve 3x3 using Cramer's Rule
  const D = det3x3(
    sumXXXX, sumXXX, sumXX,
    sumXXX, sumXX, sumX,
    sumXX, sumX, n
  );

  if (Math.abs(D) < 1e-12) return null;

  const Da = det3x3(
    sumXXY, sumXXX, sumXX,
    sumXY, sumXX, sumX,
    sumY, sumX, n
  );

  const Db = det3x3(
    sumXXXX, sumXXY, sumXX,
    sumXXX, sumXY, sumX,
    sumXX, sumY, n
  );

  const Dc = det3x3(
    sumXXXX, sumXXX, sumXXY,
    sumXXX, sumXX, sumXY,
    sumXX, sumX, sumY
  );

  const a = Da / D;
  const b = Db / D;
  const c = Dc / D;

  // Calculate R2
  const meanY = sumY / n;
  let ssTot = 0;
  let ssRes = 0;
  for (const p of points) {
    ssTot += (p.y - meanY) ** 2;
    ssRes += (p.y - (a * p.x * p.x + b * p.x + c)) ** 2;
  }

  const r2 = ssTot > 0 ? 1 - ssRes / ssTot : 1;

  // Build equation label
  const aLabel = a >= 0 ? `${a.toFixed(4)}` : `-${Math.abs(a).toFixed(4)}`;
  const bLabel = b >= 0 ? ` + ${b.toFixed(4)}` : ` - ${Math.abs(b).toFixed(4)}`;
  const cLabel = c >= 0 ? ` + ${c.toFixed(4)}` : ` - ${Math.abs(c).toFixed(4)}`;
  const equation = `y = ${aLabel}x²${bLabel}x${cLabel}`;

  return {
    type: 'quadratic',
    equation,
    r2,
    evaluate: (x: number) => a * x * x + b * x + c,
  };
}
