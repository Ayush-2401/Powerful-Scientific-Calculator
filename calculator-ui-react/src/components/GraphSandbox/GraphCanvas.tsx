import React, { useRef, useState, useEffect, useCallback, useMemo } from 'react';
import { Viewport } from './useViewport';

export interface EvaluatedCurve {
  id: string;
  color: string;
  points: { x: number; y: number }[];
  evaluate: (x: number) => number;
}

export interface ImplicitCurve {
  id: string;
  color: string;
  lines: Array<{ x1: number; y1: number; x2: number; y2: number }>;
}

export interface Point2D {
  x: number;
  y: number;
}

export interface ParametricCurve {
  id: string;
  color: string;
  evaluateX: (t: number) => number;
  evaluateY: (t: number) => number;
}

interface GraphCanvasProps {
  viewport: Viewport;
  pan: (dx: number, dy: number, width: number, height: number) => void;
  zoom: (factor: number, clientX: number, clientY: number, width: number, height: number) => void;
  resetViewport: () => void;
  curves: EvaluatedCurve[];
  toScreenX: (x: number, width: number) => number;
  toScreenY: (y: number, height: number) => number;
  toMathX: (screenX: number, width: number) => number;
  
  // Advanced features
  implicitCurves?: ImplicitCurve[];
  regressionPoints?: Point2D[];
  showTangent?: boolean;
  showIntegral?: boolean;
  integralBounds?: { a: number; b: number };
  showRiemann?: boolean;
  riemannIntervals?: number;
  riemannType?: 'left' | 'right' | 'midpoint';
  parametricCurves?: ParametricCurve[];
}

export const GraphCanvas: React.FC<GraphCanvasProps> = ({
  viewport,
  pan,
  zoom,
  resetViewport,
  curves,
  toScreenX,
  toScreenY,
  toMathX,
  implicitCurves = [],
  regressionPoints = [],
  showTangent = false,
  showIntegral = false,
  integralBounds = { a: -2, b: 2 },
  showRiemann = false,
  riemannIntervals = 5,
  riemannType = 'midpoint',
  parametricCurves = [],
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [dimensions, setDimensions] = useState({ width: 600, height: 400 });
  const [isDragging, setIsDragging] = useState(false);
  const dragStart = useRef({ x: 0, y: 0 });

  // Hover trace state
  const [hoverPoint, setHoverPoint] = useState<{
    curveId: string;
    screenX: number;
    screenY: number;
    mathX: number;
    mathY: number;
    color: string;
    label: string;
  } | null>(null);

  // Measure container dimensions
  useEffect(() => {
    if (!containerRef.current) return;
    const resizeObserver = new ResizeObserver((entries) => {
      for (let entry of entries) {
        setDimensions({
          width: Math.max(100, entry.contentRect.width),
          height: Math.max(100, entry.contentRect.height),
        });
      }
    });
    resizeObserver.observe(containerRef.current);
    return () => resizeObserver.disconnect();
  }, []);

  const { width, height } = dimensions;

  // Helper to compute grid step size based on current range
  const calculateGridStep = (range: number) => {
    const roughStep = range / 10;
    if (roughStep <= 0) return 1;
    const orderOfMagnitude = Math.floor(Math.log10(roughStep));
    const normalizedRoughStep = roughStep / Math.pow(10, orderOfMagnitude);

    let multiplier = 1;
    if (normalizedRoughStep < 1.5) multiplier = 1;
    else if (normalizedRoughStep < 3.5) multiplier = 2;
    else if (normalizedRoughStep < 7.5) multiplier = 5;
    else multiplier = 10;

    return multiplier * Math.pow(10, orderOfMagnitude);
  };

  const { xMin, xMax, yMin, yMax } = viewport;
  const gridStepX = calculateGridStep(xMax - xMin);
  const gridStepY = calculateGridStep(yMax - yMin);

  // Drag interaction handlers
  const handlePointerDown = (e: React.PointerEvent) => {
    if (e.button !== 0) return; // Left click only
    setIsDragging(true);
    dragStart.current = { x: e.clientX, y: e.clientY };
    e.currentTarget.setPointerCapture(e.pointerId);
  };

  const handlePointerMove = (e: React.PointerEvent) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const clientX = e.clientX - rect.left;
    const clientY = e.clientY - rect.top;

    if (isDragging) {
      const dx = e.clientX - dragStart.current.x;
      const dy = e.clientY - dragStart.current.y;
      pan(dx, dy, width, height);
      dragStart.current = { x: e.clientX, y: e.clientY };
    }

    // Find the closest curve point to the cursor for tracing tooltip
    const cursorMathX = toMathX(clientX, width);
    let closestPoint: typeof hoverPoint = null;
    let minDistance = 25; // Snap range limit: 25px radius

    curves.forEach((curve) => {
      try {
        const mathY = curve.evaluate(cursorMathX);
        if (isNaN(mathY) || !isFinite(mathY)) return;

        const sX = toScreenX(cursorMathX, width);
        const sY = toScreenY(mathY, height);
        const dist = Math.sqrt((clientX - sX) ** 2 + (clientY - sY) ** 2);

        if (dist < minDistance) {
          minDistance = dist;

          // Derivative approximation for tooltip tangent info
          let derivativeStr = '';
          if (showTangent) {
            const h = 1e-5;
            const y1 = curve.evaluate(cursorMathX - h);
            const y2 = curve.evaluate(cursorMathX + h);
            const slope = (y2 - y1) / (2 * h);
            if (!isNaN(slope) && isFinite(slope)) {
              derivativeStr = `, dy/dx: ${slope.toFixed(2)}`;
            }
          }

          closestPoint = {
            curveId: curve.id,
            screenX: sX,
            screenY: sY,
            mathX: cursorMathX,
            mathY: mathY,
            color: curve.color,
            label: `(${cursorMathX.toFixed(2)}, ${mathY.toFixed(2)})${derivativeStr}`,
          };
        }
      } catch (err) {
        // Evaluate failed
      }
    });

    setHoverPoint(closestPoint);
  };

  const handlePointerUp = (e: React.PointerEvent) => {
    setIsDragging(false);
    e.currentTarget.releasePointerCapture(e.pointerId);
  };

  const handleWheel = (e: React.WheelEvent) => {
    if (!containerRef.current) return;
    e.preventDefault();
    const rect = containerRef.current.getBoundingClientRect();
    const clientX = e.clientX - rect.left;
    const clientY = e.clientY - rect.top;
    const factor = e.deltaY < 0 ? 0.95 : 1.05;
    zoom(factor, clientX, clientY, width, height);
  };

  // Build grid coordinate lists
  const gridXValues: number[] = [];
  const startX = Math.ceil(xMin / gridStepX) * gridStepX;
  for (let x = startX; x <= xMax; x += gridStepX) {
    gridXValues.push(x);
  }

  const gridYValues: number[] = [];
  const startY = Math.ceil(yMin / gridStepY) * gridStepY;
  for (let y = startY; y <= yMax; y += gridStepY) {
    gridYValues.push(y);
  }

  const originScreenX = toScreenX(0, width);
  const originScreenY = toScreenY(0, height);

  // Generate SVG Path for a curve
  const generatePath = useCallback((curve: EvaluatedCurve) => {
    let path = '';
    let isDrawing = false;
    let prevScreenX = 0;
    let prevScreenY = 0;
    let prevMathX = 0;

    // Step across the screen columns
    for (let screenX = 0; screenX <= width; screenX += 1.5) {
      const mathX = toMathX(screenX, width);
      try {
        const mathY = curve.evaluate(mathX);
        if (isNaN(mathY) || !isFinite(mathY)) {
          isDrawing = false;
          continue;
        }

        const screenY = toScreenY(mathY, height);
        // Avoid drawing lines to extremely out-of-bound values (prevents overflow glitches)
        if (screenY < -1000 || screenY > height + 1000) {
          isDrawing = false;
          continue;
        }

        if (isDrawing) {
          // Check for steep transition
          if (Math.abs(screenY - prevScreenY) > 50) {
            // Adaptive sampling: insert midpoint evaluation
            const midMathX = (prevMathX + mathX) / 2;
            const midMathY = curve.evaluate(midMathX);
            if (!isNaN(midMathY) && isFinite(midMathY)) {
              const midScreenX = (prevScreenX + screenX) / 2;
              const midScreenY = toScreenY(midMathY, height);
              if (midScreenY >= -1000 && midScreenY <= height + 1000) {
                path += ` L ${midScreenX} ${midScreenY}`;
              }
            }
          }
        }

        if (!isDrawing) {
          path += `M ${screenX} ${screenY}`;
          isDrawing = true;
        } else {
          path += ` L ${screenX} ${screenY}`;
        }

        prevScreenX = screenX;
        prevScreenY = screenY;
        prevMathX = mathX;
      } catch (err) {
        isDrawing = false;
      }
    }

    return path;
  }, [width, height, toMathX, toScreenY]);

  const generateParametricPath = useCallback((curve: ParametricCurve) => {
    let path = '';
    let isDrawing = false;
    const samples = 500;
    const tMin = 0;
    const tMax = 2 * Math.PI;
    const step = (tMax - tMin) / samples;

    for (let i = 0; i <= samples; i++) {
      const t = tMin + i * step;
      try {
        const x = curve.evaluateX(t);
        const y = curve.evaluateY(t);
        if (isNaN(x) || !isFinite(x) || isNaN(y) || !isFinite(y)) {
          isDrawing = false;
          continue;
        }

        const screenX = toScreenX(x, width);
        const screenY = toScreenY(y, height);

        // Avoid drawing lines to extremely out-of-bound values
        if (screenX < -1000 || screenX > width + 1000 || screenY < -1000 || screenY > height + 1000) {
          isDrawing = false;
          continue;
        }

        if (!isDrawing) {
          path += `M ${screenX} ${screenY}`;
          isDrawing = true;
        } else {
          path += ` L ${screenX} ${screenY}`;
        }
      } catch (err) {
        isDrawing = false;
      }
    }
    return path;
  }, [width, height, toScreenX, toScreenY]);

  // Calculus overlay path generators
  const integralPath = useMemo(() => {
    if (!showIntegral || curves.length === 0 || !integralBounds) return null;
    const curve = curves[0]; // Apply to first curve
    const { a, b } = integralBounds;
    const steps = 100;
    const pathPoints: string[] = [];

    const startX = toScreenX(a, width);
    const startY = toScreenY(0, height);
    pathPoints.push(`M ${startX} ${startY}`);

    for (let i = 0; i <= steps; i++) {
      const x = a + (i / steps) * (b - a);
      try {
        const y = curve.evaluate(x);
        if (!isNaN(y) && isFinite(y)) {
          pathPoints.push(`L ${toScreenX(x, width)} ${toScreenY(y, height)}`);
        }
      } catch (e) {
        // ignore
      }
    }

    const endX = toScreenX(b, width);
    pathPoints.push(`L ${endX} ${startY}`);
    pathPoints.push('Z');
    return pathPoints.join(' ');
  }, [showIntegral, curves, integralBounds, width, height, toScreenX, toScreenY]);

  const riemannRectangles = useMemo(() => {
    if (!showRiemann || curves.length === 0 || !integralBounds) return [];
    const curve = curves[0];
    const { a, b } = integralBounds;
    const n = riemannIntervals;
    const w = (b - a) / n;
    const rects: Array<{ x: number; y: number; w: number; h: number }> = [];

    for (let i = 0; i < n; i++) {
      const xLeft = a + i * w;
      let xEval = xLeft;
      if (riemannType === 'right') {
        xEval = xLeft + w;
      } else if (riemannType === 'midpoint') {
        xEval = xLeft + w / 2;
      }

      try {
        const yEval = curve.evaluate(xEval);
        if (!isNaN(yEval) && isFinite(yEval)) {
          const sXLeft = toScreenX(xLeft, width);
          const sXRight = toScreenX(xLeft + w, width);
          const sYZero = toScreenY(0, height);
          const sYCurve = toScreenY(yEval, height);

          rects.push({
            x: Math.min(sXLeft, sXRight),
            y: Math.min(sYZero, sYCurve),
            w: Math.abs(sXRight - sXLeft),
            h: Math.abs(sYCurve - sYZero),
          });
        }
      } catch (e) {
        // ignore
      }
    }

    return rects;
  }, [showRiemann, curves, integralBounds, riemannIntervals, riemannType, width, height, toScreenX, toScreenY]);

  // Tangent line endpoints computation
  const tangentLineData = useMemo(() => {
    if (!showTangent || !hoverPoint || curves.length === 0) return null;
    const curve = curves.find((c) => c.id === hoverPoint.curveId) || curves[0];
    const x0 = hoverPoint.mathX;

    try {
      const h = 1e-5;
      const y0 = curve.evaluate(x0);
      const y1 = curve.evaluate(x0 - h);
      const y2 = curve.evaluate(x0 + h);
      const m = (y2 - y1) / (2 * h);

      if (isNaN(y0) || isNaN(m) || !isFinite(m)) return null;

      // Extrapolate line across visible domain bounds
      const yStart = m * (xMin - x0) + y0;
      const yEnd = m * (xMax - x0) + y0;

      return {
        x1: toScreenX(xMin, width),
        y1: toScreenY(yStart, height),
        x2: toScreenX(xMax, width),
        y2: toScreenY(yEnd, height),
        color: curve.color,
      };
    } catch (e) {
      return null;
    }
  }, [showTangent, hoverPoint, curves, xMin, xMax, width, height, toScreenX, toScreenY]);

  return (
    <div
      ref={containerRef}
      style={{
        position: 'relative',
        width: '100%',
        height: '100%',
        userSelect: 'none',
        borderRadius: '8px',
        overflow: 'hidden',
        background: '#141414',
        border: '1px solid rgba(255, 255, 255, 0.08)',
      }}
    >
      <svg
        width="100%"
        height="100%"
        style={{ cursor: isDragging ? 'grabbing' : 'grab', touchAction: 'none' }}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerUp}
        onWheel={handleWheel}
      >
        {/* Vertical Gridlines */}
        {gridXValues.map((x) => {
          const sX = toScreenX(x, width);
          const isAxis = Math.abs(x) < 1e-10;
          return (
            <g key={`grid-x-${x}`}>
              <line
                x1={sX}
                y1={0}
                x2={sX}
                y2={height}
                stroke={isAxis ? 'rgba(255, 255, 255, 0.25)' : 'rgba(255, 255, 255, 0.05)'}
                strokeWidth={isAxis ? 1.5 : 1}
              />
              {!isAxis && sX > 20 && sX < width - 20 && (
                <text
                  x={sX}
                  y={originScreenY >= 15 && originScreenY <= height - 15 ? originScreenY + 14 : height - 6}
                  fill="rgba(255, 255, 255, 0.4)"
                  fontSize="10"
                  textAnchor="middle"
                  style={{ pointerEvents: 'none' }}
                >
                  {Number(x.toFixed(4))}
                </text>
              )}
            </g>
          );
        })}

        {/* Horizontal Gridlines */}
        {gridYValues.map((y) => {
          const sY = toScreenY(y, height);
          const isAxis = Math.abs(y) < 1e-10;
          return (
            <g key={`grid-y-${y}`}>
              <line
                x1={0}
                y1={sY}
                x2={width}
                y2={sY}
                stroke={isAxis ? 'rgba(255, 255, 255, 0.25)' : 'rgba(255, 255, 255, 0.05)'}
                strokeWidth={isAxis ? 1.5 : 1}
              />
              {!isAxis && sY > 20 && sY < height - 20 && (
                <text
                  x={originScreenX >= 10 && originScreenX <= width - 30 ? originScreenX + 6 : 6}
                  y={sY + 4}
                  fill="rgba(255, 255, 255, 0.4)"
                  fontSize="10"
                  textAnchor="start"
                  style={{ pointerEvents: 'none' }}
                >
                  {Number(y.toFixed(4))}
                </text>
              )}
            </g>
          );
        })}

        {/* Primary Origin coordinate label */}
        {originScreenX > 0 && originScreenX < width && (
          <text
            x={originScreenX - 10}
            y={originScreenY + 14}
            fill="rgba(255, 255, 255, 0.5)"
            fontSize="10"
            style={{ pointerEvents: 'none' }}
          >
            0
          </text>
        )}

        {/* Calculus area under curve shading */}
        {integralPath && (
          <path
            d={integralPath}
            fill="rgba(24, 144, 255, 0.12)"
            stroke="none"
            style={{ pointerEvents: 'none' }}
          />
        )}

        {/* Riemann sum rectangles */}
        {riemannRectangles.map((rect, idx) => (
          <rect
            key={`riemann-${idx}`}
            x={rect.x}
            y={rect.y}
            width={rect.w}
            height={rect.h}
            fill="rgba(24, 144, 255, 0.18)"
            stroke="rgba(24, 144, 255, 0.4)"
            strokeWidth="1"
            style={{ pointerEvents: 'none' }}
          />
        ))}

        {/* Explicit function curves */}
        {curves.map((curve) => (
          <path
            key={curve.id}
            d={generatePath(curve)}
            fill="none"
            stroke={curve.color}
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        ))}

        {/* Parametric function curves */}
        {parametricCurves && parametricCurves.map((curve) => (
          <path
            key={curve.id}
            d={generateParametricPath(curve)}
            fill="none"
            stroke={curve.color}
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        ))}

        {/* Implicit function curves (marching squares line list) */}
        {implicitCurves.map((curve) => (
          <g key={curve.id} stroke={curve.color} strokeWidth="2.5" strokeLinecap="round">
            {curve.lines.map((line, idx) => (
              <line
                key={`implicit-seg-${idx}`}
                x1={toScreenX(line.x1, width)}
                y1={toScreenY(line.y1, height)}
                x2={toScreenX(line.x2, width)}
                y2={toScreenY(line.y2, height)}
              />
            ))}
          </g>
        ))}

        {/* Tangent line overlay */}
        {tangentLineData && (
          <line
            x1={tangentLineData.x1}
            y1={tangentLineData.y1}
            x2={tangentLineData.x2}
            y2={tangentLineData.y2}
            stroke={tangentLineData.color}
            strokeWidth="1.5"
            strokeDasharray="4 4"
            style={{ pointerEvents: 'none' }}
          />
        )}

        {/* Regression scatter dots */}
        {regressionPoints.map((p, idx) => (
          <circle
            key={`scatter-${idx}`}
            cx={toScreenX(p.x, width)}
            cy={toScreenY(p.y, height)}
            r="5"
            fill="#fa8c16"
            stroke="#fff"
            strokeWidth="1.5"
            style={{ pointerEvents: 'none' }}
          />
        ))}

        {/* Hover Coordinate Spot */}
        {hoverPoint && (
          <g>
            <circle
              cx={hoverPoint.screenX}
              cy={hoverPoint.screenY}
              r="6"
              fill={hoverPoint.color}
              stroke="#fff"
              strokeWidth="1.5"
              style={{ pointerEvents: 'none' }}
            />
          </g>
        )}
      </svg>

      {/* Trace Hover Tooltip */}
      {hoverPoint && (
        <div
          style={{
            position: 'absolute',
            left: `${Math.min(width - 180, Math.max(10, hoverPoint.screenX + 12))}px`,
            top: `${Math.min(height - 40, Math.max(10, hoverPoint.screenY - 32))}px`,
            background: 'rgba(0, 0, 0, 0.85)',
            border: `1px solid ${hoverPoint.color}`,
            color: '#fff',
            padding: '4px 8px',
            borderRadius: '4px',
            fontSize: '0.8rem',
            fontFamily: 'var(--font-mono)',
            pointerEvents: 'none',
            boxShadow: '0 2px 6px rgba(0,0,0,0.5)',
            zIndex: 10,
          }}
        >
          {hoverPoint.label}
        </div>
      )}

      {/* Home View reset button */}
      <button
        type="button"
        onClick={() => resetViewport()}
        style={{
          position: 'absolute',
          bottom: '10px',
          right: '10px',
          background: 'rgba(255, 255, 255, 0.05)',
          border: '1px solid rgba(255, 255, 255, 0.1)',
          color: 'rgba(255, 255, 255, 0.7)',
          padding: '4px 8px',
          borderRadius: '4px',
          fontSize: '0.75rem',
          cursor: 'pointer',
          zIndex: 5,
        }}
        onPointerDown={(e) => e.stopPropagation()}
      >
        Home
      </button>
    </div>
  );
};
