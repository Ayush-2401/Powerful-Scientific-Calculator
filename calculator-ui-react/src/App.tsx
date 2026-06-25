import { useState, useEffect } from 'react';
import './App.css';

// Database of Scientific Constants matching constants.json
const CONSTANTS = {
  Mathematical: [
    { name: "PI", value: "3.141592653589793", desc: "Pi: Circle circumference/diameter ratio" },
    { name: "E", value: "2.718281828459045", desc: "Euler's number: Base of natural log" },
    { name: "PHI", value: "1.618033988749895", desc: "Golden Ratio" }
  ],
  Physical: [
    { name: "C", value: "299792458", desc: "Speed of light in vacuum (m/s)" },
    { name: "G_GRAV", value: "6.6743e-11", desc: "Newtonian gravity constant" },
    { name: "H_PLANCK", value: "6.62607015e-34", desc: "Planck constant (J·s)" },
    { name: "KB", value: "1.380649e-23", desc: "Boltzmann constant (J/K)" },
    { name: "E_CHARGE", value: "1.602176634e-19", desc: "Elementary charge (C)" },
    { name: "ME", value: "9.1093837015e-31", desc: "Electron mass (kg)" },
    { name: "MP", value: "1.67262192e-27", desc: "Proton mass (kg)" },
    { name: "MN", value: "1.67492749e-27", desc: "Neutron mass (kg)" },
    { name: "A0", value: "5.29177210e-11", desc: "Bohr radius (m)" },
    { name: "RY", value: "10973731.568", desc: "Rydberg constant (1/m)" },
    { name: "SIGMA", value: "5.670374419e-8", desc: "Stefan-Boltzmann constant" },
    { name: "G0", value: "9.80665", desc: "Standard gravity (m/s²)" }
  ],
  Chemical: [
    { name: "NA", value: "6.02214076e23", desc: "Avogadro constant (1/mol)" },
    { name: "R", value: "8.314462618", desc: "Molar gas constant (J/(mol·K))" },
    { name: "F", value: "96485.33212", desc: "Faraday constant (C/mol)" },
    { name: "U", value: "1.6605390666e-27", desc: "Atomic mass unit (kg)" }
  ]
};

// Units definitions for Converter Category
const CONVERTER_UNITS = {
  temperature: ["Celsius", "Fahrenheit", "Kelvin"],
  pressure: ["Pascal", "Bar", "Atm"],
  energy: ["Joules", "Calories", "kWh"],
  base: ["decimal", "binary", "octal", "hexadecimal"]
};

const actionRow = ["DEG/RAD", "C", "CE", "<--", "+/-", "(", ")"];
const memoryRow = ["MC", "MR", "M+", "M-"];
const scienceRow = ["sin", "cos", "tan", "log", "ln", "x^y", "PI", "E"];
const numpadRows = [
  ["7", "8", "9", "/"],
  ["4", "5", "6", "*"],
  ["1", "2", "3", "-"],
  ["0", ".", "", "+"]
];

interface GraphPoint {
  x: number;
  y: number;
}

function App() {
  const [primaryDisplay, setPrimaryDisplay] = useState("0");
  const [secondaryDisplay, setSecondaryDisplay] = useState("0");
  const [sessionId, setSessionId] = useState("");
  const [isDegreesMode, setIsDegreesMode] = useState(true);

  // Active Tab Workspace: calculator | solvers | grapher | matrices | converter
  const [activeTab, setActiveTab] = useState<'calculator' | 'solvers' | 'grapher' | 'matrices' | 'converter'>('calculator');

  // History Panels
  const [history, setHistory] = useState<string[]>([]);
  
  // Solver State (Quadratic / Cubic)
  const [solverMode, setSolverMode] = useState<'quadratic' | 'cubic'>('quadratic');
  const [solverInputs, setSolverInputs] = useState({ a: "", b: "", c: "", d: "" });
  const [solverResults, setSolverResults] = useState("");
  const [focusedField, setFocusedField] = useState<'a' | 'b' | 'c' | 'd' | null>(null);

  // 1. Grapher State
  const [graphExpression, setGraphExpression] = useState("sin(x) * x");
  const [xmin, setXmin] = useState("-10");
  const [xmax, setXmax] = useState("10");
  const [graphStep, setGraphStep] = useState("0.1");
  const [graphPoints, setGraphPoints] = useState<GraphPoint[]>([]);
  const [graphHoverPoint, setGraphHoverPoint] = useState<GraphPoint | null>(null);
  const [hoverPosition, setHoverPosition] = useState({ x: 0, y: 0 });
  const [graphLoading, setGraphLoading] = useState(false);
  const [focusedGraphField, setFocusedGraphField] = useState<'xmin' | 'xmax' | 'step' | null>(null);

  // 2. Matrix State
  const [matrixRowsA, setMatrixRowsA] = useState<number>(3);
  const [matrixColsA, setMatrixColsA] = useState<number>(3);
  const [matrixRowsB, setMatrixRowsB] = useState<number>(3);
  const [matrixColsB, setMatrixColsB] = useState<number>(3);
  
  const [matrixA, setMatrixA] = useState<string[][]>(Array(8).fill(null).map(() => Array(8).fill("")));
  const [matrixB, setMatrixB] = useState<string[][]>(Array(8).fill(null).map(() => Array(8).fill("")));
  const [vectorB, setVectorB] = useState<string[]>(Array(8).fill(""));
  const [matrixOp, setMatrixOp] = useState<string>('multiply');
  const [matrixResult, setMatrixResult] = useState<any>(null);
  const [focusedMatrixCell, setFocusedMatrixCell] = useState<{ matrix: 'A' | 'B' | 'vector', row: number, col: number } | null>(null);
  
  // Matrix Parameters (power / scalar) and step toggles
  const [matrixPower, setMatrixPower] = useState("2");
  const [scalarK, setScalarK] = useState("2");
  const [showCofactorSteps, setShowCofactorSteps] = useState(false);
  const [showRowOpSteps, setShowRowOpSteps] = useState(false);

  // 3. Converter State
  const [convertCategory, setConvertCategory] = useState<'temperature' | 'pressure' | 'energy' | 'base'>('temperature');
  const [convertValue, setConvertValue] = useState("");
  const [convertFrom, setConvertFrom] = useState("Celsius");
  const [convertTo, setConvertTo] = useState("Fahrenheit");
  const [convertResult, setConvertResult] = useState("");
  const [convertLoading, setConvertLoading] = useState(false);
  const [focusedConverterField, setFocusedConverterField] = useState(false);

  useEffect(() => {
    // Generate or fetch session ID on mount
    let storedSessionId = localStorage.getItem('calculator_session_id');
    if (!storedSessionId) {
      storedSessionId = crypto.randomUUID();
      localStorage.setItem('calculator_session_id', storedSessionId);
    }
    setSessionId(storedSessionId);
  }, []);

  // Update Conversion automatically when inputs change
  useEffect(() => {
    if (!convertValue.trim()) {
      setConvertResult("");
      return;
    }
    const timer = setTimeout(() => {
      triggerConversion();
    }, 300); // Debounce conversions

    return () => clearTimeout(timer);
  }, [convertValue, convertFrom, convertTo, convertCategory]);

  // Synchronize Matrix constraints
  useEffect(() => {
    if (matrixOp === 'multiply') {
      setMatrixRowsB(matrixColsA);
    } else if (
      matrixOp === 'inverse' || 
      matrixOp === 'determinant' || 
      matrixOp === 'solve' ||
      matrixOp === 'minors' ||
      matrixOp === 'cofactors' ||
      matrixOp === 'adjugate' ||
      matrixOp === 'permanent' ||
      matrixOp === 'pseudo_inverse' ||
      matrixOp === 'cramer' ||
      matrixOp === 'lu_solve' ||
      matrixOp === 'lu' ||
      matrixOp === 'cholesky' ||
      matrixOp === 'eigen' ||
      matrixOp === 'char_poly' ||
      matrixOp === 'trace' ||
      matrixOp === 'power'
    ) {
      // Force square Matrix A for standard determinant, decompositions, inverse, or linear solving
      if (matrixRowsA !== matrixColsA) {
        setMatrixColsA(matrixRowsA);
      }
    }
  }, [matrixColsA, matrixRowsA, matrixOp]);

  const handleButtonClick = async (command: string) => {
    const currentExpression = secondaryDisplay;

    if (command === 'DEG/RAD') {
      setIsDegreesMode(prev => !prev);
    }

    try {
      const response = await fetch('http://localhost:8080/api/calculate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Session-ID': sessionId
        },
        body: JSON.stringify({ command }),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();

      if (data.error) {
        setPrimaryDisplay("Error");
        setSecondaryDisplay(data.error);
      } else {
        setPrimaryDisplay(data.primaryDisplay);
        setSecondaryDisplay(data.secondaryDisplay);

        if (command === '=') {
          if (data.primaryDisplay && data.primaryDisplay !== 'Error') {
            const cleanExpression = currentExpression.replace(/\s+/g, ' ').trim();
            const logEntry = `${cleanExpression} = ${data.primaryDisplay}`;
            setHistory(prev => {
              if (prev.includes(logEntry)) return prev;
              return [logEntry, ...prev];
            });
          }
        }
      }
    } catch (error) {
      console.error("Connection failed:", error);
      setPrimaryDisplay("Error");
      setSecondaryDisplay("Server connection failed");
    }
  };

  const handleSolverSubmit = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setSolverResults("Calculating...");

    let command = "";
    if (solverMode === 'quadratic') {
      const { a, b, c } = solverInputs;
      if (!a || !b || !c) {
        setSolverResults("Error: Please provide values for a, b, and c.");
        return;
      }
      command = `SOLVE_QUAD ${a},${b},${c}`;
    } else {
      const { a, b, c, d } = solverInputs;
      if (!a || !b || !c || !d) {
        setSolverResults("Error: Please provide values for a, b, c, and d.");
        return;
      }
      command = `SOLVE_CUBIC ${a},${b},${c},${d}`;
    }

    try {
      const response = await fetch('http://localhost:8080/api/calculate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Session-ID': sessionId
        },
        body: JSON.stringify({ command }),
      });

      const data = await response.json();
      if (data.error) {
        setSolverResults(data.error);
      } else {
        setSolverResults(data.primaryDisplay);
        const solverName = solverMode === 'quadratic' ? 'Quad Solver' : 'Cubic Solver';
        const coeffStr = solverMode === 'quadratic' 
          ? `[a=${solverInputs.a}, b=${solverInputs.b}, c=${solverInputs.c}]`
          : `[a=${solverInputs.a}, b=${solverInputs.b}, c=${solverInputs.c}, d=${solverInputs.d}]`;
        setHistory(prev => [`${solverName} ${coeffStr} → ${data.primaryDisplay}`, ...prev]);
      }
    } catch (error) {
      console.error("Solver connection failed:", error);
      setSolverResults("Connection to backend server failed.");
    }
  };

  // 1. Trigger Graphing API Plotting
  const triggerGraphing = async (e: React.FormEvent) => {
    e.preventDefault();
    setGraphLoading(true);
    setGraphHoverPoint(null);

    const payload = {
      expression: graphExpression,
      xmin: parseFloat(xmin) || -10,
      xmax: parseFloat(xmax) || 10,
      step: parseFloat(graphStep) || 0.1
    };

    try {
      const response = await fetch('http://localhost:8080/api/graph', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        throw new Error(`Graph error: ${response.status}`);
      }

      const data = await response.json();
      if (data.error) {
        alert(data.error);
      } else if (data.points) {
        setGraphPoints(data.points);
        setHistory(prev => [`Plotted: y = ${graphExpression} in [${xmin}, ${xmax}]`, ...prev]);
      }
    } catch (error) {
      console.error("Graph plotting failed:", error);
      alert("Failed to connect to backend graphing endpoint.");
    } finally {
      setGraphLoading(false);
    }
  };

  // 2. Trigger Matrix Algebra API
  const triggerMatrixOperation = async () => {
    setMatrixResult("Calculating...");

    const parseMat = (mat: string[][], rows: number, cols: number) => 
      mat.slice(0, rows).map(r => r.slice(0, cols).map(val => parseFloat(val) || 0));

    const parseVec = (vec: string[], size: number) => 
      vec.slice(0, size).map(val => parseFloat(val) || 0);

    const payload: any = {
      operation: matrixOp,
      matrixA: parseMat(matrixA, matrixRowsA, matrixColsA)
    };

    // Add extra params based on the operation selected
    if (matrixOp === 'multiply' || matrixOp === 'hadamard_product' || matrixOp === 'hadamard_division' || matrixOp === 'kronecker_product' || matrixOp === 'direct_sum') {
      payload.matrixB = parseMat(matrixB, matrixRowsB, matrixColsB);
    } else if (matrixOp === 'solve' || matrixOp === 'cramer' || matrixOp === 'lu_solve' || matrixOp === 'least_squares') {
      payload.vectorB = parseVec(vectorB, matrixRowsA);
    } else if (matrixOp === 'scalar_multiply') {
      payload.scalar = parseFloat(scalarK) || 1;
    } else if (matrixOp === 'power') {
      payload.power = parseInt(matrixPower) || 2;
    }

    // Add steps parameters
    if (matrixOp === 'determinant' || matrixOp === 'permanent' || matrixOp === 'minors' || matrixOp === 'cofactors') {
      payload.showCofactorSteps = showCofactorSteps;
    } else if (matrixOp === 'ref' || matrixOp === 'rref' || matrixOp === 'inverse' || matrixOp === 'solve') {
      payload.showRowOpSteps = showRowOpSteps;
    }

    try {
      const response = await fetch('http://localhost:8080/api/matrix', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const data = await response.json();
      if (data.error) {
        setMatrixResult(`Error: ${data.error}`);
      } else {
        setMatrixResult(data.result);
        const desc = `Matrix ${matrixOp.toUpperCase()} (${matrixRowsA}x${matrixColsA})`;
        setHistory(prev => [`${desc} performed successfully`, ...prev]);
      }
    } catch (error) {
      console.error("Matrix API failed:", error);
      setMatrixResult("Error: Backend connection failed.");
    }
  };

  // 3. Trigger Converter API
  const triggerConversion = async () => {
    setConvertLoading(true);
    const isBase = convertCategory === 'base';
    
    const payload = isBase 
      ? { type: "base", value: convertValue, from: convertFrom, to: convertTo }
      : { type: "unit", category: convertCategory, value: convertValue, from: convertFrom, to: convertTo };

    try {
      const response = await fetch('http://localhost:8080/api/convert', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const data = await response.json();
      if (data.error) {
        setConvertResult(data.error);
      } else {
        setConvertResult(data.result);
      }
    } catch (error) {
      console.error("Convert API failed:", error);
      setConvertResult("Error");
    } finally {
      setConvertLoading(false);
    }
  };

  // Client-Side Presets Filler
  const fillMatrixPreset = (target: 'A' | 'B', presetName: string) => {
    const rows = target === 'A' ? matrixRowsA : matrixRowsB;
    const cols = target === 'A' ? matrixColsA : matrixColsB;
    const newGrid = Array(8).fill(null).map(() => Array(8).fill(""));

    // Pascal symmetric binomial coefficients helper
    const binom = (n: number, k: number): number => {
      if (k < 0 || k > n) return 0;
      let res = 1;
      for (let i = 1; i <= k; i++) {
        res = res * (n - i + 1) / i;
      }
      return Math.round(res);
    };

    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        let val = "";
        if (presetName === 'Identity') {
          val = r === c ? "1" : "0";
        } else if (presetName === 'Zero') {
          val = "0";
        } else if (presetName === 'Random') {
          val = (Math.floor(Math.random() * 19) - 9).toString(); // range -9 to 9
        } else if (presetName === 'Hilbert') {
          val = (1 / (r + c + 1)).toFixed(5).replace(/\.?0+$/, "");
        } else if (presetName === 'Pascal') {
          val = binom(r + c, r).toString();
        } else if (presetName === 'Vandermonde') {
          val = Math.pow(r + 1, c).toString();
        }
        newGrid[r][c] = val;
      }
    }

    if (target === 'A') {
      setMatrixA(prev => {
        const next = prev.map(row => [...row]);
        for (let r = 0; r < 8; r++) {
          for (let c = 0; c < 8; c++) {
            next[r][c] = newGrid[r][c] || "";
          }
        }
        return next;
      });
    } else {
      setMatrixB(prev => {
        const next = prev.map(row => [...row]);
        for (let r = 0; r < 8; r++) {
          for (let c = 0; c < 8; c++) {
            next[r][c] = newGrid[r][c] || "";
          }
        }
        return next;
      });
    }
  };

  // Insert values into active field dynamically depending on tab
  const insertValueIntoActiveField = (val: string) => {
    if (activeTab === 'solvers' && focusedField) {
      setSolverInputs(prev => ({ ...prev, [focusedField]: val }));
    } else if (activeTab === 'grapher' && focusedGraphField) {
      if (focusedGraphField === 'xmin') setXmin(val);
      else if (focusedGraphField === 'xmax') setXmax(val);
      else if (focusedGraphField === 'step') setGraphStep(val);
    } else if (activeTab === 'converter' && focusedConverterField) {
      setConvertValue(val);
    } else if (activeTab === 'matrices' && focusedMatrixCell) {
      const { matrix, row, col } = focusedMatrixCell;
      if (matrix === 'A') {
        setMatrixA(prev => {
          const next = prev.map(r => [...r]);
          next[row][col] = val;
          return next;
        });
      } else if (matrix === 'B') {
        setMatrixB(prev => {
          const next = prev.map(r => [...r]);
          next[row][col] = val;
          return next;
        });
      } else if (matrix === 'vector') {
        setVectorB(prev => {
          const next = [...prev];
          next[row] = val;
          return next;
        });
      }
    }
  };

  // Click history entry to insert its result back into calculator display or solver input
  const handleHistoryClick = (logItem: string) => {
    let valueToInsert = "";
    if (logItem.includes('→')) {
      valueToInsert = logItem.split('→')[1].trim();
    } else if (logItem.includes('=')) {
      valueToInsert = logItem.split('=')[1].trim();
    }

    if (!valueToInsert || valueToInsert === "Error") return;

    if (activeTab === 'calculator') {
      handleButtonClick(valueToInsert);
    } else {
      insertValueIntoActiveField(valueToInsert);
    }
  };

  // Insert constants into either the active calculator display or focused solver inputs
  const handleConstantClick = (constName: string, constValue: string) => {
    if (activeTab === 'calculator') {
      handleButtonClick(constName);
    } else {
      insertValueIntoActiveField(constValue);
    }
  };

  // Switch Categories in Converter & reset defaults
  const handleConverterCategoryChange = (cat: 'temperature' | 'pressure' | 'energy' | 'base') => {
    setConvertCategory(cat);
    setConvertValue("");
    setConvertResult("");
    const units = CONVERTER_UNITS[cat];
    setConvertFrom(units[0]);
    setConvertTo(units[1]);
  };

  // Swap "From" and "To" units inside converter
  const swapConverterUnits = () => {
    const temp = convertFrom;
    setConvertFrom(convertTo);
    setConvertTo(temp);
    setConvertValue(convertResult);
  };

  // Dynamic calculations of SVG Chart bounding and coordinate mapping
  const renderGraphSvgCurve = () => {
    if (graphPoints.length === 0) return null;

    const width = 500;
    const height = 300;
    const padding = 35;

    const xVals = graphPoints.map(p => p.x);
    const yVals = graphPoints.map(p => p.y);

    const minX = Math.min(...xVals);
    const maxX = Math.max(...xVals);
    
    let minY = Math.min(...yVals);
    let maxY = Math.max(...yVals);
    const yRange = maxY - minY;
    if (yRange === 0) {
      minY -= 1;
      maxY += 1;
    } else {
      minY -= yRange * 0.08;
      maxY += yRange * 0.08;
    }

    const getSvgX = (x: number) => padding + ((x - minX) / (maxX - minX)) * (width - 2 * padding);
    const getSvgY = (y: number) => height - padding - ((y - minY) / (maxY - minY)) * (height - 2 * padding);

    const gridX = [];
    const gridY = [];
    for (let i = 0; i <= 6; i++) {
      const frac = i / 6;
      gridX.push(minX + frac * (maxX - minX));
      gridY.push(minY + frac * (maxY - minY));
    }

    const pathString = graphPoints
      .map((p, idx) => `${idx === 0 ? 'M' : 'L'} ${getSvgX(p.x).toFixed(1)} ${getSvgY(p.y).toFixed(1)}`)
      .join(' ');

    const handleSvgMouseMove = (e: React.MouseEvent<SVGSVGElement>) => {
      const rect = e.currentTarget.getBoundingClientRect();
      const clientX = e.clientX - rect.left;

      const rawX = minX + ((clientX - padding) / (width - 2 * padding)) * (maxX - minX);

      let closest: GraphPoint | null = null;
      let minDistance = Infinity;

      graphPoints.forEach(p => {
        const dist = Math.abs(p.x - rawX);
        if (dist < minDistance) {
          minDistance = dist;
          closest = p;
        }
      });

      if (closest) {
        setGraphHoverPoint(closest);
        setHoverPosition({ x: getSvgX((closest as GraphPoint).x), y: getSvgY((closest as GraphPoint).y) });
      }
    };

    const originX = getSvgX(0);
    const originY = getSvgY(0);

    return (
      <div style={{ position: 'relative', width: '100%', height: '100%' }}>
        <svg 
          className="graph-svg" 
          viewBox={`0 0 ${width} ${height}`}
          onMouseMove={handleSvgMouseMove}
          onMouseLeave={() => setGraphHoverPoint(null)}
        >
          {/* Vertical Gridlines */}
          {gridX.map((x, i) => (
            <line key={`x-${i}`} x1={getSvgX(x)} y1={padding} x2={getSvgX(x)} y2={height - padding} className="graph-gridline" />
          ))}
          {/* Horizontal Gridlines */}
          {gridY.map((y, i) => (
            <line key={`y-${i}`} x1={padding} y1={getSvgY(y)} x2={width - padding} y2={getSvgY(y)} className="graph-gridline" />
          ))}

          {/* Core Y-Axis Line */}
          {minX <= 0 && maxX >= 0 && (
            <line x1={originX} y1={padding} x2={originX} y2={height - padding} className="graph-axis" />
          )}
          {/* Core X-Axis Line */}
          {minY <= 0 && maxY >= 0 && (
            <line x1={padding} y1={originY} x2={width - padding} y2={originY} className="graph-axis" />
          )}

          {/* Coordinate Labels along Axis */}
          {gridX.map((x, i) => (
            <text key={`lx-${i}`} x={getSvgX(x)} y={height - padding + 15} textAnchor="middle" className="graph-axis-label">
              {x.toFixed(1)}
            </text>
          ))}
          {gridY.map((y, i) => (
            <text key={`ly-${i}`} x={padding - 8} y={getSvgY(y) + 4} textAnchor="end" className="graph-axis-label">
              {y.toFixed(1)}
            </text>
          ))}

          {/* Drawn Plot Curve */}
          <path d={pathString} className="graph-curve" />

          {/* Interactive hover tooltip dot marker */}
          {graphHoverPoint && (
            <circle 
              cx={hoverPosition.x} 
              cy={hoverPosition.y} 
              r="6" 
              className="graph-tooltip-marker" 
            />
          )}
        </svg>

        {/* Hover Tooltip Box */}
        {graphHoverPoint && (
          <div 
            className="graph-tooltip-box"
            style={{ 
              left: `${hoverPosition.x + 10}px`, 
              top: `${hoverPosition.y - 40}px` 
            }}
          >
            x: {graphHoverPoint.x.toFixed(2)}<br />
            y: {graphHoverPoint.y.toFixed(4)}
          </div>
        )}
      </div>
    );
  };

  const renderResultGrid = (grid: number[][]) => {
    return (
      <div className="matrix-bracket-wrapper">
        <div 
          className="matrix-input-grid" 
          style={{ 
            gridTemplateColumns: `repeat(${grid[0].length}, 1fr)`,
            gridTemplateRows: `repeat(${grid.length}, 1fr)` 
          }}
        >
          {grid.map((row, r) =>
            row.map((val, c) => (
              <div key={`${r}-${c}`} className="matrix-cell-input" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.85rem' }}>
                {typeof val === 'number' ? val.toFixed(4).replace(/\.?0+$/, '') : val}
              </div>
            ))
          )}
        </div>
      </div>
    );
  };

  const renderResultVector = (vec: number[]) => {
    return (
      <div className="matrix-bracket-wrapper">
        <div className="matrix-vector-grid" style={{ gridTemplateRows: `repeat(${vec.length}, 1fr)` }}>
          {vec.map((val, i) => (
            <div key={i} className="matrix-cell-input" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.85rem' }}>
              {typeof val === 'number' ? val.toFixed(4).replace(/\.?0+$/, '') : val}
            </div>
          ))}
        </div>
      </div>
    );
  };

  // Complex response result formatter
  const renderMatrixResult = () => {
    if (!matrixResult) return null;
    
    // 1. Plain String / Number
    if (typeof matrixResult === 'string' || typeof matrixResult === 'number') {
      return (
        <div style={{ fontSize: '1.2rem', fontWeight: 600, color: 'var(--text-accent)', textAlign: 'center', width: '100%' }}>
          {matrixResult}
        </div>
      );
    }

    // 2. Direct Arrays
    if (Array.isArray(matrixResult)) {
      if (matrixResult.length === 0) return <div>Empty Result</div>;
      const is1D = !Array.isArray(matrixResult[0]);
      if (is1D) {
        return renderResultVector(matrixResult as number[]);
      }
      return renderResultGrid(matrixResult as number[][]);
    }

    // 3. Object structures (decompositions, eigens, spaces, norms, steps)
    let mainResultNode = null;
    let stepsNode = null;

    const decompKeys = ['L', 'U', 'P', 'Q', 'R', 'Sigma', 'V', 'matrix'];
    const hasDecomp = Object.keys(matrixResult).some(k => decompKeys.includes(k) && Array.isArray(matrixResult[k]));

    if (hasDecomp) {
      // Decompositions rendering
      mainResultNode = (
        <div className="matrix-decomp-grid">
          {Object.entries(matrixResult).map(([key, val]) => {
            if (!Array.isArray(val)) return null;
            return (
              <div key={key} className="matrix-box">
                <h4>{key}</h4>
                {Array.isArray(val[0]) ? renderResultGrid(val as number[][]) : renderResultVector(val as number[])}
              </div>
            );
          })}
        </div>
      );
    } else if (matrixResult.eigenvalues || matrixResult.eigenvectors) {
      // Eigenvalues / Eigenvectors rendering
      mainResultNode = (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', width: '100%' }}>
          {matrixResult.charPolynomial && (
            <div style={{ marginBottom: '10px', textAlign: 'center' }}>
              <span style={{ color: 'var(--text-secondary)' }}>p(λ) = </span>
              <span style={{ fontFamily: 'var(--font-mono)', fontSize: '1.1rem', color: 'var(--text-accent)' }}>{matrixResult.charPolynomial}</span>
            </div>
          )}
          {matrixResult.eigenvalues && matrixResult.eigenvalues.map((λ: any, idx: number) => {
            const vector = matrixResult.eigenvectors ? matrixResult.eigenvectors[idx] : null;
            return (
              <div key={idx} className="matrix-property-card" style={{ width: '100%', flexDirection: 'row', justifyContent: 'space-between', padding: '10px 20px' }}>
                <div>
                  <span style={{ color: 'var(--text-secondary)' }}>λ_{idx+1} = </span>
                  <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-accent)', fontWeight: 600 }}>
                    {typeof λ === 'number' ? λ.toFixed(4).replace(/\.?0+$/, '') : λ}
                  </span>
                </div>
                {vector && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ color: 'var(--text-secondary)' }}>v_{idx+1}: </span>
                    {renderResultVector(vector)}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      );
    } else if (matrixResult.basis || matrixResult.basisVectors) {
      // Subspace basis vectors
      const vectors = matrixResult.basis || matrixResult.basisVectors;
      mainResultNode = (
        <div className="matrix-decomp-grid">
          {vectors.length === 0 ? (
            <div style={{ fontStyle: 'italic', color: 'var(--text-secondary)' }}>Empty Set (Zero Subspace basis)</div>
          ) : (
            vectors.map((vec: number[], idx: number) => (
              <div key={idx} className="matrix-box">
                <h4>v_{idx+1}</h4>
                {renderResultVector(vec)}
              </div>
            ))
          )}
        </div>
      );
    } else if (matrixResult.norms || matrixResult.properties) {
      // Properties norms rendering
      const props = matrixResult.norms || matrixResult.properties;
      mainResultNode = (
        <div className="matrix-properties-grid">
          {Object.entries(props).map(([key, val]: [string, any]) => (
            <div key={key} className="matrix-property-card">
              <span className="matrix-property-label">{key.replace(/_/g, ' ')}</span>
              <span className="matrix-property-val">
                {typeof val === 'number' ? val.toFixed(4).replace(/\.?0+$/, '') : val.toString()}
              </span>
            </div>
          ))}
        </div>
      );
    } else {
      // Generic dictionary response
      mainResultNode = (
        <div className="matrix-properties-grid">
          {Object.entries(matrixResult).map(([key, val]: [string, any]) => {
            if (key === 'steps' || key === 'stepsList') return null;
            return (
              <div key={key} className="matrix-property-card">
                <span className="matrix-property-label">{key.replace(/_/g, ' ')}</span>
                <span className="matrix-property-val">
                  {Array.isArray(val) 
                    ? (Array.isArray(val[0]) ? "[Matrix]" : "[Vector]") 
                    : (typeof val === 'number' ? val.toFixed(4).replace(/\.?0+$/, '') : val.toString())
                  }
                </span>
              </div>
            );
          })}
        </div>
      );
    }

    // Steps list rendering
    const steps = matrixResult.steps || matrixResult.stepsList;
    if (Array.isArray(steps) && steps.length > 0) {
      stepsNode = (
        <div className="matrix-steps-card">
          <h5>Derivation / Expansion Steps</h5>
          {steps.map((step: string, idx: number) => (
            <div key={idx} className="matrix-step-item">
              {step}
            </div>
          ))}
        </div>
      );
    }

    return (
      <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: '15px' }}>
        {mainResultNode}
        {stepsNode}
      </div>
    );
  };

  return (
    <div className="app-container">
      {/* HEADER SECTION */}
      <header className="app-header">
        <div className="brand-section">
          <h1>Antigravity Calc</h1>
          <span>Scientific Workspace</span>
        </div>

        <nav className="nav-tabs">
          <button 
            className={`tab-btn ${activeTab === 'calculator' ? 'active' : ''}`}
            onClick={() => setActiveTab('calculator')}
          >
            Calculator
          </button>
          <button 
            className={`tab-btn ${activeTab === 'solvers' ? 'active' : ''}`}
            onClick={() => setActiveTab('solvers')}
          >
            Solvers
          </button>
          <button 
            className={`tab-btn ${activeTab === 'grapher' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('grapher');
              if (graphPoints.length === 0) setGraphPoints([]);
            }}
          >
            Function Grapher
          </button>
          <button 
            className={`tab-btn ${activeTab === 'matrices' ? 'active' : ''}`}
            onClick={() => setActiveTab('matrices')}
          >
            Matrix Workspace
          </button>
          <button 
            className={`tab-btn ${activeTab === 'converter' ? 'active' : ''}`}
            onClick={() => setActiveTab('converter')}
          >
            Converter
          </button>
        </nav>

        {/* COMPACT GLOBAL CONSTANTS DROPDOWN */}
        <div style={{ display: 'flex', gap: '15px', alignItems: 'center' }}>
          <select 
            className="converter-select" 
            defaultValue=""
            onChange={e => {
              const val = e.target.value;
              if (!val) return;
              let foundName = "";
              let foundValue = "";
              Object.values(CONSTANTS).flat().forEach(c => {
                if (c.name === val) {
                  foundName = c.name;
                  foundValue = c.value;
                }
              });
              if (foundName) handleConstantClick(foundName, foundValue);
              e.target.value = ""; // Reset index placeholder
            }}
            style={{ padding: '6px 12px', fontSize: '0.85rem', width: '130px' }}
          >
            <option value="">Constants...</option>
            {Object.entries(CONSTANTS).map(([category, consts]) => (
              <optgroup key={category} label={category}>
                {consts.map(c => (
                  <option key={c.name} value={c.name}>
                    {c.name} ({parseFloat(c.value) > 1e6 || parseFloat(c.value) < 1e-4 ? c.value : Number(c.value).toLocaleString()})
                  </option>
                ))}
              </optgroup>
            ))}
          </select>

          <div className="status-badge">
            <div className="status-dot"></div>
            <span>API Connected</span>
          </div>
        </div>
      </header>

      {/* CORE WORKSPACE */}
      <main className="workspace">
        {/* LEFT COLUMN: HISTORY PANEL */}
        <aside className="sidebar-panel">
          <h2>
            History
            {history.length > 0 && (
              <button className="clear-btn" onClick={() => setHistory([])}>Clear</button>
            )}
          </h2>
          <div className="history-list">
            {history.length === 0 ? (
              <p className="empty-state">No history logged yet</p>
            ) : (
              history.map((item, idx) => (
                <div key={idx} className="history-card" onClick={() => handleHistoryClick(item)}>
                  <div className="hist-expr">
                    {item.includes('→') ? item.split('→')[0].trim() : item.includes('=') ? item.split('=')[0].trim() : item}
                  </div>
                  <div className="hist-res">
                    {item.includes('→') ? item.split('→')[1].trim() : item.includes('=') ? item.split('=')[1].trim() : ''}
                  </div>
                </div>
              ))
            )}
          </div>
        </aside>

        {/* CENTER COLUMN: ACTIVE TAB AREA */}
        <section className="center-panel">
          
          {/* DISPLAY CONTAINER */}
          {activeTab === 'calculator' && (
            <div className="calculator-display">
              <div className="display-indicators">
                <span>{isDegreesMode ? "DEG" : "RAD"}</span>
                <span>Scientific Mode</span>
              </div>
              <div className="display-secondary">{secondaryDisplay}</div>
              <div className="display-primary">{primaryDisplay}</div>
            </div>
          )}

          {/* DYNAMIC TAB CONTROLLER */}
          <div className="calculator-grid-wrapper" style={{ flex: 1 }}>
            
            {activeTab === 'calculator' && (
              /* TAB 1: SCIENTIFIC CALCULATOR */
              <div className="calculator-keyboard">
                <div className="keyboard-row-group">
                  {actionRow.map(btn => (
                    <button 
                      key={btn} 
                      onClick={() => handleButtonClick(btn)}
                      className={`btn ${btn === 'C' || btn === 'CE' ? 'btn-clear' : btn === 'DEG/RAD' ? 'btn-sci' : 'btn-op'}`}
                    >
                      {btn}
                    </button>
                  ))}
                </div>

                <div className="keyboard-row-group">
                  {memoryRow.map(btn => (
                    <button 
                      key={btn} 
                      onClick={() => handleButtonClick(btn)}
                      className="btn btn-op"
                    >
                      {btn}
                    </button>
                  ))}
                  <button className="btn btn-sci" onClick={() => handleButtonClick("sqrt")}>sqrt</button>
                  <button className="btn btn-sci" onClick={() => handleButtonClick("1/x")}>1/x</button>
                  <button className="btn btn-sci" onClick={() => handleButtonClick("!")}>!</button>
                </div>

                <div style={{ display: 'flex', gap: '10px' }}>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '8px', flex: '0 0 160px' }}>
                    {scienceRow.map(btn => (
                      <button 
                        key={btn} 
                        onClick={() => handleButtonClick(btn)}
                        className="btn btn-sci"
                      >
                        {btn}
                      </button>
                    ))}
                    <button className="btn btn-sci" onClick={() => handleButtonClick("%")}>%</button>
                  </div>

                  <div style={{ display: 'flex', gap: '8px', flex: 1 }}>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '8px', flex: 1 }}>
                      {numpadRows.map((row) => 
                        row.map(btn => (
                          <button
                            key={btn}
                            onClick={() => handleButtonClick(btn)}
                            className={`btn ${['/', '*', '-', '+'].includes(btn) ? 'btn-op' : 'btn-num'}`}
                          >
                            {btn}
                          </button>
                        ))
                      )}
                    </div>
                    <button 
                      onClick={() => handleButtonClick("=")} 
                      className="btn btn-equal"
                      style={{ width: '60px', height: 'auto' }}
                    >
                      =
                    </button>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'solvers' && (
              /* TAB 2: EQUATION SOLVERS */
              <div className="solvers-container">
                <div className="solver-selector">
                  <button 
                    className={`solver-tab-btn ${solverMode === 'quadratic' ? 'active' : ''}`}
                    onClick={() => {
                      setSolverMode('quadratic');
                      setSolverResults("");
                      setSolverInputs({ a: "", b: "", c: "", d: "" });
                    }}
                  >
                    Quadratic (ax² + bx + c)
                  </button>
                  <button 
                    className={`solver-tab-btn ${solverMode === 'cubic' ? 'active' : ''}`}
                    onClick={() => {
                      setSolverMode('cubic');
                      setSolverResults("");
                      setSolverInputs({ a: "", b: "", c: "", d: "" });
                    }}
                  >
                    Cubic (ax³ + bx² + cx + d)
                  </button>
                </div>

                <form className="solver-form" onSubmit={handleSolverSubmit}>
                  <h3>
                    {solverMode === 'quadratic' 
                      ? "Quadratic Solver (ax² + bx + c = 0)"
                      : "Cubic Solver (ax³ + bx² + cx + d = 0)"
                    }
                  </h3>

                  <div className="solver-inputs-grid">
                    <div className="solver-input-group">
                      <label>Coeff a</label>
                      <input 
                        type="text" 
                        placeholder="a" 
                        value={solverInputs.a}
                        onChange={e => setSolverInputs(prev => ({ ...prev, a: e.target.value }))}
                        onFocus={() => setFocusedField('a')}
                      />
                    </div>
                    <div className="solver-input-group">
                      <label>Coeff b</label>
                      <input 
                        type="text" 
                        placeholder="b" 
                        value={solverInputs.b}
                        onChange={e => setSolverInputs(prev => ({ ...prev, b: e.target.value }))}
                        onFocus={() => setFocusedField('b')}
                      />
                    </div>
                    <div className="solver-input-group">
                      <label>Coeff c</label>
                      <input 
                        type="text" 
                        placeholder="c" 
                        value={solverInputs.c}
                        onChange={e => setSolverInputs(prev => ({ ...prev, c: e.target.value }))}
                        onFocus={() => setFocusedField('c')}
                      />
                    </div>
                    {solverMode === 'cubic' && (
                      <div className="solver-input-group">
                        <label>Coeff d</label>
                        <input 
                          type="text" 
                          placeholder="d" 
                          value={solverInputs.d}
                          onChange={e => setSolverInputs(prev => ({ ...prev, d: e.target.value }))}
                          onFocus={() => setFocusedField('d')}
                        />
                      </div>
                    )}
                  </div>

                  <button type="submit" className="solver-submit-btn">
                    Solve Equation
                  </button>
                </form>

                {solverResults && (
                  <div className="solver-results">
                    <h4>Equation Roots</h4>
                    <div className="solver-roots">{solverResults}</div>
                  </div>
                )}
              </div>
            )}

            {activeTab === 'grapher' && (
              /* TAB 3: FUNCTION GRAPHER */
              <div className="grapher-container">
                <form className="grapher-form" onSubmit={triggerGraphing}>
                  <div className="graph-inputs-row">
                    <div className="graph-input-group">
                      <label>Function f(x)</label>
                      <input 
                        type="text" 
                        value={graphExpression} 
                        onChange={e => setGraphExpression(e.target.value)} 
                        placeholder="e.g. sin(x) * x"
                      />
                    </div>
                    <div className="graph-input-group">
                      <label>Min X</label>
                      <input 
                        type="text" 
                        value={xmin} 
                        onChange={e => setXmin(e.target.value)}
                        onFocus={() => setFocusedGraphField('xmin')}
                        onBlur={() => setFocusedGraphField(null)}
                      />
                    </div>
                    <div className="graph-input-group">
                      <label>Max X</label>
                      <input 
                        type="text" 
                        value={xmax} 
                        onChange={e => setXmax(e.target.value)}
                        onFocus={() => setFocusedGraphField('xmax')}
                        onBlur={() => setFocusedGraphField(null)}
                      />
                    </div>
                    <button type="submit" className="graph-btn" disabled={graphLoading}>
                      {graphLoading ? "Ploting..." : "Plot"}
                    </button>
                  </div>
                </form>

                <div className="graph-display-wrapper">
                  {graphPoints.length === 0 ? (
                    <div className="empty-state">Enter an expression and click plot to render graph</div>
                  ) : renderGraphSvgCurve()}
                </div>
              </div>
            )}

            {activeTab === 'matrices' && (
              /* TAB 4: MATRIX ALGEBRA WORKSPACE (UP TO 8x8) */
              <div className="matrix-workspace-container">
                <div className="matrix-workspace-controls">
                  
                  {/* MATRIX A ROWS & COLUMNS SELECTORS (1-8) */}
                  <div style={{ display: 'flex', gap: '15px', alignItems: 'center' }}>
                    <div style={{ display: 'flex', gap: '4px', alignItems: 'center' }}>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>A:</span>
                      <select 
                        className="converter-select" 
                        style={{ padding: '3px 6px', fontSize: '0.8rem', width: '55px' }}
                        value={matrixRowsA}
                        onChange={e => {
                          const val = parseInt(e.target.value);
                          setMatrixRowsA(val);
                          setMatrixResult(null);
                        }}
                      >
                        {[1, 2, 3, 4, 5, 6, 7, 8].map(n => <option key={n} value={n}>{n} R</option>)}
                      </select>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>×</span>
                      <select 
                        className="converter-select" 
                        style={{ padding: '3px 6px', fontSize: '0.8rem', width: '55px' }}
                        value={matrixColsA}
                        onChange={e => {
                          const val = parseInt(e.target.value);
                          setMatrixColsA(val);
                          setMatrixResult(null);
                        }}
                      >
                        {[1, 2, 3, 4, 5, 6, 7, 8].map(n => <option key={n} value={n}>{n} C</option>)}
                      </select>
                    </div>

                    {/* MATRIX B COLUMNS SELECTOR (1-8) */}
                    {matrixOp === 'multiply' && (
                      <div style={{ display: 'flex', gap: '4px', alignItems: 'center' }}>
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>B:</span>
                        <span className="status-badge" style={{ padding: '3px 6px', fontSize: '0.8rem', border: '1px solid rgba(255,255,255,0.08)' }}>
                          {matrixRowsB} R
                        </span>
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>×</span>
                        <select 
                          className="converter-select" 
                          style={{ padding: '3px 6px', fontSize: '0.8rem', width: '55px' }}
                          value={matrixColsB}
                          onChange={e => {
                            setMatrixColsB(parseInt(e.target.value));
                            setMatrixResult(null);
                          }}
                        >
                          {[1, 2, 3, 4, 5, 6, 7, 8].map(n => <option key={n} value={n}>{n} C</option>)}
                        </select>
                      </div>
                    )}
                  </div>

                  {/* ADVANCED OPERATIONS LIST DROPDOWN */}
                  <select 
                    className="converter-select" 
                    style={{ padding: '4px 10px', fontSize: '0.85rem' }}
                    value={matrixOp}
                    onChange={e => {
                      setMatrixOp(e.target.value);
                      setMatrixResult(null);
                    }}
                  >
                    <optgroup label="Arithmetic">
                      <option value="multiply">A × B (Multiplication)</option>
                      <option value="scalar_multiply">k · A (Scalar Product)</option>
                      <option value="hadamard_product">A ⊙ B (Hadamard Product)</option>
                      <option value="hadamard_division">A ÷ B (Hadamard Division)</option>
                      <option value="kronecker_product">A ⊗ B (Kronecker Product)</option>
                      <option value="direct_sum">A ⊕ B (Direct Sum)</option>
                      <option value="power">Aⁿ (Matrix Power)</option>
                    </optgroup>
                    <optgroup label="Determinants">
                      <option value="determinant">det(A) (Determinant)</option>
                      <option value="minors">Minors Matrix</option>
                      <option value="cofactors">Cofactors Matrix</option>
                      <option value="adjugate">adj(A) (Adjugate)</option>
                      <option value="permanent">perm(A) (Permanent)</option>
                    </optgroup>
                    <optgroup label="Generalized Inverses">
                      <option value="inverse">A⁻¹ (Inverse)</option>
                      <option value="pseudo_inverse">A⁺ (Moore-Penrose Inverse)</option>
                      <option value="left_inverse">Left Inverse</option>
                      <option value="right_inverse">Right Inverse</option>
                    </optgroup>
                    <optgroup label="Row Operations">
                      <option value="ref">REF (Echelon Form)</option>
                      <option value="rref">RREF (Reduced Echelon Form)</option>
                      <option value="rank">Rank</option>
                      <option value="nullity">Nullity</option>
                    </optgroup>
                    <optgroup label="Subspaces">
                      <option value="null_space">Null Space (Kernel)</option>
                      <option value="column_space">Column Space</option>
                      <option value="row_space">Row Space</option>
                      <option value="left_null_space">Left Null Space</option>
                    </optgroup>
                    <optgroup label="Solvers">
                      <option value="solve">A × X = B (Solve System)</option>
                      <option value="cramer">Cramer's Rule</option>
                      <option value="lu_solve">LU Solve</option>
                      <option value="least_squares">Least Squares (A⁺b)</option>
                    </optgroup>
                    <optgroup label="Decompositions">
                      <option value="lu">LU Decomposition</option>
                      <option value="qr">QR Decomposition</option>
                      <option value="cholesky">Cholesky Decomposition</option>
                    </optgroup>
                    <optgroup label="Eigenvalues & Eigenvectors">
                      <option value="eigen">Eigenvalues / Eigenvectors</option>
                      <option value="char_poly">Characteristic Polynomial</option>
                    </optgroup>
                    <optgroup label="Properties & Norms">
                      <option value="trace">tr(A) (Trace)</option>
                      <option value="norms">Matrix Norms</option>
                      <option value="auto_detect_type">Type Auto-Detection</option>
                    </optgroup>
                  </select>
                </div>

                {/* EXTRA PARAMETER FIELDS OR STEPS CHECKBOXES */}
                {(matrixOp === 'power' || matrixOp === 'scalar_multiply' || ['determinant', 'permanent', 'minors', 'cofactors', 'ref', 'rref', 'inverse', 'solve'].includes(matrixOp)) && (
                  <div className="matrix-param-row">
                    {matrixOp === 'power' && (
                      <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Power n:</span>
                        <input 
                          type="number" 
                          className="matrix-param-input" 
                          value={matrixPower} 
                          onChange={e => setMatrixPower(e.target.value)} 
                        />
                      </div>
                    )}
                    {matrixOp === 'scalar_multiply' && (
                      <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Scalar k:</span>
                        <input 
                          type="text" 
                          className="matrix-param-input" 
                          value={scalarK} 
                          onChange={e => setScalarK(e.target.value)} 
                        />
                      </div>
                    )}
                    {['determinant', 'permanent', 'minors', 'cofactors'].includes(matrixOp) && (
                      <label className="matrix-checkbox-group">
                        <input 
                          type="checkbox" 
                          checked={showCofactorSteps} 
                          onChange={e => setShowCofactorSteps(e.target.checked)} 
                        />
                        Show Cofactor Expansion Steps
                      </label>
                    )}
                    {['ref', 'rref', 'inverse', 'solve'].includes(matrixOp) && (
                      <label className="matrix-checkbox-group">
                        <input 
                          type="checkbox" 
                          checked={showRowOpSteps} 
                          onChange={e => setShowRowOpSteps(e.target.checked)} 
                        />
                        Show Step-by-Step Row Operations
                      </label>
                    )}
                  </div>
                )}

                <div className="matrix-grids-container">
                  {/* MATRIX A */}
                  <div className="matrix-box">
                    <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center', marginBottom: '4px' }}>
                      <h4>Matrix A ({matrixRowsA}×{matrixColsA})</h4>
                      <select 
                        className="converter-select" 
                        style={{ padding: '3px 6px', fontSize: '0.75rem', width: '110px' }}
                        defaultValue=""
                        onChange={e => {
                          if (e.target.value) {
                            fillMatrixPreset('A', e.target.value);
                            e.target.value = "";
                          }
                        }}
                      >
                        <option value="">Fill Preset...</option>
                        <option value="Identity">Identity</option>
                        <option value="Zero">Zero</option>
                        <option value="Random">Random</option>
                        <option value="Hilbert">Hilbert</option>
                        <option value="Pascal">Pascal</option>
                        <option value="Vandermonde">Vandermonde</option>
                      </select>
                    </div>
                    <div className="matrix-bracket-wrapper">
                      <div 
                        className="matrix-input-grid"
                        style={{ 
                          gridTemplateColumns: `repeat(${matrixColsA}, 1fr)`,
                          gridTemplateRows: `repeat(${matrixRowsA}, 1fr)` 
                        }}
                      >
                        {Array(matrixRowsA).fill(null).map((_, r) =>
                          Array(matrixColsA).fill(null).map((_, c) => (
                            <input 
                              key={`A-${r}-${c}`}
                              type="text" 
                              className="matrix-cell-input"
                              placeholder={`A${r+1}${c+1}`}
                              value={matrixA[r][c]}
                              onChange={e => {
                                const val = e.target.value;
                                setMatrixA(prev => {
                                  const next = prev.map(row => [...row]);
                                  next[r][c] = val;
                                  return next;
                                });
                              }}
                              onFocus={() => setFocusedMatrixCell({ matrix: 'A', row: r, col: c })}
                            />
                          ))
                        )}
                      </div>
                    </div>
                  </div>

                  {/* MATRIX B */}
                  {(matrixOp === 'multiply' || matrixOp === 'hadamard_product' || matrixOp === 'hadamard_division' || matrixOp === 'kronecker_product' || matrixOp === 'direct_sum') && (
                    <div className="matrix-box">
                      <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center', marginBottom: '4px' }}>
                        <h4>Matrix B ({matrixRowsB}×{matrixColsB})</h4>
                        <select 
                          className="converter-select" 
                          style={{ padding: '3px 6px', fontSize: '0.75rem', width: '110px' }}
                          defaultValue=""
                          onChange={e => {
                            if (e.target.value) {
                              fillMatrixPreset('B', e.target.value);
                              e.target.value = "";
                            }
                          }}
                        >
                          <option value="">Fill Preset...</option>
                          <option value="Identity">Identity</option>
                          <option value="Zero">Zero</option>
                          <option value="Random">Random</option>
                          <option value="Hilbert">Hilbert</option>
                          <option value="Pascal">Pascal</option>
                          <option value="Vandermonde">Vandermonde</option>
                        </select>
                      </div>
                      <div className="matrix-bracket-wrapper">
                        <div 
                          className="matrix-input-grid"
                          style={{ 
                            gridTemplateColumns: `repeat(${matrixColsB}, 1fr)`,
                            gridTemplateRows: `repeat(${matrixRowsB}, 1fr)` 
                          }}
                        >
                          {Array(matrixRowsB).fill(null).map((_, r) =>
                            Array(matrixColsB).fill(null).map((_, c) => (
                              <input 
                                key={`B-${r}-${c}`}
                                type="text" 
                                className="matrix-cell-input"
                                placeholder={`B${r+1}${c+1}`}
                                value={matrixB[r][c]}
                                onChange={e => {
                                  const val = e.target.value;
                                  setMatrixB(prev => {
                                    const next = prev.map(row => [...row]);
                                    next[r][c] = val;
                                    return next;
                                  });
                                }}
                                onFocus={() => setFocusedMatrixCell({ matrix: 'B', row: r, col: c })}
                              />
                            ))
                          )}
                        </div>
                      </div>
                    </div>
                  )}

                  {/* VECTOR B */}
                  {(matrixOp === 'solve' || matrixOp === 'cramer' || matrixOp === 'lu_solve' || matrixOp === 'least_squares') && (
                    <div className="matrix-box">
                      <h4>Vector B ({matrixRowsA}×1)</h4>
                      <div className="matrix-bracket-wrapper">
                        <div className="matrix-vector-grid" style={{ gridTemplateRows: `repeat(${matrixRowsA}, 1fr)` }}>
                          {Array(matrixRowsA).fill(null).map((_, r) => (
                            <input 
                              key={`V-${r}`}
                              type="text" 
                              className="matrix-cell-input"
                              placeholder={`B${r+1}`}
                              value={vectorB[r]}
                              onChange={e => {
                                const val = e.target.value;
                                setVectorB(prev => {
                                  const next = [...prev];
                                  next[r] = val;
                                  return next;
                                });
                              }}
                              onFocus={() => setFocusedMatrixCell({ matrix: 'vector', row: r, col: 0 })}
                            />
                          ))}
                        </div>
                      </div>
                    </div>
                  )}
                </div>

                {/* CALCULATE BUTTON */}
                <button 
                  className="solver-submit-btn" 
                  onClick={triggerMatrixOperation}
                  style={{ alignSelf: 'center', width: '220px', marginTop: '10px' }}
                >
                  Calculate Matrix Operation
                </button>

                {matrixResult && (
                  <div className="matrix-results-area">
                    <h4>Result Output</h4>
                    <div className="matrix-result-render">
                      {renderMatrixResult()}
                    </div>
                  </div>
                )}
              </div>
            )}

            {activeTab === 'converter' && (
              /* TAB 5: BASE AND UNIT CONVERTER */
              <div className="converter-container">
                <div className="matrix-workspace-controls" style={{ justifyContent: 'center', gap: '8px' }}>
                  <button 
                    className={`matrix-size-btn ${convertCategory === 'temperature' ? 'active' : ''}`}
                    onClick={() => handleConverterCategoryChange('temperature')}
                  >
                    Temperature
                  </button>
                  <button 
                    className={`matrix-size-btn ${convertCategory === 'pressure' ? 'active' : ''}`}
                    onClick={() => handleConverterCategoryChange('pressure')}
                  >
                    Pressure
                  </button>
                  <button 
                    className={`matrix-size-btn ${convertCategory === 'energy' ? 'active' : ''}`}
                    onClick={() => handleConverterCategoryChange('energy')}
                  >
                    Energy
                  </button>
                  <button 
                    className={`matrix-size-btn ${convertCategory === 'base' ? 'active' : ''}`}
                    onClick={() => handleConverterCategoryChange('base')}
                  >
                    Bases (Hex/Dec/Bin)
                  </button>
                </div>

                <div className="converter-grid">
                  {/* FROM COLUMN */}
                  <div className="converter-panel-half">
                    <div className="converter-field-group">
                      <label>From Scale</label>
                      <select 
                        className="converter-select" 
                        value={convertFrom}
                        onChange={e => setConvertFrom(e.target.value)}
                      >
                        {CONVERTER_UNITS[convertCategory].map(unit => (
                          <option key={unit} value={unit}>{unit}</option>
                        ))}
                      </select>
                    </div>

                    <div className="converter-field-group">
                      <label>Enter Value</label>
                      <input 
                        type="text" 
                        className="converter-input" 
                        placeholder={`Enter ${convertFrom} value`}
                        value={convertValue}
                        onChange={e => setConvertValue(e.target.value)}
                        onFocus={() => setFocusedConverterField(true)}
                        onBlur={() => setFocusedConverterField(false)}
                      />
                    </div>
                  </div>

                  {/* SWAP MIDDLE BUTTON */}
                  <button className="converter-swap-btn" onClick={swapConverterUnits} title="Swap Scales">
                    ⇄
                  </button>

                  {/* TO COLUMN */}
                  <div className="converter-panel-half">
                    <div className="converter-field-group">
                      <label>To Scale</label>
                      <select 
                        className="converter-select" 
                        value={convertTo}
                        onChange={e => setConvertTo(e.target.value)}
                      >
                        {CONVERTER_UNITS[convertCategory].map(unit => (
                          <option key={unit} value={unit}>{unit}</option>
                        ))}
                      </select>
                    </div>

                    <div className="converter-field-group">
                      <label>Result</label>
                      <div className="converter-result-box">
                        {convertLoading ? "Converting..." : (convertResult || "—")}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}

          </div>
        </section>
      </main>
    </div>
  );
}

export default App;