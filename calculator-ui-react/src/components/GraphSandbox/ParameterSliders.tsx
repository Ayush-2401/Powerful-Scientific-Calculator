import React, { useState, useEffect, useRef } from 'react';

export interface ParameterConfig {
  min: number;
  max: number;
  step: number;
}

interface ParameterSlidersProps {
  parameters: string[];
  parameterValues: Record<string, number>;
  parameterConfigs: Record<string, ParameterConfig>;
  onChangeValue: (name: string, value: number) => void;
  onChangeConfig: (name: string, config: ParameterConfig) => void;
}

export const ParameterSliders: React.FC<ParameterSlidersProps> = ({
  parameters,
  parameterValues,
  parameterConfigs,
  onChangeValue,
  onChangeConfig,
}) => {
  const [animatingParams, setAnimatingParams] = useState<Record<string, boolean>>({});
  const [editingConfigs, setEditingConfigs] = useState<Record<string, boolean>>({});

  const latestValuesRef = useRef(parameterValues);
  latestValuesRef.current = parameterValues;

  const latestConfigsRef = useRef(parameterConfigs);
  latestConfigsRef.current = parameterConfigs;

  // Smooth animation effect
  useEffect(() => {
    let animationFrameId: number;
    let lastTime = performance.now();

    const animate = (time: number) => {
      const delta = time - lastTime;
      if (delta >= 16) {
        lastTime = time;

        Object.keys(animatingParams).forEach((name) => {
          if (!animatingParams[name] || !parameters.includes(name)) return;

          const config = latestConfigsRef.current[name] || { min: -10, max: 10, step: 0.1 };
          const range = config.max - config.min;
          const currentVal = latestValuesRef.current[name] ?? 0;

          // Sweep full range in 5 seconds
          const increment = (range / 5000) * delta;
          let newVal = currentVal + increment;
          if (newVal > config.max) {
            newVal = config.min;
          }

          // Align to step precision
          const steps = Math.round((newVal - config.min) / config.step);
          const steppedVal = config.min + steps * config.step;
          
          onChangeValue(name, parseFloat(steppedVal.toFixed(4)));
        });
      }
      animationFrameId = requestAnimationFrame(animate);
    };

    const isAnyAnimating = Object.keys(animatingParams).some(
      (name) => animatingParams[name] && parameters.includes(name)
    );

    if (isAnyAnimating) {
      animationFrameId = requestAnimationFrame(animate);
    }

    return () => {
      cancelAnimationFrame(animationFrameId);
    };
  }, [animatingParams, parameters, onChangeValue]);

  const toggleAnimate = (name: string) => {
    setAnimatingParams((prev) => ({
      ...prev,
      [name]: !prev[name],
    }));
  };

  const toggleEditConfig = (name: string) => {
    setEditingConfigs((prev) => ({
      ...prev,
      [name]: !prev[name],
    }));
  };

  if (parameters.length === 0) {
    return null;
  }

  return (
    <div
      className="parameters-panel"
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
        marginTop: '15px',
        borderTop: '1px solid rgba(255, 255, 255, 0.08)',
        paddingTop: '15px',
      }}
    >
      <h3 style={{ margin: '0 0 8px 0', fontSize: '1rem', color: 'var(--text-accent)' }}>Parameters</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {parameters.map((name) => {
          const value = parameterValues[name] ?? 0;
          const config = parameterConfigs[name] || { min: -10, max: 10, step: 0.1 };
          const isAnimating = !!animatingParams[name];
          const isEditing = !!editingConfigs[name];

          return (
            <div
              key={name}
              style={{
                display: 'flex',
                flexDirection: 'column',
                background: 'rgba(255, 255, 255, 0.02)',
                border: '1px solid rgba(255, 255, 255, 0.06)',
                borderRadius: '6px',
                padding: '10px',
              }}
            >
              {/* Parameter Title + Input */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px', marginBottom: '8px' }}>
                <span style={{ fontWeight: 'bold', fontSize: '0.95rem', color: '#fff', fontFamily: 'var(--font-mono)' }}>
                  {name}
                </span>

                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  {/* Play/Pause Button */}
                  <button
                    type="button"
                    onClick={() => toggleAnimate(name)}
                    style={{
                      background: 'rgba(255, 255, 255, 0.05)',
                      border: '1px solid rgba(255, 255, 255, 0.1)',
                      color: isAnimating ? 'var(--text-accent)' : '#fff',
                      cursor: 'pointer',
                      borderRadius: '4px',
                      padding: '2px 8px',
                      fontSize: '0.75rem',
                      fontFamily: 'var(--font-mono)',
                    }}
                  >
                    {isAnimating ? '⏸ Pause' : '▶ Play'}
                  </button>

                  {/* Value Text Input */}
                  <input
                    type="number"
                    step={config.step}
                    value={value}
                    onChange={(e) => {
                      const v = parseFloat(e.target.value);
                      if (!isNaN(v)) {
                        const clamped = Math.min(config.max, Math.max(config.min, v));
                        onChangeValue(name, clamped);
                      }
                    }}
                    aria-label={`Value of parameter ${name}`}
                    style={{
                      width: '65px',
                      background: 'rgba(0, 0, 0, 0.3)',
                      border: '1px solid rgba(255, 255, 255, 0.1)',
                      borderRadius: '4px',
                      color: '#fff',
                      textAlign: 'center',
                      padding: '2px 4px',
                      fontSize: '0.85rem',
                      fontFamily: 'var(--font-mono)',
                    }}
                  />

                  {/* Settings Toggle */}
                  <button
                    type="button"
                    onClick={() => toggleEditConfig(name)}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: 'rgba(255, 255, 255, 0.4)',
                      cursor: 'pointer',
                      fontSize: '0.9rem',
                      padding: '0 4px',
                    }}
                    title="Edit slider bounds"
                  >
                    ⚙
                  </button>
                </div>
              </div>

              {/* Slider Input */}
              <input
                type="range"
                min={config.min}
                max={config.max}
                step={config.step}
                value={value}
                onChange={(e) => onChangeValue(name, parseFloat(e.target.value))}
                aria-label={`Parameter ${name} slider`}
                aria-valuemin={config.min}
                aria-valuemax={config.max}
                aria-valuenow={value}
                style={{
                  width: '100%',
                  height: '4px',
                  borderRadius: '2px',
                  background: 'rgba(255, 255, 255, 0.1)',
                  cursor: 'pointer',
                }}
              />

              {/* Accessible Live Region for Value Changes */}
              <div
                aria-live="polite"
                style={{
                  position: 'absolute',
                  width: '1px',
                  height: '1px',
                  padding: 0,
                  margin: '-1px',
                  overflow: 'hidden',
                  clip: 'rect(0, 0, 0, 0)',
                  border: 0,
                }}
              >
                {`Parameter ${name} is set to ${value}`}
              </div>

              {/* Settings Configuration Area */}
              {isEditing && (
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(3, 1fr)',
                    gap: '6px',
                    marginTop: '10px',
                    background: 'rgba(0,0,0,0.15)',
                    padding: '8px',
                    borderRadius: '4px',
                    border: '1px dashed rgba(255,255,255,0.06)',
                  }}
                >
                  <div>
                    <label style={{ fontSize: '0.7rem', color: 'rgba(255,255,255,0.4)', display: 'block', marginBottom: '2px' }}>Min</label>
                    <input
                      type="number"
                      value={config.min}
                      onChange={(e) => {
                        const val = parseFloat(e.target.value);
                        if (!isNaN(val)) onChangeConfig(name, { ...config, min: val });
                      }}
                      style={{
                        width: '100%',
                        background: 'rgba(0,0,0,0.3)',
                        border: '1px solid rgba(255,255,255,0.08)',
                        color: '#fff',
                        fontSize: '0.75rem',
                        padding: '2px',
                        borderRadius: '3px',
                      }}
                    />
                  </div>
                  <div>
                    <label style={{ fontSize: '0.7rem', color: 'rgba(255,255,255,0.4)', display: 'block', marginBottom: '2px' }}>Max</label>
                    <input
                      type="number"
                      value={config.max}
                      onChange={(e) => {
                        const val = parseFloat(e.target.value);
                        if (!isNaN(val)) onChangeConfig(name, { ...config, max: val });
                      }}
                      style={{
                        width: '100%',
                        background: 'rgba(0,0,0,0.3)',
                        border: '1px solid rgba(255,255,255,0.08)',
                        color: '#fff',
                        fontSize: '0.75rem',
                        padding: '2px',
                        borderRadius: '3px',
                      }}
                    />
                  </div>
                  <div>
                    <label style={{ fontSize: '0.7rem', color: 'rgba(255,255,255,0.4)', display: 'block', marginBottom: '2px' }}>Step</label>
                    <input
                      type="number"
                      value={config.step}
                      min="0.001"
                      onChange={(e) => {
                        const val = parseFloat(e.target.value);
                        if (!isNaN(val) && val > 0) onChangeConfig(name, { ...config, step: val });
                      }}
                      style={{
                        width: '100%',
                        background: 'rgba(0,0,0,0.3)',
                        border: '1px solid rgba(255,255,255,0.08)',
                        color: '#fff',
                        fontSize: '0.75rem',
                        padding: '2px',
                        borderRadius: '3px',
                      }}
                    />
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
