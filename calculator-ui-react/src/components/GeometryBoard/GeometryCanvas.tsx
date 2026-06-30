import React, { useRef, useState, useEffect, useCallback, useMemo } from 'react';
import { Viewport } from '../GraphSandbox/useViewport';
import { calculateAngle, intersectLines, intersectLineCircle, intersectCircles, Point as MathPoint } from './geometrySolvers';

export interface GeometryPoint {
  id: string;
  x: number;
  y: number;
  label: string;
  color: string;
  type?: 'point_free' | 'point_midpoint' | 'point_intersection' | 'point_reflected' | 'point_rotated' | 'point_translated' | 'point_dilated';
  dependencies?: string[]; // IDs of elements it depends on
}

export interface GeometryShape {
  id: string;
  type: 'line' | 'segment' | 'ray' | 'vector' | 'circle' | 'midpoint' | 'intersect' | 'angle' | 'ellipse' | 'polygon' | 'reflect' | 'rotate' | 'translate' | 'dilate';
  label: string;
  color: string;
  dependencies: string[]; // point IDs or shape IDs
  value?: number; // length, radius, angle deg
}

interface GeometryCanvasProps {
  viewport: Viewport;
  pan: (dx: number, dy: number, width: number, height: number) => void;
  zoom: (factor: number, clientX: number, clientY: number, width: number, height: number) => void;
  resetViewport: () => void;
  toScreenX: (x: number, width: number) => number;
  toScreenY: (y: number, height: number) => number;
  toMathX: (screenX: number, width: number) => number;
  toMathY: (screenY: number, height: number) => number;
  
  points: GeometryPoint[];
  shapes: GeometryShape[];
  activeTool: string;
  tempPoints: string[]; // point IDs clicked in active tool sequence
  tempShapes: string[]; // shape IDs clicked in intersect tool sequence
  onAddPoint: (x: number, y: number) => string;
  onAddShape: (type: string, dependencies: string[]) => void;
  onDragPoint: (id: string, x: number, y: number) => void;
  onUpdateTempPoints: (pts: string[]) => void;
  onSelectShapeForIntersection: (shapeId: string) => void;
  pointTraces?: Record<string, Array<{ x: number; y: number }>>;
  snapEnabled?: boolean;
}

export const GeometryCanvas: React.FC<GeometryCanvasProps> = ({
  viewport,
  pan,
  zoom,
  resetViewport,
  toScreenX,
  toScreenY,
  toMathX,
  toMathY,
  points,
  shapes,
  activeTool,
  tempPoints,
  tempShapes,
  onAddPoint,
  onAddShape,
  onDragPoint,
  onUpdateTempPoints,
  onSelectShapeForIntersection,
  pointTraces = {},
  snapEnabled = true,
}) => {
  const pointsMap = useMemo(() => {
    return new Map(points.map((p) => [p.id, p]));
  }, [points]);

  const containerRef = useRef<HTMLDivElement>(null);
  const [dimensions, setDimensions] = useState({ width: 600, height: 400 });
  const [isPanning, setIsPanning] = useState(false);
  const [draggedPointId, setDraggedPointId] = useState<string | null>(null);
  const dragStart = useRef({ x: 0, y: 0 });

  // Hover feedback
  const [hoverCoord, setHoverCoord] = useState<{ x: number; y: number; sX: number; sY: number } | null>(null);
  const [hoveredPointId, setHoveredPointId] = useState<string | null>(null);
  const [hoveredShapeId, setHoveredShapeId] = useState<string | null>(null);

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

  // Helper to compute grid step size
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

    // Find snapped coordinates (math and screen space)
  const getSnapCoordinates = useCallback((screenX: number, screenY: number) => {
    const mathX = toMathX(screenX, width);
    const mathY = toMathY(screenY, height);

    if (!snapEnabled) {
      return { mathX, mathY, sX: screenX, sY: screenY, pointId: null };
    }

    // 1. Check proximity to points (snap threshold: 12px)
    for (let p of points) {
      const pSX = toScreenX(p.x, width);
      const pSY = toScreenY(p.y, height);
      const dist = Math.sqrt((screenX - pSX) ** 2 + (screenY - pSY) ** 2);
      if (dist < 12) {
        return { mathX: p.x, mathY: p.y, sX: pSX, sY: pSY, pointId: p.id };
      }
    }

    // 2. Check proximity to shape-to-shape intersections (snap threshold: 12px)
    const resolvedGeoms = shapes.map((s) => {
      const pts = s.dependencies.map((id) => pointsMap.get(id)).filter(Boolean);
      if (pts.length < 2 || !pts[0] || !pts[1]) return null;
      const [p1, p2] = pts;
      if (isNaN(p1.x) || isNaN(p1.y) || isNaN(p2.x) || isNaN(p2.y)) return null;

      if (s.type === 'circle') {
        const r = Math.sqrt((p2.x - p1.x) ** 2 + (p2.y - p1.y) ** 2);
        return { id: s.id, type: 'circle', cx: p1.x, cy: p1.y, r };
      }
      return { id: s.id, type: s.type, x1: p1.x, y1: p1.y, x2: p2.x, y2: p2.y };
    }).filter(Boolean);

    for (let i = 0; i < resolvedGeoms.length; i++) {
      for (let j = i + 1; j < resolvedGeoms.length; j++) {
        const g1 = resolvedGeoms[i];
        const g2 = resolvedGeoms[j];
        if (!g1 || !g2) continue;

        let intersectionPts: MathPoint[] = [];
        try {
          if (g1.type === 'circle' && g2.type === 'circle') {
            intersectionPts = intersectCircles(g1, g2);
          } else if (g1.type === 'circle') {
            intersectionPts = intersectLineCircle(g2 as any, g1);
          } else if (g2.type === 'circle') {
            intersectionPts = intersectLineCircle(g1 as any, g2);
          } else {
            const pt = intersectLines(g1 as any, g2 as any);
            intersectionPts = pt ? [pt] : [];
          }
        } catch (e) {
          // ignore solver failures
        }

        for (let pt of intersectionPts) {
          const sX = toScreenX(pt.x, width);
          const sY = toScreenY(pt.y, height);
          const dist = Math.sqrt((screenX - sX) ** 2 + (screenY - sY) ** 2);
          if (dist < 12) {
            return { mathX: pt.x, mathY: pt.y, sX, sY, pointId: null };
          }
        }
      }
    }

    // 3. Check proximity to grid intersections (snap threshold: 12px)
    const snapX = Math.round(mathX / gridStepX) * gridStepX;
    const snapY = Math.round(mathY / gridStepY) * gridStepY;
    const gSX = toScreenX(snapX, width);
    const gSY = toScreenY(snapY, height);
    const dist = Math.sqrt((screenX - gSX) ** 2 + (screenY - gSY) ** 2);
    if (dist < 12) {
      return { mathX: snapX, mathY: snapY, sX: gSX, sY: gSY, pointId: null };
    }

    // No snap, return continuous
    return { mathX, mathY, sX: screenX, sY: screenY, pointId: null };
  }, [points, shapes, pointsMap, width, height, toMathX, toMathY, toScreenX, toScreenY, gridStepX, gridStepY]);

  // Pointer interactions
  const handlePointerDown = (e: React.PointerEvent) => {
    if (e.button !== 0) return; // Left click only
    const rect = e.currentTarget.getBoundingClientRect();
    const screenX = e.clientX - rect.left;
    const screenY = e.clientY - rect.top;

    if (activeTool === 'intersect') {
      return;
    }

    if (['reflect', 'translate'].includes(activeTool) && tempPoints.length === 1) {
      return; // Shape click is handled by shapes themselves
    }

    const snap = getSnapCoordinates(screenX, screenY);

    if (activeTool === 'select') {
      if (snap.pointId) {
        // Can only drag free points
        const pt = points.find(p => p.id === snap.pointId);
        if (pt && (pt.type === 'point_free' || !pt.type)) {
          setDraggedPointId(snap.pointId);
          e.currentTarget.setPointerCapture(e.pointerId);
        }
      } else {
        setIsPanning(true);
        dragStart.current = { x: e.clientX, y: e.clientY };
        e.currentTarget.setPointerCapture(e.pointerId);
      }
    } else {
      // Shape / Dependent point construction tool
      let pointId = snap.pointId;
      if (!pointId) {
        // Place new point
        pointId = onAddPoint(snap.mathX, snap.mathY);
      }

      const nextTempPoints = [...tempPoints, pointId];

      if (activeTool === 'point') {
        onUpdateTempPoints([]); // Point tool finishes immediately
      } else if (activeTool === 'midpoint') {
        if (nextTempPoints.length === 2) {
          if (nextTempPoints[0] !== nextTempPoints[1]) {
            onAddShape('midpoint', nextTempPoints);
          } else {
            onUpdateTempPoints([pointId]);
          }
        } else {
          onUpdateTempPoints(nextTempPoints);
        }
      } else if (activeTool === 'angle') {
        if (nextTempPoints.length === 3) {
          if (nextTempPoints[0] !== nextTempPoints[1] && nextTempPoints[1] !== nextTempPoints[2]) {
            onAddShape('angle', nextTempPoints);
          } else {
            onUpdateTempPoints([nextTempPoints[0]]);
          }
        } else {
          onUpdateTempPoints(nextTempPoints);
        }
      } else if (activeTool === 'ellipse') {
        if (nextTempPoints.length === 3) {
          if (nextTempPoints[0] !== nextTempPoints[1] && nextTempPoints[1] !== nextTempPoints[2]) {
            onAddShape('ellipse', nextTempPoints);
          } else {
            onUpdateTempPoints([nextTempPoints[0]]);
          }
        } else {
          onUpdateTempPoints(nextTempPoints);
        }
      } else if (['rotate', 'dilate'].includes(activeTool)) {
        if (nextTempPoints.length === 2) {
          if (nextTempPoints[0] !== nextTempPoints[1]) {
            onAddShape(activeTool, nextTempPoints);
          } else {
            onUpdateTempPoints([pointId]);
          }
        } else {
          onUpdateTempPoints(nextTempPoints);
        }
      } else if (['line', 'segment', 'ray', 'vector', 'circle'].includes(activeTool)) {
        if (nextTempPoints.length === 2) {
          if (nextTempPoints[0] !== nextTempPoints[1]) {
            onAddShape(activeTool, nextTempPoints);
          } else {
            onUpdateTempPoints([pointId]);
          }
        } else {
          onUpdateTempPoints(nextTempPoints);
        }
      } else if (['reflect', 'translate'].includes(activeTool)) {
        // Point clicked (adds to tempPoints, waits for shape click next)
        onUpdateTempPoints(nextTempPoints);
      } else if (activeTool === 'polygon') {
        if (tempPoints.length >= 3 && pointId === tempPoints[0]) {
          onAddShape('polygon', tempPoints);
          onUpdateTempPoints([]);
        } else {
          onUpdateTempPoints(nextTempPoints);
        }
      }
    }
  };

  const handlePointerMove = (e: React.PointerEvent) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const screenX = e.clientX - rect.left;
    const screenY = e.clientY - rect.top;

    const snap = getSnapCoordinates(screenX, screenY);
    setHoveredPointId(snap.pointId);

    if (draggedPointId) {
      onDragPoint(draggedPointId, snap.mathX, snap.mathY);
    } else if (isPanning) {
      const dx = e.clientX - dragStart.current.x;
      const dy = e.clientY - dragStart.current.y;
      pan(dx, dy, width, height);
      dragStart.current = { x: e.clientX, y: e.clientY };
    }

    setHoverCoord({
      x: snap.mathX,
      y: snap.mathY,
      sX: snap.sX,
      sY: snap.sY,
    });
  };

  const handlePointerUp = (e: React.PointerEvent) => {
    if (draggedPointId) {
      setDraggedPointId(null);
      e.currentTarget.releasePointerCapture(e.pointerId);
    } else if (isPanning) {
      setIsPanning(false);
      e.currentTarget.releasePointerCapture(e.pointerId);
    }
  };

  const handleDoubleClick = () => {
    if (activeTool === 'polygon' && tempPoints.length >= 3) {
      onAddShape('polygon', tempPoints);
      onUpdateTempPoints([]);
    }
  };

  const handleWheel = (e: React.WheelEvent) => {
    if (!containerRef.current) return;
    e.preventDefault();
    const rect = containerRef.current.getBoundingClientRect();
    const clientX = e.clientX - rect.left;
    const clientY = e.clientY - rect.top;
    const factor = e.deltaY < 0 ? 0.9 : 1.1;
    zoom(factor, clientX, clientY, dimensions.width, dimensions.height);
  };

  // Build grid lines
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


  // Helper to generate arc polyline for angles
  const generateAngleArc = (shape: GeometryShape) => {
    const pts = shape.dependencies.map(id => pointsMap.get(id));
    if (pts.length < 3 || !pts[0] || !pts[1] || !pts[2]) return null;

    const [pA, pB, pC] = pts;

    const arcRadiusScreen = 30;
    const mathScale = (xMax - xMin) / width;
    const arcRadiusMath = arcRadiusScreen * mathScale;

    const angResult = calculateAngle(pA, pB, pC);
    const { startAngle, rad } = angResult;

    // Generate polyline points
    const steps = 20;
    const pointsStrList: string[] = [];
    for (let i = 0; i <= steps; i++) {
      const theta = startAngle + (i / steps) * rad;
      const x = pB.x + arcRadiusMath * Math.cos(theta);
      const y = pB.y + arcRadiusMath * Math.sin(theta);
      pointsStrList.push(`${toScreenX(x, width)},${toScreenY(y, height)}`);
    }

    // Label coordinates
    const midAngle = startAngle + rad / 2;
    const labelRadiusMath = (arcRadiusScreen + 16) * mathScale;
    const lx = pB.x + labelRadiusMath * Math.cos(midAngle);
    const ly = pB.y + labelRadiusMath * Math.sin(midAngle);

    return {
      points: pointsStrList.join(' '),
      labelX: toScreenX(lx, width),
      labelY: toScreenY(ly, height),
      labelText: `${angResult.deg.toFixed(1)}°`,
    };
  };

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
        style={{ cursor: isPanning || draggedPointId ? 'grabbing' : 'crosshair', touchAction: 'none' }}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerUp}
        onWheel={handleWheel}
        onDoubleClick={handleDoubleClick}
      >
        {/* Definitions for arrowheads */}
        <defs>
          <marker
            id="vector-arrow"
            viewBox="0 0 10 10"
            refX="10"
            refY="5"
            markerWidth="6"
            markerHeight="6"
            orient="auto-start-reverse"
          >
            <path d="M 0 1.5 L 10 5 L 0 8.5 z" fill="rgba(255,255,255,0.7)" />
          </marker>
        </defs>

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
                  fill="rgba(255, 255, 255, 0.3)"
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
                  fill="rgba(255, 255, 255, 0.3)"
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

        {/* Axis center coordinates */}
        {originScreenX > 0 && originScreenX < width && (
          <text
            x={originScreenX - 10}
            y={originScreenY + 14}
            fill="rgba(255, 255, 255, 0.4)"
            fontSize="10"
            style={{ pointerEvents: 'none' }}
          >
            0
          </text>
        )}

        {/* Shapes Rendering */}
        {shapes.map((shape) => {
          if (shape.type === 'angle') {
            const arcData = generateAngleArc(shape);
            if (!arcData) return null;

            return (
              <g key={shape.id}>
                {/* Arc line */}
                <polyline
                  points={arcData.points}
                  fill="none"
                  stroke={shape.color}
                  strokeWidth="1.5"
                  strokeDasharray="2 2"
                />
                {/* Degree Label */}
                <text
                  x={arcData.labelX}
                  y={arcData.labelY}
                  fill="#fff"
                  fontSize="10"
                  fontFamily="var(--font-mono)"
                  textAnchor="middle"
                  style={{ pointerEvents: 'none', background: '#000' }}
                >
                  {arcData.labelText}
                </text>
              </g>
            );
          }

          const isInteractive = ['intersect', 'reflect', 'translate'].includes(activeTool);
          const isSelected = tempShapes.includes(shape.id);
          const isHovered = hoveredShapeId === shape.id;

          const shapeStrokeColor = isSelected ? 'var(--text-accent)' : shape.color;
          const shapeStrokeWidth = isHovered ? '4' : '2';

          // Click handler for shape selection
          const handleShapeClick = (e: React.MouseEvent) => {
            if (isInteractive) {
              e.stopPropagation();
              if (activeTool === 'intersect') {
                onSelectShapeForIntersection(shape.id);
              } else if (tempPoints.length === 1) {
                onAddShape(activeTool, [tempPoints[0], shape.id]);
              }
            }
          };

          if (shape.type === 'ellipse') {
            const elPts = shape.dependencies.map(id => pointsMap.get(id)).filter(Boolean) as GeometryPoint[];
            if (elPts.length < 3) return null;
            const [pF1, pF2, pP] = elPts;

            const f1x = toScreenX(pF1.x, width);
            const f1y = toScreenY(pF1.y, height);
            const f2x = toScreenX(pF2.x, width);
            const f2y = toScreenY(pF2.y, height);
            const px = toScreenX(pP.x, width);
            const py = toScreenY(pP.y, height);

            const cx = (f1x + f2x) / 2;
            const cy = (f1y + f2y) / 2;

            const dist = (xA: number, yA: number, xB: number, yB: number) =>
              Math.sqrt((xB - xA) ** 2 + (yB - yA) ** 2);

            const c_screen = dist(f1x, f1y, f2x, f2y) / 2;
            const a_screen = (dist(px, py, f1x, f1y) + dist(px, py, f2x, f2y)) / 2;

            if (a_screen <= c_screen) return null; // Impossible ellipse
            const b_screen = Math.sqrt(a_screen ** 2 - c_screen ** 2);

            const angleRad = Math.atan2(f2y - f1y, f2x - f1x);
            const angleDeg = (angleRad * 180) / Math.PI;

            return (
              <g key={shape.id}>
                <ellipse
                  cx={cx}
                  cy={cy}
                  rx={a_screen}
                  ry={b_screen}
                  transform={`rotate(${angleDeg}, ${cx}, ${cy})`}
                  fill="transparent"
                  stroke="transparent"
                  strokeWidth="12"
                  style={{ cursor: isInteractive ? 'pointer' : 'default' }}
                  onPointerDown={handleShapeClick}
                  onMouseEnter={() => isInteractive && setHoveredShapeId(shape.id)}
                  onMouseLeave={() => isInteractive && setHoveredShapeId(null)}
                />
                <ellipse
                  cx={cx}
                  cy={cy}
                  rx={a_screen}
                  ry={b_screen}
                  transform={`rotate(${angleDeg}, ${cx}, ${cy})`}
                  fill="none"
                  stroke={shapeStrokeColor}
                  strokeWidth={shapeStrokeWidth}
                  style={{ pointerEvents: 'none' }}
                />
              </g>
            );
          }

          if (shape.type === 'polygon') {
            const polyPts = shape.dependencies.map(id => pointsMap.get(id)).filter(Boolean) as GeometryPoint[];
            if (polyPts.length < 3) return null;
            const pointsString = polyPts.map(p => `${toScreenX(p.x, width)},${toScreenY(p.y, height)}`).join(' ');

            return (
              <g key={shape.id}>
                <polygon
                  points={pointsString}
                  fill="transparent"
                  stroke="transparent"
                  strokeWidth="12"
                  style={{ cursor: isInteractive ? 'pointer' : 'default' }}
                  onPointerDown={handleShapeClick}
                  onMouseEnter={() => isInteractive && setHoveredShapeId(shape.id)}
                  onMouseLeave={() => isInteractive && setHoveredShapeId(null)}
                />
                <polygon
                  points={pointsString}
                  fill={shape.color || 'var(--text-accent)'}
                  fillOpacity="0.08"
                  stroke={shapeStrokeColor}
                  strokeWidth={shapeStrokeWidth}
                  strokeLinejoin="round"
                  style={{ pointerEvents: 'none' }}
                />
              </g>
            );
          }

          const pts = shape.dependencies.map((id) => pointsMap.get(id)).filter(Boolean) as GeometryPoint[];
          if (pts.length < 2) return null;

          const [p1, p2] = pts;
          const x1 = toScreenX(p1.x, width);
          const y1 = toScreenY(p1.y, height);
          const x2 = toScreenX(p2.x, width);
          const y2 = toScreenY(p2.y, height);

          if (shape.type === 'segment') {
            return (
              <g key={shape.id}>
                {/* Large clickable stroke overlay */}
                <line
                  x1={x1}
                  y1={y1}
                  x2={x2}
                  y2={y2}
                  stroke="transparent"
                  strokeWidth="12"
                  style={{ cursor: isInteractive ? 'pointer' : 'default' }}
                  onPointerDown={handleShapeClick}
                  onMouseEnter={() => isInteractive && setHoveredShapeId(shape.id)}
                  onMouseLeave={() => isInteractive && setHoveredShapeId(null)}
                />
                <line
                  x1={x1}
                  y1={y1}
                  x2={x2}
                  y2={y2}
                  stroke={shapeStrokeColor}
                  strokeWidth={shapeStrokeWidth}
                  style={{ pointerEvents: 'none' }}
                />
                {/* Live Length Label overlay */}
                {shape.value !== undefined && (
                  <text
                    x={(x1 + x2) / 2}
                    y={(y1 + y2) / 2 - 6}
                    fill="rgba(255,255,255,0.7)"
                    fontSize="9"
                    fontFamily="var(--font-mono)"
                    textAnchor="middle"
                    style={{ pointerEvents: 'none' }}
                  >
                    {shape.value.toFixed(2)}
                  </text>
                )}
              </g>
            );
          }

          if (shape.type === 'vector') {
            return (
              <g key={shape.id}>
                <line
                  x1={x1}
                  y1={y1}
                  x2={x2}
                  y2={y2}
                  stroke="transparent"
                  strokeWidth="12"
                  style={{ cursor: isInteractive ? 'pointer' : 'default' }}
                  onPointerDown={handleShapeClick}
                  onMouseEnter={() => isInteractive && setHoveredShapeId(shape.id)}
                  onMouseLeave={() => isInteractive && setHoveredShapeId(null)}
                />
                <line
                  x1={x1}
                  y1={y1}
                  x2={x2}
                  y2={y2}
                  stroke={shapeStrokeColor}
                  strokeWidth={shapeStrokeWidth}
                  markerEnd="url(#vector-arrow)"
                  style={{ pointerEvents: 'none' }}
                />
              </g>
            );
          }

          if (shape.type === 'line') {
            const dx = x2 - x1;
            const dy = y2 - y1;
            const len = Math.sqrt(dx * dx + dy * dy);
            if (len < 0.1) return null;

            const ux = dx / len;
            const uy = dy / len;

            const lx1 = x1 - ux * 5000;
            const ly1 = y1 - uy * 5000;
            const lx2 = x2 + ux * 5000;
            const ly2 = y2 + uy * 5000;

            return (
              <g key={shape.id}>
                <line
                  x1={lx1}
                  y1={ly1}
                  x2={lx2}
                  y2={ly2}
                  stroke="transparent"
                  strokeWidth="12"
                  style={{ cursor: isInteractive ? 'pointer' : 'default' }}
                  onPointerDown={handleShapeClick}
                  onMouseEnter={() => isInteractive && setHoveredShapeId(shape.id)}
                  onMouseLeave={() => isInteractive && setHoveredShapeId(null)}
                />
                <line
                  x1={lx1}
                  y1={ly1}
                  x2={lx2}
                  y2={ly2}
                  stroke={shapeStrokeColor}
                  strokeWidth={shapeStrokeWidth}
                  style={{ pointerEvents: 'none' }}
                />
              </g>
            );
          }

          if (shape.type === 'ray') {
            const dx = x2 - x1;
            const dy = y2 - y1;
            const len = Math.sqrt(dx * dx + dy * dy);
            if (len < 0.1) return null;

            const ux = dx / len;
            const uy = dy / len;

            const rx2 = x1 + ux * 5000;
            const ry2 = y1 + uy * 5000;

            return (
              <g key={shape.id}>
                <line
                  x1={x1}
                  y1={y1}
                  x2={rx2}
                  y2={ry2}
                  stroke="transparent"
                  strokeWidth="12"
                  style={{ cursor: isInteractive ? 'pointer' : 'default' }}
                  onPointerDown={handleShapeClick}
                  onMouseEnter={() => isInteractive && setHoveredShapeId(shape.id)}
                  onMouseLeave={() => isInteractive && setHoveredShapeId(null)}
                />
                <line
                  x1={x1}
                  y1={y1}
                  x2={rx2}
                  y2={ry2}
                  stroke={shapeStrokeColor}
                  strokeWidth={shapeStrokeWidth}
                  style={{ pointerEvents: 'none' }}
                />
              </g>
            );
          }

          if (shape.type === 'circle') {
            const dx = x2 - x1;
            const dy = y2 - y1;
            const r = Math.sqrt(dx * dx + dy * dy);

            return (
              <g key={shape.id}>
                <circle
                  cx={x1}
                  cy={y1}
                  r={r}
                  fill="transparent"
                  stroke="transparent"
                  strokeWidth="12"
                  style={{ cursor: isInteractive ? 'pointer' : 'default' }}
                  onPointerDown={handleShapeClick}
                  onMouseEnter={() => isInteractive && setHoveredShapeId(shape.id)}
                  onMouseLeave={() => isInteractive && setHoveredShapeId(null)}
                />
                <circle
                  cx={x1}
                  cy={y1}
                  r={r}
                  fill="none"
                  stroke={shapeStrokeColor}
                  strokeWidth={shapeStrokeWidth}
                  style={{ pointerEvents: 'none' }}
                />
              </g>
            );
          }

          return null;
        })}

        {/* Trajectory / Locus trace path rendering */}
        {Object.entries(pointTraces || {}).map(([ptId, tracePoints]) => {
          if (tracePoints.length < 2) return null;
          const pointsStr = tracePoints
            .map((p) => `${toScreenX(p.x, width)},${toScreenY(p.y, height)}`)
            .join(' ');
          
          const pt = points.find(p => p.id === ptId);
          const color = pt ? pt.color : '#1890ff';

          return (
            <polyline
              key={`trace-${ptId}`}
              points={pointsStr}
              fill="none"
              stroke={color}
              strokeWidth="1.5"
              strokeDasharray="3 3"
              opacity="0.5"
              style={{ pointerEvents: 'none' }}
            />
          );
        })}

        {/* Points Rendering */}
        {points.map((p) => {
          const sX = toScreenX(p.x, width);
          const sY = toScreenY(p.y, height);
          const isSelectedInTemp = tempPoints.includes(p.id);
          const isHovered = hoveredPointId === p.id;

          // Distinct color and style for dependent vs free points
          const isFree = !p.type || p.type === 'point_free';
          const ptColor = isSelectedInTemp ? 'var(--text-accent)' : p.color;
          const ptRadius = isHovered ? (isFree ? 6 : 5.5) : (isFree ? 5 : 4.5);
          const ptFill = isFree ? ptColor : 'transparent';
          const ptStroke = isFree ? '#fff' : ptColor;
          const ptStrokeWidth = isFree ? 1.5 : (isHovered ? 2.5 : 2);

          return (
            <g key={p.id}>
              <circle
                cx={sX}
                cy={sY}
                r="12"
                fill="transparent"
                style={{ cursor: isFree && activeTool === 'select' ? 'grab' : 'crosshair' }}
              />
              <circle
                cx={sX}
                cy={sY}
                r={ptRadius}
                fill={ptFill}
                stroke={ptStroke}
                strokeWidth={ptStrokeWidth}
                style={{ cursor: isFree && activeTool === 'select' ? 'grab' : 'crosshair' }}
              />
              <text
                x={sX + 8}
                y={sY - 8}
                fill="#fff"
                fontSize="11"
                fontFamily="var(--font-mono)"
                fontWeight="bold"
                style={{ pointerEvents: 'none', userSelect: 'none' }}
              >
                {p.label}
              </text>
            </g>
          );
        })}

        {/* Temporary preview line/circle while creating shape */}
        {tempPoints.length === 1 && hoverCoord && (
          (() => {
            const firstPoint = pointsMap.get(tempPoints[0]);
            if (!firstPoint) return null;

            const x1 = toScreenX(firstPoint.x, width);
            const y1 = toScreenY(firstPoint.y, height);
            const x2 = hoverCoord.sX;
            const y2 = hoverCoord.sY;

            if (['segment', 'vector', 'line', 'ray', 'midpoint'].includes(activeTool)) {
              return (
                <line
                  x1={x1}
                  y1={y1}
                  x2={x2}
                  y2={y2}
                  stroke="rgba(255, 255, 255, 0.4)"
                  strokeWidth="1.5"
                  strokeDasharray="4 4"
                  markerEnd={activeTool === 'vector' ? 'url(#vector-arrow)' : undefined}
                />
              );
            }

            if (activeTool === 'circle') {
              const dx = x2 - x1;
              const dy = y2 - y1;
              const r = Math.sqrt(dx * dx + dy * dy);
              return (
                <circle
                  cx={x1}
                  cy={y1}
                  r={r}
                  fill="none"
                  stroke="rgba(255, 255, 255, 0.4)"
                  strokeWidth="1.5"
                  strokeDasharray="4 4"
                />
              );
            }

            return null;
          })()
        )}

        {/* Temporary preview for polygon */}
        {activeTool === 'polygon' && tempPoints.length >= 1 && hoverCoord && (
          (() => {
            const polyPts = tempPoints.map(id => pointsMap.get(id)).filter(Boolean) as GeometryPoint[];
            if (polyPts.length === 0) return null;

            const screenPts = polyPts.map(p => ({ x: toScreenX(p.x, width), y: toScreenY(p.y, height) }));
            const first = screenPts[0];
            const last = screenPts[screenPts.length - 1];

            // Build path for solid segments
            let solidPath = `M ${first.x} ${first.y}`;
            for (let i = 1; i < screenPts.length; i++) {
              solidPath += ` L ${screenPts[i].x} ${screenPts[i].y}`;
            }

            return (
              <g>
                {/* Established segments */}
                {screenPts.length > 1 && (
                  <path
                    d={solidPath}
                    fill="none"
                    stroke="rgba(255, 255, 255, 0.6)"
                    strokeWidth="1.5"
                  />
                )}
                {/* Dashed lines connecting to hover cursor */}
                <line
                  x1={last.x}
                  y1={last.y}
                  x2={hoverCoord.sX}
                  y2={hoverCoord.sY}
                  stroke="rgba(255, 255, 255, 0.4)"
                  strokeWidth="1.5"
                  strokeDasharray="4 4"
                />
                <line
                  x1={hoverCoord.sX}
                  y1={hoverCoord.sY}
                  x2={first.x}
                  y2={first.y}
                  stroke="rgba(255, 255, 255, 0.4)"
                  strokeWidth="1.5"
                  strokeDasharray="4 4"
                />
              </g>
            );
          })()
        )}
      </svg>

      {/* Grid coordinates HUD */}
      {hoverCoord && (
        <div
          style={{
            position: 'absolute',
            bottom: '10px',
            left: '10px',
            background: 'rgba(0,0,0,0.75)',
            border: '1px solid rgba(255,255,255,0.1)',
            color: 'rgba(255,255,255,0.6)',
            padding: '2px 8px',
            borderRadius: '4px',
            fontSize: '0.75rem',
            fontFamily: 'var(--font-mono)',
            pointerEvents: 'none',
          }}
        >
          {`x: ${hoverCoord.x.toFixed(2)}, y: ${hoverCoord.y.toFixed(2)}`}
        </div>
      )}

      {/* Home view reset */}
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
