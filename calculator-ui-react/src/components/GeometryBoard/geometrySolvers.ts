export interface LineSegment {
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  type: 'line' | 'segment' | 'ray' | 'vector';
}

export interface Circle {
  cx: number;
  cy: number;
  r: number;
}

export interface Point {
  x: number;
  y: number;
  isInfinite?: boolean;
}

// 1. Line-Line intersection
export function intersectLines(l1: LineSegment, l2: LineSegment): Point | null {
  const { x1, y1, x2, y2 } = l1;
  const { x3, y3, x4, y4 } = { x3: l2.x1, y3: l2.y1, x4: l2.x2, y4: l2.y2 };

  const denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
  if (Math.abs(denom) < 1e-10) {
    // Check if overlapping (collinear)
    const cross = (x1 - x3) * (y4 - y3) - (y1 - y3) * (x4 - x3);
    if (Math.abs(cross) < 1e-5) {
      return { x: NaN, y: NaN, isInfinite: true };
    }
    return null; // Parallel or coincident
  }

  // Let's use the explicit coordinate formula for the intersection point:
  const px = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / denom;
  const py = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / denom;

  const pt: Point = { x: px, y: py };

  // Check bounds constraints for both shapes
  if (!isPointOnLineBounds(pt, l1) || !isPointOnLineBounds(pt, l2)) {
    return null;
  }

  return pt;
}

function isPointOnLineBounds(pt: Point, line: LineSegment): boolean {
  const { x1, y1, x2, y2, type } = line;
  const { x, y } = pt;

  if (type === 'line') return true;

  const dx = x2 - x1;
  const dy = y2 - y1;
  const dot = (x - x1) * dx + (y - y1) * dy;
  const lineLengthSq = dx * dx + dy * dy;

  if (type === 'segment' || type === 'vector') {
    // Parameter t must be in [0, 1]
    const t = dot / lineLengthSq;
    return t >= -1e-4 && t <= 1 + 1e-4;
  }

  if (type === 'ray') {
    // Parameter t must be >= 0
    return dot >= -1e-4;
  }

  return true;
}

// 2. Line-Circle intersection
export function intersectLineCircle(line: LineSegment, circle: Circle): Point[] {
  const { x1, y1, x2, y2 } = line;
  const { cx, cy, r } = circle;

  const dx = x2 - x1;
  const dy = y2 - y1;

  const a = dx * dx + dy * dy;
  if (a < 1e-10) return []; // Points are coincident

  const b = 2 * (dx * (x1 - cx) + dy * (y1 - cy));
  const c = (x1 - cx) * (x1 - cx) + (y1 - cy) * (y1 - cy) - r * r;

  const disc = b * b - 4 * a * c;
  if (disc < -1e-9) {
    return []; // No real roots
  }

  const results: Point[] = [];
  const tVals = Math.abs(disc) < 1e-9 ? [-b / (2 * a)] : [(-b - Math.sqrt(disc)) / (2 * a), (-b + Math.sqrt(disc)) / (2 * a)];

  tVals.forEach((t) => {
    const pt = {
      x: x1 + t * dx,
      y: y1 + t * dy,
    };
    if (isPointOnLineBounds(pt, line)) {
      results.push(pt);
    }
  });

  return results;
}

// 3. Circle-Circle intersection
export function intersectCircles(c1: Circle, c2: Circle): Point[] {
  const { cx: x1, cy: y1, r: r1 } = c1;
  const { cx: x2, cy: y2, r: r2 } = c2;

  const dx = x2 - x1;
  const dy = y2 - y1;
  const d = Math.sqrt(dx * dx + dy * dy);

  // Checks for no intersections
  if (d > r1 + r2 + 1e-8 || d < Math.abs(r1 - r2) - 1e-8 || d < 1e-10) {
    return [];
  }

  // Radical line parameter
  const a = (r1 * r1 - r2 * r2 + d * d) / (2 * d);
  const hSq = r1 * r1 - a * a;
  const h = hSq < 0 ? 0 : Math.sqrt(hSq);

  // Orthogonal direction
  const x0 = x1 + (a * dx) / d;
  const y0 = y1 + (a * dy) / d;

  const rx = -(dy * h) / d;
  const ry = (dx * h) / d;

  if (h < 1e-3) {
    // Single tangent point
    return [{ x: x0, y: y0 }];
  }

  return [
    { x: x0 + rx, y: y0 + ry },
    { x: x0 - rx, y: y0 - ry },
  ];
}

// 4. CCW Oriented Angle from BA to BC
export interface AngleResult {
  rad: number;
  deg: number;
  startAngle: number; // in radians (atan2 of BA)
  endAngle: number;   // in radians (atan2 of BC)
}

export function calculateAngle(a: Point, b: Point, c: Point): AngleResult {
  const angA = Math.atan2(a.y - b.y, a.x - b.x);
  const angC = Math.atan2(c.y - b.y, c.x - b.x);

  let diff = angC - angA;
  if (diff < 0) {
    diff += 2 * Math.PI;
  }

  return {
    rad: diff,
    deg: (diff * 180) / Math.PI,
    startAngle: angA,
    endAngle: angC,
  };
}

// 5. Polygon Area via Shoelace Formula
export function calculatePolygonArea(pts: Point[]): number {
  const n = pts.length;
  if (n < 3) return 0;
  let sum = 0;
  for (let i = 0; i < n; i++) {
    const p1 = pts[i];
    const p2 = pts[(i + 1) % n];
    sum += p1.x * p2.y - p2.x * p1.y;
  }
  return 0.5 * Math.abs(sum);
}
