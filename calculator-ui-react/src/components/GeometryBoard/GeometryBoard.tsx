import React, { useState, useMemo, useEffect, useCallback } from 'react';
import { useUndoRedo } from '../../hooks/useUndoRedo';
import { useViewport } from '../GraphSandbox/useViewport';
import { ToolPalette, GeometryTool } from './ToolPalette';
import { GeometryCanvas, GeometryPoint, GeometryShape } from './GeometryCanvas';
import {
  intersectLines,
  intersectLineCircle,
  intersectCircles,
  calculateAngle,
  calculatePolygonArea,
  Point as MathPoint,
} from './geometrySolvers';

export const GeometryBoard: React.FC = () => {
  const {
    viewport,
    pan,
    zoom,
    resetViewport,
    toScreenX,
    toScreenY,
    toMathX,
    toMathY,
  } = useViewport({ xMin: -10, xMax: 10, yMin: -10, yMax: 10 });

  const [activeTool, setActiveTool] = useState<GeometryTool>('select');
  const [snapEnabled, setSnapEnabled] = useState(true);
  
  // Point drag trajectory histories
  const [pointTraces, setPointTraces] = useState<Record<string, Array<{ x: number; y: number }>>>({});

  // Main board state history tracking
  const {
    state: boardState,
    set: setBoardState,
    undo,
    redo,
    canUndo,
    canRedo,
    reset: resetBoardState,
  } = useUndoRedo({ points: [] as GeometryPoint[], shapes: [] as GeometryShape[] });

  const points = boardState.points;
  const shapes = boardState.shapes;

  const setPoints = (updater: GeometryPoint[] | ((prev: GeometryPoint[]) => GeometryPoint[])) => {
    setBoardState((prev) => {
      const nextPoints = typeof updater === 'function' ? (updater as any)(prev.points) : updater;
      return { ...prev, points: nextPoints };
    });
  };

  const setShapes = (updater: GeometryShape[] | ((prev: GeometryShape[]) => GeometryShape[])) => {
    setBoardState((prev) => {
      const nextShapes = typeof updater === 'function' ? (updater as any)(prev.shapes) : updater;
      return { ...prev, shapes: nextShapes };
    });
  };

  const [tempPoints, setTempPoints] = useState<string[]>([]); // active click buffer for constructing elements
  const [tempShapes, setTempShapes] = useState<string[]>([]); // active buffer for Intersect tool

  // Generate next alphabetical label (A, B, ..., Z, A1, B1, ...)
  const getNextPointLabel = (pts: GeometryPoint[]) => {
    const labels = new Set(pts.map((p) => p.label));
    const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';

    for (let i = 0; i < alphabet.length; i++) {
      if (!labels.has(alphabet[i])) {
        return alphabet[i];
      }
    }

    let suffix = 1;
    while (true) {
      for (let i = 0; i < alphabet.length; i++) {
        const label = `${alphabet[i]}${suffix}`;
        if (!labels.has(label)) {
          return label;
        }
      }
      suffix++;
    }
  };

  // Add a free point
  const handleAddPoint = (x: number, y: number) => {
    const id = crypto.randomUUID();
    const label = getNextPointLabel(points);
    const newPoint: GeometryPoint = {
      id,
      x: parseFloat(x.toFixed(4)),
      y: parseFloat(y.toFixed(4)),
      label,
      color: '#1890ff',
      type: 'point_free',
    };
    setPoints((prev) => [...prev, newPoint]);
    return id;
  };

  // Drag a point (only allowed for free points)
  const handleDragPoint = (id: string, x: number, y: number) => {
    setPoints((prev) =>
      prev.map((p) =>
        p.id === id && p.type === 'point_free'
          ? { ...p, x: parseFloat(x.toFixed(4)), y: parseFloat(y.toFixed(4)) }
          : p
      )
    );
  };

  // Recursive Resolver for dependent element coordinates
  const resolvedPoints = useMemo(() => {
    const cache = new Map<string, MathPoint>();
    const pointsMap = new Map(points.map((p) => [p.id, p]));
    const shapesMap = new Map(shapes.map((s) => [s.id, s]));

    const resolvePoint = (id: string, depth = 0): MathPoint | null => {
      if (depth > 50) {
        console.warn('Circular dependency detected for point:', id);
        return null;
      }

      if (cache.has(id)) return cache.get(id)!;

      const p = pointsMap.get(id);
      if (!p) return null;

      if (p.type === 'point_free' || !p.type) {
        const coords = { x: p.x, y: p.y };
        cache.set(id, coords);
        return coords;
      }

      if (p.type === 'point_midpoint') {
        const [p1Id, p2Id] = p.dependencies || [];
        const c1 = resolvePoint(p1Id, depth + 1);
        const c2 = resolvePoint(p2Id, depth + 1);
        if (!c1 || !c2) return null;
        
        const coords = {
          x: parseFloat(((c1.x + c2.x) / 2).toFixed(4)),
          y: parseFloat(((c1.y + c2.y) / 2).toFixed(4)),
        };
        cache.set(id, coords);
        return coords;
      }

      if (p.type === 'point_intersection') {
        const [s1Id, s2Id] = p.dependencies || [];
        const s1 = shapesMap.get(s1Id);
        const s2 = shapesMap.get(s2Id);
        if (!s1 || !s2) return null;

        const g1 = getResolvedShapeData(s1, depth + 1);
        const g2 = getResolvedShapeData(s2, depth + 1);
        if (!g1 || !g2) return null;

        const intersections = solveIntersections(g1, g2);
        const idx = (p as any).intersectIndex || 0;
        const pt = intersections[idx];
        if (!pt) return null;

        const coords = {
          x: parseFloat(pt.x.toFixed(4)),
          y: parseFloat(pt.y.toFixed(4)),
        };
        cache.set(id, coords);
        return coords;
      }

      if (p.type === 'point_reflected') {
        const [pId, shapeId] = p.dependencies || [];
        const ptCoords = resolvePoint(pId, depth + 1);
        const s = shapesMap.get(shapeId);
        if (!ptCoords || !s) return null;

        const sPts = s.dependencies.map((depId) => resolvePoint(depId, depth + 1)).filter(Boolean) as MathPoint[];
        if (sPts.length < 2) return null;
        const [p1, p2] = sPts;

        // Line equation Ax + By + C = 0
        const A = p2.y - p1.y;
        const B = p1.x - p2.x;
        const C = p2.x * p1.y - p1.x * p2.y;
        const d = A * A + B * B;
        if (Math.abs(d) < 1e-10) return null;

        const coords = {
          x: parseFloat((ptCoords.x - 2 * A * (A * ptCoords.x + B * ptCoords.y + C) / d).toFixed(4)),
          y: parseFloat((ptCoords.y - 2 * B * (A * ptCoords.x + B * ptCoords.y + C) / d).toFixed(4)),
        };
        cache.set(id, coords);
        return coords;
      }

      if (p.type === 'point_rotated') {
        const [pId, centerId] = p.dependencies || [];
        const ptCoords = resolvePoint(pId, depth + 1);
        const centerCoords = resolvePoint(centerId, depth + 1);
        if (!ptCoords || !centerCoords) return null;

        // Rotate 90 degrees CCW
        const coords = {
          x: parseFloat((centerCoords.x - (ptCoords.y - centerCoords.y)).toFixed(4)),
          y: parseFloat((centerCoords.y + (ptCoords.x - centerCoords.x)).toFixed(4)),
        };
        cache.set(id, coords);
        return coords;
      }

      if (p.type === 'point_translated') {
        const [pId, vectorId] = p.dependencies || [];
        const ptCoords = resolvePoint(pId, depth + 1);
        const s = shapesMap.get(vectorId);
        if (!ptCoords || !s) return null;

        const sPts = s.dependencies.map((depId) => resolvePoint(depId, depth + 1)).filter(Boolean) as MathPoint[];
        if (sPts.length < 2) return null;
        const [p1, p2] = sPts;

        const coords = {
          x: parseFloat((ptCoords.x + (p2.x - p1.x)).toFixed(4)),
          y: parseFloat((ptCoords.y + (p2.y - p1.y)).toFixed(4)),
        };
        cache.set(id, coords);
        return coords;
      }

      if (p.type === 'point_dilated') {
        const [pId, centerId] = p.dependencies || [];
        const ptCoords = resolvePoint(pId, depth + 1);
        const centerCoords = resolvePoint(centerId, depth + 1);
        if (!ptCoords || !centerCoords) return null;

        // Dilate by 2x scale
        const coords = {
          x: parseFloat((centerCoords.x + 2 * (ptCoords.x - centerCoords.x)).toFixed(4)),
          y: parseFloat((centerCoords.y + 2 * (ptCoords.y - centerCoords.y)).toFixed(4)),
        };
        cache.set(id, coords);
        return coords;
      }

      return null;
    };

    const getResolvedShapeData = (s: GeometryShape, depth = 0): any => {
      const pts = s.dependencies.map((id) => resolvePoint(id, depth)).filter(Boolean) as MathPoint[];
      if (pts.length < 2) return null;
      const [p1, p2] = pts;

      if (s.type === 'circle') {
        const r = Math.sqrt((p2.x - p1.x) ** 2 + (p2.y - p1.y) ** 2);
        return { type: 'circle', cx: p1.x, cy: p1.y, r };
      }

      return { type: s.type, x1: p1.x, y1: p1.y, x2: p2.x, y2: p2.y };
    };

    const solveIntersections = (g1: any, g2: any): MathPoint[] => {
      if (g1.type === 'circle' && g2.type === 'circle') {
        return intersectCircles(g1, g2);
      }
      if (g1.type === 'circle') {
        return intersectLineCircle(g2, g1);
      }
      if (g2.type === 'circle') {
        return intersectLineCircle(g1, g2);
      }
      const pt = intersectLines(g1, g2);
      return pt ? [pt] : [];
    };

    // Construct fully resolved point list
    const list: GeometryPoint[] = [];
    points.forEach((p) => {
      const coords = resolvePoint(p.id);
      if (coords) {
        list.push({
          ...p,
          x: coords.x,
          y: coords.y,
        });
      } else {
        list.push({ ...p, x: NaN, y: NaN });
      }
    });

    return list;
  }, [points, shapes]);

  // Point trajectory traces logger
  useEffect(() => {
    setPointTraces((prev) => {
      const next = { ...prev };
      let changed = false;

      // Remove trace records of deleted points
      const activeIds = new Set(resolvedPoints.map((p) => p.id));
      Object.keys(next).forEach((id) => {
        if (!activeIds.has(id)) {
          delete next[id];
          changed = true;
        }
      });

      resolvedPoints.forEach((p) => {
        if (isNaN(p.x) || isNaN(p.y)) return;
        const history = next[p.id] || [];
        const last = history[history.length - 1];

        // Append coordinates if they changed significantly
        if (!last || Math.abs(last.x - p.x) > 1e-4 || Math.abs(last.y - p.y) > 1e-4) {
          next[p.id] = [...history, { x: p.x, y: p.y }].slice(-40);
          changed = true;
        }
      });

      return changed ? next : prev;
    });
  }, [resolvedPoints]);

  // Compute resolved shape parameters and live measurements
  const resolvedShapes = useMemo(() => {
    const ptsMap = new Map(resolvedPoints.map((p) => [p.id, p]));

    return shapes.map((s) => {
      const pts = s.dependencies.map((id) => ptsMap.get(id)).filter(Boolean) as GeometryPoint[];
      if (pts.length < 2) return s;

      const [p1, p2] = pts;

      if (isNaN(p1.x) || isNaN(p1.y) || isNaN(p2.x) || isNaN(p2.y)) {
        return { ...s, value: undefined };
      }

      if (s.type === 'segment') {
        const len = Math.sqrt((p2.x - p1.x) ** 2 + (p2.y - p1.y) ** 2);
        return { ...s, value: len };
      }

      if (s.type === 'circle') {
        const r = Math.sqrt((p2.x - p1.x) ** 2 + (p2.y - p1.y) ** 2);
        return { ...s, value: r }; // radius
      }

      if (s.type === 'angle') {
        const p3 = pts[2];
        if (!p3 || isNaN(p3.x) || isNaN(p3.y)) return s;
        const ang = calculateAngle(p1, p2, p3);
        return { ...s, value: ang.deg };
      }

      if (s.type === 'ellipse') {
        const p3 = pts[2];
        if (!p3 || isNaN(p3.x) || isNaN(p3.y)) return s;

        const dist = (ptA: GeometryPoint, ptB: GeometryPoint) =>
          Math.sqrt((ptB.x - ptA.x) ** 2 + (ptB.y - ptA.y) ** 2);
        const c = dist(p1, p2) / 2;
        const a = (dist(p3, p1) + dist(p3, p2)) / 2;
        if (a <= c) return s;
        const b = Math.sqrt(a * a - c * c);
        const area = Math.PI * a * b;
        return { ...s, value: area }; // store ellipse area
      }

      if (s.type === 'polygon') {
        const area = calculatePolygonArea(pts);
        return { ...s, value: area }; // store polygon area
      }

      return s;
    });
  }, [shapes, resolvedPoints]);

  // Add dependent shapes / lines / transformed points
  const handleAddShape = (type: string, dependencies: string[]) => {
    const id = crypto.randomUUID();
    const ptsMap = new Map(points.map((p) => [p.id, p]));

    if (type === 'midpoint') {
      const label = getNextPointLabel(points);
      const newPoint: GeometryPoint = {
        id,
        x: 0,
        y: 0,
        label,
        color: '#52c41a', // Green for midpoints
        type: 'point_midpoint',
        dependencies,
      };
      setPoints((prev) => [...prev, newPoint]);
      setTempPoints([]);
      return;
    }

    if (['reflect', 'rotate', 'translate', 'dilate'].includes(type)) {
      const label = getNextPointLabel(points);
      const newPoint: GeometryPoint = {
        id,
        x: 0,
        y: 0,
        label,
        color: '#fa8c16', // Orange for transformation dependencies
        type: `point_${type}` as any,
        dependencies,
      };
      setPoints((prev) => [...prev, newPoint]);
      setTempPoints([]);
      return;
    }

    const p1 = ptsMap.get(dependencies[0]);
    const p2 = ptsMap.get(dependencies[1]);
    let label = `${type}_${shapes.length + 1}`;

    if (type === 'ellipse' && dependencies.length === 3) {
      const p3 = ptsMap.get(dependencies[2]);
      if (p1 && p2 && p3) {
        label = `Ellipse(${p1.label}, ${p2.label}, ${p3.label})`;
      }
      const newShape: GeometryShape = {
        id,
        type: 'ellipse' as any,
        label,
        color: '#722ed1', // Purple for ellipse
        dependencies,
      };
      setShapes((prev) => [...prev, newShape]);
      setTempPoints([]);
      return;
    }

    if (type === 'polygon') {
      const pLabels = dependencies.map((pid) => ptsMap.get(pid)?.label || '?');
      label = `Poly(${pLabels.join('')})`;
      const newShape: GeometryShape = {
        id,
        type: 'polygon' as any,
        label,
        color: '#13c2c2', // Cyan for polygons
        dependencies,
      };
      setShapes((prev) => [...prev, newShape]);
      setTempPoints([]);
      return;
    }

    if (type === 'angle' && dependencies.length === 3) {
      const p3 = ptsMap.get(dependencies[2]);
      if (p1 && p2 && p3) {
        label = `∠${p1.label}${p2.label}${p3.label}`;
      }
    } else if (p1 && p2) {
      label = `${type}${p1.label}${p2.label}`;
    }

    const newShape: GeometryShape = {
      id,
      type: type as any,
      label,
      color: type === 'angle' ? '#fa8c16' : '#ff4d4f',
      dependencies,
    };
    setShapes((prev) => [...prev, newShape]);
    setTempPoints([]);
  };

  // Add intersection points from two shapes
  const handleSelectShapeForIntersection = (shapeId: string) => {
    const nextTempShapes = [...tempShapes, shapeId];

    if (nextTempShapes.length === 2) {
      const [s1Id, s2Id] = nextTempShapes;
      if (s1Id === s2Id) {
        setTempShapes([shapeId]);
        return;
      }

      // Check how many intersections they have to construct points accordingly
      const s1 = shapes.find(s => s.id === s1Id);
      const s2 = shapes.find(s => s.id === s2Id);
      if (!s1 || !s2) {
        setTempShapes([]);
        return;
      }

      const label1 = getNextPointLabel(points);
      const id1 = crypto.randomUUID();
      const pt1: GeometryPoint = {
        id: id1,
        x: 0,
        y: 0,
        label: label1,
        color: '#fa8c16',
        type: 'point_intersection',
        dependencies: [s1Id, s2Id],
      };
      (pt1 as any).intersectIndex = 0;

      const pointsToAdd = [pt1];

      // Circles can intersect in up to 2 points
      if (s1.type === 'circle' || s2.type === 'circle') {
        const label2 = getNextPointLabel([...points, pt1]);
        const id2 = crypto.randomUUID();
        const pt2: GeometryPoint = {
          id: id2,
          x: 0,
          y: 0,
          label: label2,
          color: '#fa8c16',
          type: 'point_intersection',
          dependencies: [s1Id, s2Id],
        };
        (pt2 as any).intersectIndex = 1;
        pointsToAdd.push(pt2);
      }

      setPoints((prev) => [...prev, ...pointsToAdd]);
      setTempShapes([]);
    } else {
      setTempShapes(nextTempShapes);
    }
  };

  const [editingPointId, setEditingPointId] = useState<string | null>(null);
  const [editingLabelValue, setEditingLabelValue] = useState<string>('');

  const handleStartRename = (id: string, currentLabel: string) => {
    setEditingPointId(id);
    setEditingLabelValue(currentLabel);
  };

  const handleSavePointRename = (id: string) => {
    const cleanLabel = editingLabelValue.trim();
    if (cleanLabel !== '') {
      setPoints((prev) =>
        prev.map((p) => (p.id === id ? { ...p, label: cleanLabel } : p))
      );
    }
    setEditingPointId(null);
  };

  // Deletion cascade
  const handleDeletePoint = (id: string) => {
    setPoints((prev) => prev.filter((p) => p.id !== id));
    setShapes((prev) => prev.filter((s) => !s.dependencies.includes(id)));
    const deletedShapeIds = shapes
      .filter((s) => s.dependencies.includes(id))
      .map((s) => s.id);
    
    if (deletedShapeIds.length > 0) {
      setPoints((prev) =>
        prev.filter(
          (p) =>
            !p.dependencies ||
            !p.dependencies.some((dep) => deletedShapeIds.includes(dep))
        )
      );
    }

    setPoints((prev) =>
      prev.filter((p) => !p.dependencies || !p.dependencies.includes(id))
    );

    setTempPoints([]);
    setTempShapes([]);
  };

  const handleDeleteShape = (id: string) => {
    setShapes((prev) => prev.filter((s) => s.id !== id));
    setPoints((prev) =>
      prev.filter(
        (p) => !p.dependencies || !p.dependencies.includes(id)
      )
    );
    setTempShapes([]);
  };

  const handleToolChange = useCallback((tool: GeometryTool) => {
    setActiveTool(tool);
    setTempPoints([]);
    setTempShapes([]);
  }, []);

  const handleClearAll = () => {
    resetBoardState({ points: [], shapes: [] });
    setTempPoints([]);
    setTempShapes([]);
  };

  const exportSvg = () => {
    const container = document.querySelector('.geometry-board-container');
    const svg = container?.querySelector('svg');
    if (!svg) return;
    const serializer = new XMLSerializer();
    const svgStr = serializer.serializeToString(svg);
    const blob = new Blob([svgStr], { type: 'image/svg+xml' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'geometry_board.svg';
    link.click();
    URL.revokeObjectURL(url);
  };

  const exportPng = () => {
    const container = document.querySelector('.geometry-board-container');
    const svg = container?.querySelector('svg');
    if (!svg) return;
    const serializer = new XMLSerializer();
    const svgStr = serializer.serializeToString(svg);
    const svgWidth = svg.clientWidth || 600;
    const svgHeight = svg.clientHeight || 400;

    const img = new Image();
    img.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svgStr);
    img.onload = () => {
      const canvas = document.createElement('canvas');
      canvas.width = svgWidth;
      canvas.height = svgHeight;
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.fillStyle = '#141414';
        ctx.fillRect(0, 0, svgWidth, svgHeight);
        ctx.drawImage(img, 0, 0);
        const pngUrl = canvas.toDataURL('image/png');
        const link = document.createElement('a');
        link.href = pngUrl;
        link.download = 'geometry_board.png';
        link.click();
      }
    };
  };

  const handleSaveSession = () => {
    const sessionData = {
      points,
      shapes,
    };
    const blob = new Blob([JSON.stringify(sessionData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'geometry_session.json';
    link.click();
    URL.revokeObjectURL(url);
  };

  const handleLoadSession = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (evt) => {
      try {
        const data = JSON.parse(evt.target?.result as string);
        if (data.points && data.shapes) {
          resetBoardState({ points: data.points, shapes: data.shapes });
        }
      } catch (err: any) {
        alert('Invalid session JSON: ' + err.message);
      }
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  // Keyboard shortcuts and Ctrl+Z / Ctrl+Y
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement;
      if (target.tagName === 'INPUT' || target.tagName === 'SELECT' || target.tagName === 'TEXTAREA') {
        return;
      }

      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') {
        e.preventDefault();
        if (e.shiftKey) {
          redo();
        } else {
          undo();
        }
      } else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'y') {
        e.preventDefault();
        redo();
      }

      const key = e.key.toLowerCase();
      if (key === 's') handleToolChange('select');
      else if (key === 'p') handleToolChange('point');
      else if (key === 'l') handleToolChange('line');
      else if (key === 'g') handleToolChange('segment');
      else if (key === 'r') handleToolChange('ray');
      else if (key === 'v') handleToolChange('vector');
      else if (key === 'c') handleToolChange('circle');
      else if (key === 'm') handleToolChange('midpoint');
      else if (key === 'i') handleToolChange('intersect');
      else if (key === 'a') handleToolChange('angle');
      else if (key === 'h') handleToolChange('reflect');
      else if (key === 'o') handleToolChange('rotate');
      else if (key === 't') handleToolChange('translate');
      else if (key === 'd') handleToolChange('dilate');
      else if (key === 'e') handleToolChange('ellipse');
      else if (key === 'y') handleToolChange('polygon');
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [undo, redo, handleToolChange]);

  const ptsMap = useMemo(() => {
    return new Map(resolvedPoints.map((p) => [p.id, p]));
  }, [resolvedPoints]);

  const getPointTypeLabel = (type?: string) => {
    if (!type) return '';
    const labels: Record<string, string> = {
      point_midpoint: ' (midpoint)',
      point_intersection: ' (intersect)',
      point_reflected: ' (reflect)',
      point_rotated: ' (rotate)',
      point_translated: ' (translate)',
      point_dilated: ' (dilate)',
    };
    return labels[type] || '';
  };

  return (
    <div
      className="geometry-board-container"
      style={{
        display: 'flex',
        flexDirection: 'column',
        width: '100%',
        marginTop: '10px',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
          <ToolPalette activeTool={activeTool} onChangeTool={handleToolChange} />
          
          {/* Undo / Redo controls */}
          <div style={{ display: 'flex', gap: '4px', marginBottom: '10px' }}>
            <button
              type="button"
              disabled={!canUndo}
              onClick={undo}
              style={{
                padding: '6px 12px',
                fontSize: '0.85rem',
                background: canUndo ? 'rgba(255, 255, 255, 0.05)' : 'rgba(255, 255, 255, 0.02)',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                color: canUndo ? '#fff' : 'rgba(255, 255, 255, 0.25)',
                borderRadius: '6px',
                cursor: canUndo ? 'pointer' : 'default',
              }}
              title="Undo (Ctrl+Z)"
            >
              ↶ Undo
            </button>
            <button
              type="button"
              disabled={!canRedo}
              onClick={redo}
              style={{
                padding: '6px 12px',
                fontSize: '0.85rem',
                background: canRedo ? 'rgba(255, 255, 255, 0.05)' : 'rgba(255, 255, 255, 0.02)',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                color: canRedo ? '#fff' : 'rgba(255, 255, 255, 0.25)',
                borderRadius: '6px',
                cursor: canRedo ? 'pointer' : 'default',
              }}
              title="Redo (Ctrl+Y)"
            >
              ↷ Redo
            </button>
            <button
              type="button"
              onClick={() => setSnapEnabled((prev) => !prev)}
              style={{
                padding: '6px 12px',
                fontSize: '0.85rem',
                background: snapEnabled ? 'rgba(82, 196, 26, 0.15)' : 'rgba(255, 255, 255, 0.05)',
                border: snapEnabled ? '1px solid rgba(82, 196, 26, 0.3)' : '1px solid rgba(255, 255, 255, 0.1)',
                color: snapEnabled ? '#52c41a' : '#fff',
                borderRadius: '6px',
                cursor: 'pointer',
              }}
              title="Toggle snap-to-grid proximity"
            >
              {snapEnabled ? '🧲 Snap: ON' : '🧲 Snap: OFF'}
            </button>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '6px', marginBottom: '10px' }}>
          <button
            type="button"
            onClick={handleSaveSession}
            style={{
              padding: '6px 12px',
              fontSize: '0.85rem',
              background: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              color: '#fff',
              borderRadius: '6px',
              cursor: 'pointer',
            }}
          >
            Save Session
          </button>
          <button
            type="button"
            onClick={() => document.getElementById('load-geometry-session-input')?.click()}
            style={{
              padding: '6px 12px',
              fontSize: '0.85rem',
              background: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              color: '#fff',
              borderRadius: '6px',
              cursor: 'pointer',
            }}
          >
            Load Session
          </button>
          <input
            type="file"
            id="load-geometry-session-input"
            style={{ display: 'none' }}
            accept=".json"
            onChange={handleLoadSession}
          />

          {/* Export options */}
          <button
            type="button"
            onClick={exportSvg}
            style={{
              padding: '6px 12px',
              fontSize: '0.85rem',
              background: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              color: '#fff',
              borderRadius: '6px',
              cursor: 'pointer',
            }}
          >
            Export SVG
          </button>
          <button
            type="button"
            onClick={exportPng}
            style={{
              padding: '6px 12px',
              fontSize: '0.85rem',
              background: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              color: '#fff',
              borderRadius: '6px',
              cursor: 'pointer',
            }}
          >
            Export PNG
          </button>
          <button
            type="button"
            onClick={handleClearAll}
            style={{
              padding: '6px 12px',
              fontSize: '0.85rem',
              background: 'rgba(255, 77, 79, 0.1)',
              border: '1px solid rgba(255, 77, 79, 0.3)',
              color: '#ff4d4f',
              borderRadius: '6px',
              cursor: 'pointer',
            }}
          >
            Clear Board
          </button>
        </div>
      </div>

      <div
        style={{
          display: 'flex',
          gap: '20px',
          height: '500px',
          width: '100%',
        }}
      >
        {/* Sidebar algebraic display (Algebra view) */}
        <div
          className="geometry-sidebar"
          style={{
            width: '280px',
            background: 'rgba(255, 255, 255, 0.02)',
            border: '1px solid rgba(255, 255, 255, 0.08)',
            borderRadius: '8px',
            padding: '16px',
            display: 'flex',
            flexDirection: 'column',
            gap: '16px',
            overflowY: 'auto',
          }}
        >
          {/* Points list */}
          <div>
            <h3 style={{ margin: '0 0 8px 0', fontSize: '1rem', color: 'var(--text-accent)' }}>Points</h3>
            {resolvedPoints.length === 0 ? (
              <span style={{ fontSize: '0.8rem', color: 'rgba(255,255,255,0.3)', fontStyle: 'italic' }}>
                No points placed yet.
              </span>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                {resolvedPoints.map((p) => {
                  const typeLabel = getPointTypeLabel(p.type);
                  
                  return (
                    <div
                      key={p.id}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        background: 'rgba(255, 255, 255, 0.03)',
                        border: '1px solid rgba(255, 255, 255, 0.05)',
                        borderRadius: '4px',
                        padding: '4px 8px',
                        fontSize: '0.85rem',
                        fontFamily: 'var(--font-mono)',
                      }}
                    >
                      <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', display: 'flex', alignItems: 'center', gap: '4px', flexWrap: 'wrap' }}>
                        {editingPointId === p.id ? (
                          <input
                            type="text"
                            value={editingLabelValue}
                            onChange={(e) => setEditingLabelValue(e.target.value)}
                            onBlur={() => handleSavePointRename(p.id)}
                            onKeyDown={(e) => e.key === 'Enter' && handleSavePointRename(p.id)}
                            style={{
                              width: '50px',
                              background: 'rgba(0,0,0,0.5)',
                              color: '#fff',
                              border: '1px solid var(--text-accent)',
                              borderRadius: '3px',
                              padding: '0 2px',
                              fontSize: '0.8rem',
                              fontFamily: 'var(--font-mono)',
                            }}
                            autoFocus
                          />
                        ) : (
                          <>
                            <strong
                              style={{ color: p.color, cursor: 'pointer' }}
                              onDoubleClick={() => handleStartRename(p.id, p.label)}
                              title="Double click to rename"
                            >
                              {p.label}
                            </strong>
                            <button
                              type="button"
                              onClick={() => handleStartRename(p.id, p.label)}
                              style={{
                                background: 'none',
                                border: 'none',
                                color: 'rgba(255,255,255,0.3)',
                                cursor: 'pointer',
                                padding: '0 2px',
                                fontSize: '0.75rem',
                              }}
                              title="Rename point"
                            >
                              ✎
                            </button>
                          </>
                        )}
                        {isNaN(p.x) 
                          ? ' = undefined' 
                          : ` = (${p.x.toFixed(2)}, ${p.y.toFixed(2)})`}
                        <span style={{ fontSize: '0.7rem', color: 'rgba(255,255,255,0.35)' }}>{typeLabel}</span>
                      </span>
                      <button
                        type="button"
                        onClick={() => handleDeletePoint(p.id)}
                        style={{
                          background: 'none',
                          border: 'none',
                          color: 'rgba(255, 77, 79, 0.7)',
                          cursor: 'pointer',
                          fontSize: '0.75rem',
                        }}
                      >
                        ✕
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Constructed Shapes list */}
          <div>
            <h3 style={{ margin: '0 0 8px 0', fontSize: '1rem', color: 'var(--text-accent)' }}>Dependent Shapes</h3>
            {resolvedShapes.length === 0 ? (
              <span style={{ fontSize: '0.8rem', color: 'rgba(255,255,255,0.3)', fontStyle: 'italic' }}>
                No shapes constructed yet.
              </span>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                {resolvedShapes.map((s) => {
                  const pNames = s.dependencies.map((pid) => ptsMap.get(pid)?.label || '?');
                  
                  let desc = `${s.type.charAt(0).toUpperCase() + s.type.slice(1)}(${pNames.join(', ')})`;
                  
                  if (s.type === 'ellipse') {
                    desc = `Ellipse(${pNames[0]}, ${pNames[1]}, ${pNames[2]})`;
                    if (s.value !== undefined) {
                      desc += ` (area=${s.value.toFixed(1)})`;
                    }
                  } else if (s.type === 'polygon') {
                    desc = `Polygon(${pNames.join(', ')})`;
                    if (s.value !== undefined) {
                      desc += ` (area=${s.value.toFixed(2)})`;
                    }
                  } else if (s.value !== undefined) {
                    if (s.type === 'segment') {
                      desc += ` = ${s.value.toFixed(2)}`;
                    } else if (s.type === 'circle') {
                      const area = Math.PI * s.value * s.value;
                      desc += ` (r=${s.value.toFixed(2)}, area=${area.toFixed(1)})`;
                    } else if (s.type === 'angle') {
                      desc = `Angle(${pNames.join(', ')}) = ${s.value.toFixed(1)}°`;
                    }
                  }

                  return (
                    <div
                      key={s.id}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        background: 'rgba(255, 255, 255, 0.03)',
                        border: '1px solid rgba(255, 255, 255, 0.05)',
                        borderRadius: '4px',
                        padding: '4px 8px',
                        fontSize: '0.85rem',
                        fontFamily: 'var(--font-mono)',
                      }}
                    >
                      <span>
                        <strong style={{ color: s.color }}>{s.label}</strong>
                        {` = ${desc}`}
                      </span>
                      <button
                        type="button"
                        onClick={() => handleDeleteShape(s.id)}
                        style={{
                          background: 'none',
                          border: 'none',
                          color: 'rgba(255, 77, 79, 0.7)',
                          cursor: 'pointer',
                          fontSize: '0.75rem',
                        }}
                      >
                        ✕
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Keyboard Shortcuts Help Panel */}
          <details
            style={{
              borderTop: '1px solid rgba(255, 255, 255, 0.08)',
              paddingTop: '12px',
              marginTop: '8px',
              fontSize: '0.8rem',
              color: 'rgba(255,255,255,0.7)',
              cursor: 'pointer',
            }}
          >
            <summary style={{ fontWeight: 'bold', color: 'var(--text-accent)' }}>Keyboard Shortcuts</summary>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '4px',
                marginTop: '8px',
                fontSize: '0.75rem',
                fontFamily: 'var(--font-mono)',
                color: 'rgba(255,255,255,0.5)',
                cursor: 'default',
              }}
            >
              <div>Select: <strong>S</strong></div>
              <div>Point: <strong>P</strong></div>
              <div>Line: <strong>L</strong></div>
              <div>Segment: <strong>G</strong></div>
              <div>Ray: <strong>R</strong></div>
              <div>Vector: <strong>V</strong></div>
              <div>Circle: <strong>C</strong></div>
              <div>Midpoint: <strong>M</strong></div>
              <div>Intersect: <strong>I</strong></div>
              <div>Angle: <strong>A</strong></div>
              <div>Reflect: <strong>H</strong></div>
              <div>Rotate: <strong>O</strong></div>
              <div>Translate: <strong>T</strong></div>
              <div>Dilate: <strong>D</strong></div>
              <div>Ellipse: <strong>E</strong></div>
              <div>Polygon: <strong>Y</strong></div>
              <div>Undo: <strong>Ctrl+Z</strong></div>
              <div>Redo: <strong>Ctrl+Y</strong></div>
            </div>
          </details>
        </div>

        {/* Geometry Canvas container */}
        <div style={{ flex: 1, height: '100%' }}>
          <GeometryCanvas
            viewport={viewport}
            pan={pan}
            zoom={zoom}
            resetViewport={resetViewport}
            toScreenX={toScreenX}
            toScreenY={toScreenY}
            toMathX={toMathX}
            toMathY={toMathY}
            points={resolvedPoints}
            shapes={resolvedShapes}
            activeTool={activeTool}
            tempPoints={tempPoints}
            tempShapes={tempShapes}
            onAddPoint={handleAddPoint}
            onAddShape={handleAddShape}
            onDragPoint={handleDragPoint}
            onUpdateTempPoints={setTempPoints}
            onSelectShapeForIntersection={handleSelectShapeForIntersection}
            pointTraces={pointTraces}
            snapEnabled={snapEnabled}
          />
        </div>
      </div>
    </div>
  );
};
