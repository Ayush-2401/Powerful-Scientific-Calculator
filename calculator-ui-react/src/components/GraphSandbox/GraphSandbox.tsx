import React, { useState, useEffect, useMemo } from 'react';
import { useUndoRedo } from '../../hooks/useUndoRedo';
import * as math from 'mathjs';
import { useViewport } from './useViewport';
import { EquationInput, EquationItem } from './EquationInput';
import { GraphCanvas, EvaluatedCurve, ImplicitCurve, Point2D } from './GraphCanvas';
import { extractParameters } from './useParameterParser';
import { ParameterSliders, ParameterConfig } from './ParameterSliders';
import { plotImplicitCurve } from './marchingSquares';
import { fitLinear, fitQuadratic } from './regressionMath';
import { Graph3D } from './Graph3D';
import { useCAS } from '../../hooks/useCAS';

const DEFAULT_EQUATIONS: EquationItem[] = [
  { id: '1', expr: 'a * sin(b * x) + c', color: '#ff4d4f', visible: true },
];

export const GraphSandbox: React.FC = () => {
  const {
    viewport,
    pan,
    zoom,
    resetViewport,
    toScreenX,
    toScreenY,
    toMathX,
  } = useViewport();

  const { simplifyExpr, differentiateExpr } = useCAS();

  // Mode Selection: 'explicit' | 'implicit' | 'parametric' | 'regression' | '3d'
  const [activeMode, setActiveMode] = useState<'explicit' | 'implicit' | 'parametric' | 'regression' | '3d'>('explicit');

  // Explicit/Implicit equation state history wrapper
  const {
    state: equations,
    set: setEquations,
    undo,
    redo,
    canUndo,
    canRedo,
  } = useUndoRedo<EquationItem[]>(DEFAULT_EQUATIONS);

  // Parameter states
  const [parameterValues, setParameterValues] = useState<Record<string, number>>({});
  const [parameterConfigs, setParameterConfigs] = useState<Record<string, ParameterConfig>>({});

  // Calculus Overlays states
  const [showTangent, setShowTangent] = useState(false);
  const [showIntegral, setShowIntegral] = useState(false);
  const [integralBounds, setIntegralBounds] = useState({ a: -2, b: 2 });
  const [showRiemann, setShowRiemann] = useState(false);
  const [riemannIntervals, setRiemannIntervals] = useState(6);
  const [riemannType, setRiemannType] = useState<'left' | 'right' | 'midpoint'>('midpoint');

  // Regression states
  const [regressionInput, setRegressionInput] = useState<string>("1, 2\n2, 3.8\n3, 9\n4, 16.2");
  const [regressionType, setRegressionType] = useState<'linear' | 'quadratic'>('linear');

  // 3D equation state
  const [equation3D, setEquation3D] = useState<string>("z = sin(x) * cos(y)");

  // 1. Detect parameters based on active mode context
  const detectedParameters = useMemo(() => {
    const paramsSet = new Set<string>();
    if (activeMode === 'explicit' || activeMode === 'implicit') {
      equations.forEach((eq) => {
        extractParameters(eq.expr).forEach((p) => paramsSet.add(p));
      });
    } else if (activeMode === '3d') {
      extractParameters(equation3D).forEach((p) => paramsSet.add(p));
    }
    return Array.from(paramsSet);
  }, [equations, equation3D, activeMode]);

  // 2. Initialize newly detected parameters with sensible defaults
  useEffect(() => {
    setParameterValues((prev) => {
      let changed = false;
      const next = { ...prev };
      detectedParameters.forEach((param) => {
        if (next[param] === undefined) {
          next[param] = 1; // default initial value
          changed = true;
        }
      });
      return changed ? next : prev;
    });

    setParameterConfigs((prev) => {
      let changed = false;
      const next = { ...prev };
      detectedParameters.forEach((param) => {
        if (next[param] === undefined) {
          next[param] = { min: -10, max: 10, step: 0.1 };
          changed = true;
        }
      });
      return changed ? next : prev;
    });
  }, [detectedParameters]);

  // 3. Equation input validators
  useEffect(() => {
    if (activeMode === 'regression') return;

    let changed = false;
    const updatedEquations = equations.map((eq) => {
      if (activeMode === 'parametric') {
        const cleanExpr1 = eq.expr.trim();
        const cleanExpr2 = (eq.expr2 || '').trim();

        let error1: string | undefined = undefined;
        let error2: string | undefined = undefined;

        if (cleanExpr1 !== '') {
          if (cleanExpr1.length > 500) {
            error1 = 'Equation too long (max 500 characters)';
          } else {
            try {
              let exprToCompile = cleanExpr1;
              if (exprToCompile.includes('=')) {
                exprToCompile = exprToCompile.split('=')[1].trim();
              }
              const compiled = math.compile(exprToCompile);
              const exprParams = extractParameters(exprToCompile);
              const testScope: Record<string, number> = { t: 1 };
              exprParams.forEach((p) => {
                testScope[p] = parameterValues[p] ?? 1;
              });
              compiled.evaluate(testScope);
            } catch (err: any) {
              error1 = err.message || 'Invalid expression';
            }
          }
        }

        if (cleanExpr2 !== '') {
          if (cleanExpr2.length > 500) {
            error2 = 'Equation too long (max 500 characters)';
          } else {
            try {
              let exprToCompile = cleanExpr2;
              if (exprToCompile.includes('=')) {
                exprToCompile = exprToCompile.split('=')[1].trim();
              }
              const compiled = math.compile(exprToCompile);
              const exprParams = extractParameters(exprToCompile);
              const testScope: Record<string, number> = { t: 1 };
              exprParams.forEach((p) => {
                testScope[p] = parameterValues[p] ?? 1;
              });
              compiled.evaluate(testScope);
            } catch (err: any) {
              error2 = err.message || 'Invalid expression';
            }
          }
        }

        if (eq.error !== error1 || eq.error2 !== error2) {
          changed = true;
          return { ...eq, error: error1, error2: error2 };
        }
        return eq;
      }

      const cleanExpr = eq.expr.trim();
      if (cleanExpr === '') {
        return eq.error ? { ...eq, error: undefined } : eq;
      }

      if (cleanExpr.length > 500) {
        const errMsg = 'Equation too long (max 500 characters)';
        if (eq.error !== errMsg) {
          changed = true;
          return { ...eq, error: errMsg };
        }
        return eq;
      }

      let exprToCompile = cleanExpr;
      if (exprToCompile.includes('=')) {
        const parts = exprToCompile.split('=');
        exprToCompile = parts[1].trim();
      }

      try {
        const compiled = math.compile(exprToCompile);
        const exprParams = extractParameters(exprToCompile);
        const testScope: Record<string, number> = { x: 1, y: 1 };
        exprParams.forEach((p) => {
          testScope[p] = parameterValues[p] ?? 1;
        });

        compiled.evaluate(testScope);
        if (eq.error) {
          changed = true;
          return { ...eq, error: undefined };
        }
      } catch (err: any) {
        const errMsg = err.message || 'Invalid expression';
        if (eq.error !== errMsg) {
          changed = true;
          return { ...eq, error: errMsg };
        }
      }
      return eq;
    });

    if (changed) {
      setEquations(updatedEquations);
    }
  }, [equations, parameterValues, activeMode]);

  const handleUpdateEquation = (id: string, updates: Partial<EquationItem>) => {
    setEquations((prev) =>
      prev.map((eq) => (eq.id === id ? { ...eq, ...updates } : eq))
    );
  };

  const handleAddEquation = () => {
    const colors = ['#ff4d4f', '#1890ff', '#52c41a', '#722ed1', '#fa8c16', '#13c2c2'];
    const nextColor = colors[equations.length % colors.length];
    const newId = crypto.randomUUID();
    setEquations((prev) => [
      ...prev,
      { id: newId, expr: '', color: nextColor, visible: true },
    ]);
  };

  const handleRemoveEquation = (id: string) => {
    setEquations((prev) => prev.filter((eq) => eq.id !== id));
  };

  const handleParameterChangeValue = (name: string, value: number) => {
    setParameterValues((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleParameterChangeConfig = (name: string, config: ParameterConfig) => {
    setParameterConfigs((prev) => ({
      ...prev,
      [name]: config,
    }));
  };

  // Compile active and valid curves for rendering
  const explicitCurves = useMemo(() => {
    if (activeMode !== 'explicit') return [];
    return equations
      .filter((eq) => eq.visible && eq.expr.trim() !== '')
      .map((eq) => {
        let cleanExpr = eq.expr.trim();
        if (cleanExpr.includes('=')) {
          const parts = cleanExpr.split('=');
          cleanExpr = parts[1].trim();
        }

        try {
          const compiled = math.compile(cleanExpr);
          return {
            id: eq.id,
            color: eq.color,
            points: [] as Array<{ x: number; y: number }>,
            evaluate: (x: number) => {
              const evalScope = { x, ...parameterValues };
              const val = compiled.evaluate(evalScope);
              return typeof val === 'number' ? val : NaN;
            },
          };
        } catch (err) {
          return null;
        }
      })
      .filter((curve): curve is EvaluatedCurve => curve !== null);
  }, [equations, parameterValues, activeMode]);

  // Evaluate implicit curves
  const implicitCurves = useMemo(() => {
    if (activeMode !== 'implicit') return [];
    return equations
      .filter((eq) => eq.visible && eq.expr.trim() !== '')
      .map((eq) => {
        const cleanExpr = eq.expr.trim();
        let lhs = cleanExpr;
        let rhs = '0';
        if (cleanExpr.includes('=')) {
          const parts = cleanExpr.split('=');
          lhs = parts[0].trim();
          rhs = parts[1].trim();
        }

        const lines = plotImplicitCurve(
          lhs,
          rhs,
          viewport.xMin,
          viewport.xMax,
          viewport.yMin,
          viewport.yMax,
          parameterValues,
          75
        );

        return {
          id: eq.id,
          color: eq.color,
          lines,
        };
      })
      .filter((curve): curve is ImplicitCurve => curve !== null);
  }, [equations, parameterValues, activeMode, viewport]);

  // Evaluate parametric curves
  const parametricCurves = useMemo(() => {
    if (activeMode !== 'parametric') return [];
    return equations
      .filter((eq) => eq.visible && eq.expr.trim() !== '')
      .map((eq) => {
        let cleanExpr1 = eq.expr.trim();
        let cleanExpr2 = (eq.expr2 || '').trim();

        if (cleanExpr1.includes('=')) {
          cleanExpr1 = cleanExpr1.split('=')[1].trim();
        }
        if (cleanExpr2.includes('=')) {
          cleanExpr2 = cleanExpr2.split('=')[1].trim();
        }

        try {
          const compiled1 = math.compile(cleanExpr1);
          const compiled2 = math.compile(cleanExpr2);

          return {
            id: eq.id,
            color: eq.color,
            evaluateX: (t: number) => {
              const val = compiled1.evaluate({ t, ...parameterValues });
              return typeof val === 'number' ? val : NaN;
            },
            evaluateY: (t: number) => {
              const val = compiled2.evaluate({ t, ...parameterValues });
              return typeof val === 'number' ? val : NaN;
            },
          };
        } catch (err) {
          return null;
        }
      })
      .filter((curve): curve is any => curve !== null);
  }, [equations, parameterValues, activeMode]);

  // Regression fitting evaluator
  const regressionPoints = useMemo(() => {
    const lines = regressionInput.split('\n');
    const pts: Point2D[] = [];
    lines.forEach((line) => {
      const parts = line.trim().split(/[,\s]+/);
      if (parts.length >= 2) {
        const x = parseFloat(parts[0]);
        const y = parseFloat(parts[1]);
        if (!isNaN(x) && !isNaN(y)) {
          pts.push({ x, y });
        }
      }
    });
    return pts;
  }, [regressionInput]);

  const regressionResult = useMemo(() => {
    if (activeMode !== 'regression' || regressionPoints.length < 2) return null;
    return regressionType === 'linear' ? fitLinear(regressionPoints) : fitQuadratic(regressionPoints);
  }, [activeMode, regressionPoints, regressionType]);

  const curvesToRender = useMemo(() => {
    const list = [...explicitCurves];
    if (activeMode === 'regression' && regressionResult) {
      list.push({
        id: 'regression-fit',
        color: '#1890ff',
        points: [] as Array<{ x: number; y: number }>,
        evaluate: regressionResult.evaluate,
      });
    }
    return list;
  }, [explicitCurves, activeMode, regressionResult]);

  // Numerical Integration result display calculation
  const calculatedIntegralValue = useMemo(() => {
    if (!showIntegral || explicitCurves.length === 0) return null;
    const curve = explicitCurves[0];
    const { a, b } = integralBounds;
    const steps = 200;
    const h = (b - a) / steps;
    let sum = 0.5 * (curve.evaluate(a) + curve.evaluate(b));

    for (let i = 1; i < steps; i++) {
      const x = a + i * h;
      const y = curve.evaluate(x);
      if (!isNaN(y) && isFinite(y)) {
        sum += y;
      }
    }
    return sum * h;
  }, [showIntegral, explicitCurves, integralBounds]);

  const exportSvg = () => {
    const container = document.querySelector('.graph-sandbox-container');
    const svg = container?.querySelector('svg');
    if (!svg) return;
    const serializer = new XMLSerializer();
    const svgStr = serializer.serializeToString(svg);
    const blob = new Blob([svgStr], { type: 'image/svg+xml' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'graph_sandbox.svg';
    link.click();
    URL.revokeObjectURL(url);
  };

  const exportPng = () => {
    const container = document.querySelector('.graph-sandbox-container');
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
        link.download = 'graph_sandbox.png';
        link.click();
      }
    };
  };

  const handleSaveSession = () => {
    const sessionData = {
      equations,
      parameterValues,
      parameterConfigs,
      activeMode,
      equation3D,
      regressionInput,
      regressionType,
      showTangent,
      showIntegral,
      integralBounds,
      showRiemann,
      riemannIntervals,
      riemannType,
    };
    const blob = new Blob([JSON.stringify(sessionData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'graph_session.json';
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
        if (data.equations) setEquations(data.equations);
        if (data.parameterValues) setParameterValues(data.parameterValues);
        if (data.parameterConfigs) setParameterConfigs(data.parameterConfigs);
        if (data.activeMode) setActiveMode(data.activeMode);
        if (data.equation3D) setEquation3D(data.equation3D);
        if (data.regressionInput) setRegressionInput(data.regressionInput);
        if (data.regressionType) setRegressionType(data.regressionType);
        if (data.showTangent !== undefined) setShowTangent(data.showTangent);
        if (data.showIntegral !== undefined) setShowIntegral(data.showIntegral);
        if (data.integralBounds) setIntegralBounds(data.integralBounds);
        if (data.showRiemann !== undefined) setShowRiemann(data.showRiemann);
        if (data.riemannIntervals) setRiemannIntervals(data.riemannIntervals);
        if (data.riemannType) setRiemannType(data.riemannType);
      } catch (err: any) {
        alert('Invalid session JSON: ' + err.message);
      }
    };
    reader.readAsText(file);
    e.target.value = ''; // Clear file input
  };

  // Keyboard shortcuts for Undo / Redo
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
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [undo, redo]);

  return (
    <div
      className="graph-sandbox-wrapper"
      style={{
        display: 'flex',
        flexDirection: 'column',
        width: '100%',
        marginTop: '10px',
      }}
    >
      {/* Top Mode Selection Tab Bar */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          background: 'rgba(255, 255, 255, 0.03)',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          borderRadius: '8px',
          padding: '8px 16px',
          marginBottom: '10px',
          flexWrap: 'wrap',
          gap: '10px',
        }}
      >
        <div style={{ display: 'flex', gap: '6px' }}>
          {(['explicit', 'implicit', 'parametric', 'regression', '3d'] as const).map((mode) => (
            <button
              key={mode}
              type="button"
              onClick={() => setActiveMode(mode)}
              style={{
                padding: '6px 12px',
                fontSize: '0.85rem',
                borderRadius: '6px',
                border: '1px solid rgba(255,255,255,0.1)',
                background: activeMode === mode ? 'var(--text-accent)' : 'rgba(255,255,255,0.03)',
                color: '#fff',
                cursor: 'pointer',
                fontWeight: activeMode === mode ? 'bold' : 'normal',
                textTransform: 'capitalize',
              }}
            >
              {mode === '3d' ? '3D Surface' : `${mode} plotting`}
            </button>
          ))}
        </div>

        {/* Undo, Redo, Exports */}
        <div style={{ display: 'flex', gap: '6px' }}>
          <div style={{ display: 'flex', gap: '4px', marginRight: '10px' }}>
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
              onClick={() => document.getElementById('load-graph-session-input')?.click()}
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
              id="load-graph-session-input"
              style={{ display: 'none' }}
              accept=".json"
              onChange={handleLoadSession}
            />

            {activeMode !== '3d' && (
              <>
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
            </>
          )}
        </div>
      </div>
    </div>

      <div
        className="graph-sandbox-container"
        style={{
          display: 'flex',
          gap: '20px',
          height: '520px',
          width: '100%',
        }}
      >
        {/* Sidebar Controls */}
        <div
          className="sandbox-sidebar"
          style={{
            width: '300px',
            background: 'rgba(255, 255, 255, 0.02)',
            border: '1px solid rgba(255, 255, 255, 0.08)',
            borderRadius: '8px',
            padding: '16px',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            overflowY: 'auto',
          }}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            
            {/* Mode-specific Inputs */}
            {(activeMode === 'explicit' || activeMode === 'implicit' || activeMode === 'parametric') && (
              <EquationInput
                equations={equations}
                onUpdateEquation={handleUpdateEquation}
                onAddEquation={handleAddEquation}
                onRemoveEquation={handleRemoveEquation}
                activeMode={activeMode}
              />
            )}

            {activeMode === 'regression' && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <h4 style={{ margin: 0, fontSize: '0.9rem', color: 'var(--text-accent)' }}>Point Coordinates (x, y)</h4>
                <textarea
                  value={regressionInput}
                  onChange={(e) => setRegressionInput(e.target.value)}
                  style={{
                    width: '100%',
                    height: '100px',
                    background: 'rgba(0,0,0,0.5)',
                    border: '1px solid rgba(255,255,255,0.15)',
                    color: '#fff',
                    borderRadius: '6px',
                    padding: '8px',
                    fontSize: '0.8rem',
                    fontFamily: 'var(--font-mono)',
                    resize: 'none',
                  }}
                  placeholder="1, 2&#10;2, 3.8&#10;3, 9"
                />

                <div style={{ display: 'flex', gap: '8px', alignItems: 'center', marginTop: '6px' }}>
                  <span style={{ fontSize: '0.85rem', color: 'rgba(255,255,255,0.7)' }}>Model:</span>
                  <select
                    value={regressionType}
                    onChange={(e) => setRegressionType(e.target.value as any)}
                    style={{
                      background: '#1f1f1f',
                      border: '1px solid rgba(255,255,255,0.15)',
                      color: '#fff',
                      borderRadius: '4px',
                      padding: '4px',
                      fontSize: '0.85rem',
                      cursor: 'pointer',
                    }}
                  >
                    <option value="linear">Linear (y = mx + c)</option>
                    <option value="quadratic">Quadratic (y = ax² + bx + c)</option>
                  </select>
                </div>

                {regressionResult && (
                  <div
                    style={{
                      background: 'rgba(24, 144, 255, 0.08)',
                      border: '1px solid rgba(24, 144, 255, 0.25)',
                      borderRadius: '6px',
                      padding: '8px 10px',
                      marginTop: '8px',
                      fontSize: '0.8rem',
                      fontFamily: 'var(--font-mono)',
                    }}
                  >
                    <div style={{ color: 'var(--text-accent)', fontWeight: 'bold' }}>Fitted Curve:</div>
                    <div style={{ color: '#fff', marginTop: '4px' }}>{regressionResult.equation}</div>
                    <div style={{ color: 'rgba(255,255,255,0.6)', marginTop: '4px' }}>R²: {regressionResult.r2.toFixed(4)}</div>
                  </div>
                )}
              </div>
            )}

            {activeMode === '3d' && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <h4 style={{ margin: 0, fontSize: '0.9rem', color: 'var(--text-accent)' }}>z = f(x, y) formula</h4>
                <input
                  type="text"
                  value={equation3D}
                  onChange={(e) => setEquation3D(e.target.value)}
                  style={{
                    width: '100%',
                    background: 'rgba(0,0,0,0.5)',
                    border: '1px solid rgba(255,255,255,0.15)',
                    color: '#fff',
                    borderRadius: '6px',
                    padding: '8px 10px',
                    fontSize: '0.85rem',
                  }}
                  placeholder="sin(x) * cos(y)"
                />
              </div>
            )}

            {/* Parameter sliders wrapper */}
            {detectedParameters.length > 0 && (
              <ParameterSliders
                parameters={detectedParameters}
                parameterValues={parameterValues}
                parameterConfigs={parameterConfigs}
                onChangeValue={handleParameterChangeValue}
                onChangeConfig={handleParameterChangeConfig}
              />
            )}

            {/* Calculus settings for explicit mode */}
            {activeMode === 'explicit' && explicitCurves.length > 0 && (
              <div
                style={{
                  borderTop: '1px solid rgba(255, 255, 255, 0.08)',
                  paddingTop: '12px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '10px',
                }}
              >
                <h4 style={{ margin: 0, fontSize: '0.9rem', color: 'var(--text-accent)' }}>Calculus Overlays</h4>

                {/* Tangent line toggle */}
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '0.85rem' }}>
                  <input
                    type="checkbox"
                    checked={showTangent}
                    onChange={(e) => setShowTangent(e.target.checked)}
                  />
                  <span>Show Tangent on Hover</span>
                </label>

                {/* Definite Integral toggle */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '0.85rem' }}>
                    <input
                      type="checkbox"
                      checked={showIntegral}
                      onChange={(e) => setShowIntegral(e.target.checked)}
                    />
                    <span>Show Definite Integral Area</span>
                  </label>

                  {showIntegral && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', paddingLeft: '20px' }}>
                      <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                        <span style={{ fontSize: '0.75rem', color: 'rgba(255,255,255,0.5)' }}>a:</span>
                        <input
                          type="number"
                          value={integralBounds.a}
                          onChange={(e) => setIntegralBounds({ ...integralBounds, a: parseFloat(e.target.value) || 0 })}
                          style={{
                            width: '50px',
                            background: '#1f1f1f',
                            border: '1px solid rgba(255,255,255,0.1)',
                            color: '#fff',
                            borderRadius: '4px',
                            fontSize: '0.75rem',
                            padding: '2px 4px',
                          }}
                        />
                        <span style={{ fontSize: '0.75rem', color: 'rgba(255,255,255,0.5)' }}>b:</span>
                        <input
                          type="number"
                          value={integralBounds.b}
                          onChange={(e) => setIntegralBounds({ ...integralBounds, b: parseFloat(e.target.value) || 0 })}
                          style={{
                            width: '50px',
                            background: '#1f1f1f',
                            border: '1px solid rgba(255,255,255,0.1)',
                            color: '#fff',
                            borderRadius: '4px',
                            fontSize: '0.75rem',
                            padding: '2px 4px',
                          }}
                        />
                      </div>
                      {calculatedIntegralValue !== null && (
                        <div style={{ fontSize: '0.75rem', fontFamily: 'var(--font-mono)', color: '#52c41a' }}>
                          {`∫ f(x)dx ≈ ${calculatedIntegralValue.toFixed(4)}`}
                        </div>
                      )}
                    </div>
                  )}
                </div>

                {/* Riemann sums toggle */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '0.85rem' }}>
                    <input
                      type="checkbox"
                      checked={showRiemann}
                      onChange={(e) => setShowRiemann(e.target.checked)}
                    />
                    <span>Show Riemann Sums</span>
                  </label>

                  {showRiemann && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', paddingLeft: '20px' }}>
                      <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                        <span style={{ fontSize: '0.75rem', color: 'rgba(255,255,255,0.5)' }}>Intervals:</span>
                        <input
                          type="number"
                          min="2"
                          max="100"
                          value={riemannIntervals}
                          onChange={(e) => setRiemannIntervals(Math.max(2, parseInt(e.target.value) || 2))}
                          style={{
                            width: '50px',
                            background: '#1f1f1f',
                            border: '1px solid rgba(255,255,255,0.1)',
                            color: '#fff',
                            borderRadius: '4px',
                            fontSize: '0.75rem',
                            padding: '2px 4px',
                          }}
                        />
                      </div>
                      <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                        <span style={{ fontSize: '0.75rem', color: 'rgba(255,255,255,0.5)' }}>Type:</span>
                        <select
                          value={riemannType}
                          onChange={(e) => setRiemannType(e.target.value as any)}
                          style={{
                            background: '#1f1f1f',
                            border: '1px solid rgba(255,255,255,0.1)',
                            color: '#fff',
                            borderRadius: '4px',
                            fontSize: '0.75rem',
                          }}
                        >
                          <option value="left">Left Sum</option>
                          <option value="right">Right Sum</option>
                          <option value="midpoint">Midpoint Sum</option>
                        </select>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}
            {/* CAS inspector */}
            {activeMode === 'explicit' && equations.filter(eq => eq.visible && eq.expr.trim() !== '').length > 0 && (
              <div
                style={{
                  borderTop: '1px solid rgba(255, 255, 255, 0.08)',
                  paddingTop: '12px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '10px',
                }}
              >
                <h4 style={{ margin: 0, fontSize: '0.9rem', color: 'var(--text-accent)' }}>CAS Inspector</h4>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {equations
                    .filter((eq) => eq.visible && eq.expr.trim() !== '')
                    .map((eq) => {
                      let simplified = 'N/A';
                      let derivative = 'N/A';
                      try {
                        simplified = simplifyExpr(eq.expr);
                        derivative = differentiateExpr(eq.expr, 'x');
                      } catch (err) {}

                      return (
                        <div
                          key={`cas-${eq.id}`}
                          style={{
                            background: 'rgba(255, 255, 255, 0.02)',
                            border: '1px solid rgba(255, 255, 255, 0.06)',
                            borderRadius: '6px',
                            padding: '8px',
                            fontSize: '0.75rem',
                            fontFamily: 'var(--font-mono)',
                          }}
                        >
                          <div style={{ color: eq.color, fontWeight: 'bold', marginBottom: '4px' }}>
                            f(x) = {eq.expr}
                          </div>
                          <div style={{ color: 'rgba(255,255,255,0.85)' }}>
                            <span style={{ color: 'var(--text-accent)' }}>Simplify:</span> {simplified}
                          </div>
                          <div style={{ color: 'rgba(255,255,255,0.85)', marginTop: '2px' }}>
                            <span style={{ color: 'var(--text-accent)' }}>d/dx:</span> {derivative}
                          </div>
                        </div>
                      );
                    })}
                </div>
              </div>
            )}
          </div>
          
          {/* Instructions */}
          <div
            style={{
              marginTop: '15px',
              fontSize: '0.75rem',
              color: 'rgba(255, 255, 255, 0.35)',
              borderTop: '1px solid rgba(255, 255, 255, 0.06)',
              paddingTop: '8px',
              lineHeight: '1.4',
            }}
          >
            <strong>Interactions:</strong>
            <ul style={{ margin: '4px 0 0 0', paddingLeft: '16px' }}>
              {activeMode === '3d' ? (
                <li>Drag to orbit the 3D surface</li>
              ) : (
                <>
                  <li>Drag canvas to pan view</li>
                  <li>Use mouse wheel to zoom</li>
                  <li>Hover curves to trace coords</li>
                </>
              )}
            </ul>
          </div>
        </div>

        {/* Main Graph Content Area */}
        <div style={{ flex: 1, height: '100%' }}>
          {activeMode === '3d' ? (
            <Graph3D expr={equation3D} params={parameterValues} />
          ) : (
            <GraphCanvas
              viewport={viewport}
              pan={pan}
              zoom={zoom}
              resetViewport={resetViewport}
              curves={curvesToRender}
              toScreenX={toScreenX}
              toScreenY={toScreenY}
              toMathX={toMathX}
              implicitCurves={implicitCurves}
              regressionPoints={regressionPoints}
              showTangent={showTangent}
              showIntegral={showIntegral}
              integralBounds={integralBounds}
              showRiemann={showRiemann}
              riemannIntervals={riemannIntervals}
              riemannType={riemannType}
              parametricCurves={parametricCurves}
            />
          )}
        </div>
      </div>
    </div>
  );
};
