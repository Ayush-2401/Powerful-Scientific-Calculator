import React from 'react';

export interface EquationItem {
  id: string;
  expr: string;
  expr2?: string; // for parametric y(t)
  color: string;
  visible: boolean;
  error?: string;
  error2?: string; // for parametric y(t)
}

interface EquationInputProps {
  equations: EquationItem[];
  onUpdateEquation: (id: string, updates: Partial<EquationItem>) => void;
  onAddEquation: () => void;
  onRemoveEquation: (id: string) => void;
  activeMode?: string;
}

const PRESET_COLORS = [
  '#ff4d4f', // red
  '#1890ff', // blue
  '#52c41a', // green
  '#722ed1', // purple
  '#fa8c16', // orange
  '#13c2c2', // cyan
];

export const EquationInput: React.FC<EquationInputProps> = ({
  equations,
  onUpdateEquation,
  onAddEquation,
  onRemoveEquation,
  activeMode = 'explicit',
}) => {
  return (
    <div className="equations-panel" style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
      <h3 style={{ margin: '0 0 8px 0', fontSize: '1rem', color: 'var(--text-accent)' }}>Equations</h3>
      <div className="equations-list" style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
        {equations.map((eq, index) => (
          <div
            key={eq.id}
            className="equation-row"
            style={{
              display: 'flex',
              flexDirection: 'column',
              padding: '10px',
              borderRadius: '8px',
              background: 'rgba(255, 255, 255, 0.03)',
              border: `1px solid ${eq.error || eq.error2 ? 'rgba(255, 77, 79, 0.3)' : 'rgba(255, 255, 255, 0.08)'}`,
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', width: '100%' }}>
              {/* Visibility Toggle */}
              <button
                type="button"
                onClick={() => onUpdateEquation(eq.id, { visible: !eq.visible })}
                style={{
                  background: 'none',
                  border: 'none',
                  color: eq.visible ? eq.color : 'rgba(255, 255, 255, 0.2)',
                  fontSize: '1.2rem',
                  cursor: 'pointer',
                  padding: 0,
                  width: '24px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
                title={eq.visible ? "Hide curve" : "Show curve"}
              >
                {eq.visible ? '●' : '○'}
              </button>

              {/* Color Picker */}
              <select
                value={eq.color}
                onChange={(e) => onUpdateEquation(eq.id, { color: e.target.value })}
                style={{
                  background: eq.color,
                  border: 'none',
                  borderRadius: '50%',
                  width: '16px',
                  height: '16px',
                  cursor: 'pointer',
                  appearance: 'none',
                  outline: 'none',
                }}
              >
                {PRESET_COLORS.map((c) => (
                  <option key={c} value={c} style={{ background: c }} />
                ))}
              </select>

              {/* Equation Label & Inputs */}
              {activeMode === 'parametric' ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <span style={{ minWidth: '45px', fontSize: '0.85rem', color: 'rgba(255, 255, 255, 0.6)', fontFamily: 'var(--font-mono)' }}>
                      x(t) =
                    </span>
                    <input
                      type="text"
                      value={eq.expr}
                      onChange={(e) => onUpdateEquation(eq.id, { expr: e.target.value })}
                      placeholder="e.g. 2 * cos(t)"
                      style={{
                        flex: 1,
                        background: 'rgba(0, 0, 0, 0.2)',
                        border: '1px solid rgba(255, 255, 255, 0.1)',
                        borderRadius: '4px',
                        color: '#fff',
                        padding: '6px 10px',
                        fontSize: '0.9rem',
                        fontFamily: 'var(--font-mono)',
                      }}
                    />
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <span style={{ minWidth: '45px', fontSize: '0.85rem', color: 'rgba(255, 255, 255, 0.6)', fontFamily: 'var(--font-mono)' }}>
                      y(t) =
                    </span>
                    <input
                      type="text"
                      value={eq.expr2 || ''}
                      onChange={(e) => onUpdateEquation(eq.id, { expr2: e.target.value })}
                      placeholder="e.g. 2 * sin(t)"
                      style={{
                        flex: 1,
                        background: 'rgba(0, 0, 0, 0.2)',
                        border: '1px solid rgba(255, 255, 255, 0.1)',
                        borderRadius: '4px',
                        color: '#fff',
                        padding: '6px 10px',
                        fontSize: '0.9rem',
                        fontFamily: 'var(--font-mono)',
                      }}
                    />
                  </div>
                </div>
              ) : (
                <>
                  <span style={{ fontSize: '0.9rem', color: 'rgba(255, 255, 255, 0.6)', fontFamily: 'var(--font-mono)' }}>
                    {`f${index + 1}(x) =`}
                  </span>

                  <input
                    type="text"
                    value={eq.expr}
                    onChange={(e) => onUpdateEquation(eq.id, { expr: e.target.value })}
                    placeholder="e.g. sin(x) * x"
                    style={{
                      flex: 1,
                      background: 'rgba(0, 0, 0, 0.2)',
                      border: '1px solid rgba(255, 255, 255, 0.1)',
                      borderRadius: '4px',
                      color: '#fff',
                      padding: '6px 10px',
                      fontSize: '0.9rem',
                      fontFamily: 'var(--font-mono)',
                    }}
                  />
                </>
              )}

              {/* Delete Button */}
              {equations.length > 1 && (
                <button
                  type="button"
                  onClick={() => onRemoveEquation(eq.id)}
                  style={{
                    background: 'rgba(255, 77, 79, 0.1)',
                    border: 'none',
                    borderRadius: '4px',
                    color: '#ff4d4f',
                    cursor: 'pointer',
                    padding: '4px 8px',
                    fontSize: '0.8rem',
                  }}
                >
                  ✕
                </button>
              )}
            </div>
            {eq.error && (
              <div style={{ color: '#ff4d4f', fontSize: '0.75rem', marginTop: '6px', marginLeft: '32px' }}>
                {activeMode === 'parametric' ? `x(t): ${eq.error}` : eq.error}
              </div>
            )}
            {eq.error2 && (
              <div style={{ color: '#ff4d4f', fontSize: '0.75rem', marginTop: '6px', marginLeft: '32px' }}>
                {`y(t): ${eq.error2}`}
              </div>
            )}
          </div>
        ))}
      </div>
      <button
        type="button"
        onClick={onAddEquation}
        className="solver-submit-btn"
        style={{
          marginTop: '6px',
          padding: '8px',
          fontSize: '0.85rem',
          background: 'rgba(255, 255, 255, 0.05)',
          border: '1px solid rgba(255, 255, 255, 0.15)',
          color: '#fff',
          borderRadius: '6px',
          cursor: 'pointer',
        }}
      >
        + Add Equation
      </button>
    </div>
  );
};
