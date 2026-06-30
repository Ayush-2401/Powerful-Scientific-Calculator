import React from 'react';

export type GeometryTool = 
  | 'select' | 'point' | 'line' | 'segment' | 'ray' | 'vector' | 'circle' | 'midpoint' | 'intersect' | 'angle'
  | 'reflect' | 'rotate' | 'translate' | 'dilate' | 'ellipse' | 'polygon';

interface ToolPaletteProps {
  activeTool: GeometryTool;
  onChangeTool: (tool: GeometryTool) => void;
}

interface ToolItem {
  id: GeometryTool;
  label: string;
  icon: string;
  description: string;
  keyShortcut: string;
}

const TOOLS: ToolItem[] = [
  { id: 'select', label: 'Select', icon: '⬈', description: 'Select and drag points', keyShortcut: 'S' },
  { id: 'point', label: 'Point', icon: '●', description: 'Click to add a point', keyShortcut: 'P' },
  { id: 'line', label: 'Line', icon: '⟷', description: 'Click two points to draw a line', keyShortcut: 'L' },
  { id: 'segment', label: 'Segment', icon: '―', description: 'Click two points to draw a segment', keyShortcut: 'G' },
  { id: 'ray', label: 'Ray', icon: '⟶', description: 'Click start point, then point on ray', keyShortcut: 'R' },
  { id: 'vector', label: 'Vector', icon: '↗', description: 'Click start point, then endpoint', keyShortcut: 'V' },
  { id: 'circle', label: 'Circle', icon: '○', description: 'Click center point, then radius point', keyShortcut: 'C' },
  { id: 'midpoint', label: 'Midpoint', icon: '◓', description: 'Click two points to construct midpoint', keyShortcut: 'M' },
  { id: 'intersect', label: 'Intersect', icon: '≬', description: 'Click two lines/circles to intersect', keyShortcut: 'I' },
  { id: 'angle', label: 'Angle', icon: '⦢', description: 'Click three points to construct angle (vertex second)', keyShortcut: 'A' },
  { id: 'reflect', label: 'Reflect', icon: '↔', description: 'Click point, then line to reflect across', keyShortcut: 'H' },
  { id: 'rotate', label: 'Rotate', icon: '⟳', description: 'Click point, then rotation center point', keyShortcut: 'O' },
  { id: 'translate', label: 'Translate', icon: '⇾', description: 'Click point, then translation vector', keyShortcut: 'T' },
  { id: 'dilate', label: 'Dilate', icon: '⤧', description: 'Click point, then dilation center point', keyShortcut: 'D' },
  { id: 'ellipse', label: 'Ellipse', icon: '⬭', description: 'Click two foci, then point on ellipse', keyShortcut: 'E' },
  { id: 'polygon', label: 'Polygon', icon: '▲', description: 'Click points, click start point or double click to close', keyShortcut: 'Y' },
];

export const ToolPalette: React.FC<ToolPaletteProps> = ({ activeTool, onChangeTool }) => {
  return (
    <div
      style={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: '6px',
        padding: '10px',
        background: 'rgba(255, 255, 255, 0.03)',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        borderRadius: '8px',
        marginBottom: '10px',
      }}
    >
      {TOOLS.map((tool) => {
        const isActive = activeTool === tool.id;
        return (
          <button
            key={tool.id}
            type="button"
            onClick={() => onChangeTool(tool.id)}
            aria-keyshortcuts={tool.keyShortcut}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '6px 12px',
              fontSize: '0.85rem',
              fontWeight: 500,
              color: isActive ? 'var(--text-accent)' : '#fff',
              background: isActive ? 'rgba(24, 144, 255, 0.15)' : 'rgba(255, 255, 255, 0.05)',
              border: isActive
                ? '1px solid var(--text-accent)'
                : '1px solid rgba(255, 255, 255, 0.1)',
              borderRadius: '6px',
              cursor: 'pointer',
              transition: 'all 0.15s ease',
            }}
            title={`${tool.description} (Shortcut: ${tool.keyShortcut})`}
          >
            <span style={{ fontSize: '1rem', lineHeight: 1 }}>{tool.icon}</span>
            <span>{tool.label}</span>
            <span
              style={{
                fontSize: '0.65rem',
                color: isActive ? 'var(--text-accent)' : 'rgba(255, 255, 255, 0.35)',
                background: 'rgba(0, 0, 0, 0.25)',
                padding: '1px 4px',
                borderRadius: '3px',
                fontFamily: 'var(--font-mono)',
              }}
            >
              {tool.keyShortcut}
            </span>
          </button>
        );
      })}
    </div>
  );
};
