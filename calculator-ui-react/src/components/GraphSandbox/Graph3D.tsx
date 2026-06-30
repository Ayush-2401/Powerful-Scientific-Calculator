import React, { useRef, useState, useEffect, useMemo } from 'react';
import * as math from 'mathjs';

interface Graph3DProps {
  expr: string;
  params: Record<string, number>;
}

interface Point3D {
  x: number;
  y: number;
  z: number;
}

interface ProjectedPoint {
  x: number;
  y: number;
  depth: number;
}

interface PolygonData {
  pts: [ProjectedPoint, ProjectedPoint, ProjectedPoint, ProjectedPoint];
  depth: number;
  zAvg: number;
}

export const Graph3D: React.FC<Graph3DProps> = ({ expr, params }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [dimensions, setDimensions] = useState({ width: 600, height: 400 });
  const [yaw, setYaw] = useState<number>(-0.6); // Rotation around Z-axis
  const [pitch, setPitch] = useState<number>(0.8); // Pitch around X-axis
  const [isDragging, setIsDragging] = useState(false);
  const dragStart = useRef({ x: 0, y: 0 });

  // Update canvas bounds
  useEffect(() => {
    if (!containerRef.current) return;
    const observer = new ResizeObserver((entries) => {
      for (let entry of entries) {
        setDimensions({
          width: Math.max(100, entry.contentRect.width),
          height: Math.max(100, entry.contentRect.height),
        });
      }
    });
    observer.observe(containerRef.current);
    return () => observer.disconnect();
  }, []);

  const { width, height } = dimensions;

  // Compile expression for evaluation
  const compiled = useMemo(() => {
    let cleanExpr = expr.trim();
    if (cleanExpr.includes('=')) {
      const parts = cleanExpr.split('=');
      cleanExpr = parts[1].trim();
    }
    try {
      return math.compile(cleanExpr);
    } catch (e) {
      return null;
    }
  }, [expr]);

  // Generate 3D grid values (26 x 26 grid)
  const gridResolution = 25;
  const range = 6.0; // from -3 to 3

  const gridData = useMemo(() => {
    if (!compiled) return null;

    const points: Point3D[][] = [];
    let minZ = Infinity;
    let maxZ = -Infinity;

    for (let i = 0; i <= gridResolution; i++) {
      points[i] = [];
      const x = -range / 2 + (i / gridResolution) * range;
      for (let j = 0; j <= gridResolution; j++) {
        const y = -range / 2 + (j / gridResolution) * range;
        try {
          const zVal = compiled.evaluate({ x, y, ...params });
          const z = typeof zVal === 'number' && !isNaN(zVal) ? zVal : 0;
          points[i][j] = { x, y, z };
          minZ = Math.min(minZ, z);
          maxZ = Math.max(maxZ, z);
        } catch (e) {
          points[i][j] = { x, y, z: 0 };
          minZ = Math.min(minZ, 0);
          maxZ = Math.max(maxZ, 0);
        }
      }
    }

    return { points, minZ, maxZ };
  }, [compiled, params]);

  // Orbit camera rotation controls
  const handlePointerDown = (e: React.PointerEvent) => {
    if (e.button !== 0) return;
    setIsDragging(true);
    dragStart.current = { x: e.clientX, y: e.clientY };
    e.currentTarget.setPointerCapture(e.pointerId);
  };

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!isDragging) return;
    const dx = e.clientX - dragStart.current.x;
    const dy = e.clientY - dragStart.current.y;
    setYaw((prev) => prev + dx * 0.007);
    setPitch((prev) => Math.max(-1.4, Math.min(1.4, prev + dy * 0.007)));
    dragStart.current = { x: e.clientX, y: e.clientY };
  };

  const handlePointerUp = (e: React.PointerEvent) => {
    setIsDragging(false);
    e.currentTarget.releasePointerCapture(e.pointerId);
  };

  // Build projected polygons
  const polygons = useMemo(() => {
    if (!gridData) return [];
    const { points } = gridData;

    const centerX = width / 2;
    const centerY = height / 2;
    const scale = Math.min(width, height) / 8; // scaling factor

    const cosYaw = Math.cos(yaw);
    const sinYaw = Math.sin(yaw);
    const cosPitch = Math.cos(pitch);
    const sinPitch = Math.sin(pitch);

    // Rotate and project a 3D point
    const project = (pt: Point3D): ProjectedPoint => {
      // 1. Yaw rotation (Z-axis)
      const x1 = pt.x * cosYaw - pt.y * sinYaw;
      const y1 = pt.x * sinYaw + pt.y * cosYaw;
      const z1 = pt.z;

      // 2. Pitch rotation (X-axis)
      const x2 = x1;
      const y2 = y1 * cosPitch - z1 * sinPitch;
      const z2 = y1 * sinPitch + z1 * cosPitch;

      // Depth is y2 (depth along view axis)
      return {
        x: centerX + x2 * scale,
        y: centerY - z2 * scale, // invert Z for screen Y
        depth: y2,
      };
    };

    // Project all grid vertices
    const projected: ProjectedPoint[][] = [];
    for (let i = 0; i <= gridResolution; i++) {
      projected[i] = [];
      for (let j = 0; j <= gridResolution; j++) {
        projected[i][j] = project(points[i][j]);
      }
    }

    // Build quadrilateral faces
    const faces: PolygonData[] = [];
    for (let i = 0; i < gridResolution; i++) {
      for (let j = 0; j < gridResolution; j++) {
        const pA = projected[i][j];
        const pB = projected[i + 1][j];
        const pC = projected[i + 1][j + 1];
        const pD = projected[i][j + 1];

        // Average depth of this face
        const depth = (pA.depth + pB.depth + pC.depth + pD.depth) / 4;
        const zAvg = (points[i][j].z + points[i + 1][j].z + points[i + 1][j + 1].z + points[i][j + 1].z) / 4;

        faces.push({
          pts: [pA, pB, pC, pD],
          depth,
          zAvg,
        });
      }
    }

    // Painter's Algorithm: Sort faces by depth descending (farther ones first)
    faces.sort((a, b) => b.depth - a.depth);
    return faces;
  }, [gridData, yaw, pitch, width, height]);

  // Color helper based on zHeight
  const getShadedColor = (z: number, minZ: number, maxZ: number) => {
    const rangeZ = maxZ - minZ;
    const normalized = rangeZ > 1e-6 ? (z - minZ) / rangeZ : 0.5;
    // Map normalized value to HSL color (blue = 240 deg, cyan = 180, green = 120, red = 0)
    const hue = (1 - normalized) * 240;
    return `hsla(${hue.toFixed(0)}, 85%, 55%, 0.85)`;
  };

  return (
    <div
      ref={containerRef}
      style={{
        position: 'relative',
        width: '100%',
        height: '100%',
        background: '#141414',
        borderRadius: '8px',
        overflow: 'hidden',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        userSelect: 'none',
      }}
    >
      {!compiled ? (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100%',
            color: 'rgba(255, 255, 255, 0.35)',
            fontSize: '0.9rem',
            fontStyle: 'italic',
          }}
        >
          Enter a valid z = f(x, y) formula to render
        </div>
      ) : (
        <svg
          width="100%"
          height="100%"
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerUp}
          style={{ cursor: isDragging ? 'grabbing' : 'grab', touchAction: 'none' }}
        >
          {/* Surface Polygons */}
          {polygons.map((poly, idx) => {
            const pointsStr = poly.pts.map((p) => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');
            const fill = getShadedColor(poly.zAvg, gridData?.minZ ?? -1, gridData?.maxZ ?? 1);
            return (
              <polygon
                key={idx}
                points={pointsStr}
                fill={fill}
                stroke="rgba(0, 0, 0, 0.15)"
                strokeWidth="0.5"
              />
            );
          })}
        </svg>
      )}

      {/* Orbit Instructions overlay */}
      <div
        style={{
          position: 'absolute',
          bottom: '10px',
          left: '10px',
          background: 'rgba(0,0,0,0.6)',
          border: '1px solid rgba(255,255,255,0.08)',
          color: 'rgba(255,255,255,0.5)',
          padding: '4px 8px',
          borderRadius: '4px',
          fontSize: '0.75rem',
          pointerEvents: 'none',
        }}
      >
        Click & Drag to Rotate camera
      </div>
    </div>
  );
};
