import { useState, useCallback } from 'react';

export interface Viewport {
  xMin: number;
  xMax: number;
  yMin: number;
  yMax: number;
}

export function useViewport(initialViewport: Viewport = { xMin: -10, xMax: 10, yMin: -10, yMax: 10 }) {
  const [viewport, setViewport] = useState<Viewport>(initialViewport);

  const setViewportBounds = useCallback((bounds: Partial<Viewport>) => {
    setViewport((prev) => ({
      ...prev,
      ...bounds,
    }));
  }, []);

  const resetViewport = useCallback(() => {
    setViewport(initialViewport);
  }, [initialViewport]);

  const toScreenX = useCallback((x: number, width: number) => {
    const { xMin, xMax } = viewport;
    return ((x - xMin) / (xMax - xMin)) * width;
  }, [viewport]);

  const toScreenY = useCallback((y: number, height: number) => {
    const { yMin, yMax } = viewport;
    return height - ((y - yMin) / (yMax - yMin)) * height;
  }, [viewport]);

  const toMathX = useCallback((screenX: number, width: number) => {
    const { xMin, xMax } = viewport;
    return xMin + (screenX / width) * (xMax - xMin);
  }, [viewport]);

  const toMathY = useCallback((screenY: number, height: number) => {
    const { yMin, yMax } = viewport;
    return yMax - (screenY / height) * (yMax - yMin);
  }, [viewport]);

  const pan = useCallback((dx: number, dy: number, width: number, height: number) => {
    setViewport((prev) => {
      const xRange = prev.xMax - prev.xMin;
      const yRange = prev.yMax - prev.yMin;
      
      const deltaX = (dx / width) * xRange;
      const deltaY = (dy / height) * yRange;

      return {
        xMin: prev.xMin - deltaX,
        xMax: prev.xMax - deltaX,
        yMin: prev.yMin + deltaY,
        yMax: prev.yMax + deltaY,
      };
    });
  }, []);

  const zoom = useCallback((factor: number, clientX: number, clientY: number, width: number, height: number) => {
    setViewport((prev) => {
      const anchorX = prev.xMin + (clientX / width) * (prev.xMax - prev.xMin);
      const anchorY = prev.yMax - (clientY / height) * (prev.yMax - prev.yMin);

      const xRange = prev.xMax - prev.xMin;
      const yRange = prev.yMax - prev.yMin;

      const newXRange = xRange * factor;
      const newYRange = yRange * factor;

      // Maintain relative position to the anchor
      const pctX = (anchorX - prev.xMin) / xRange;
      const pctY = (anchorY - prev.yMin) / yRange;

      return {
        xMin: anchorX - pctX * newXRange,
        xMax: anchorX + (1 - pctX) * newXRange,
        yMin: anchorY - pctY * newYRange,
        yMax: anchorY + (1 - pctY) * newYRange,
      };
    });
  }, []);

  return {
    viewport,
    setViewportBounds,
    resetViewport,
    toScreenX,
    toScreenY,
    toMathX,
    toMathY,
    pan,
    zoom,
  };
}
