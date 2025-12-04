import { useState, useEffect } from 'react';
import './App.css';

function App() {
  const [primaryDisplay, setPrimaryDisplay] = useState("0");
  const [secondaryDisplay, setSecondaryDisplay] = useState("");
  const [sessionId, setSessionId] = useState("");

  useEffect(() => {
    // Load or generate session ID on mount
    let storedSessionId = localStorage.getItem('calculator_session_id');
    if (!storedSessionId) {
      storedSessionId = crypto.randomUUID();
      localStorage.setItem('calculator_session_id', storedSessionId);
    }
    setSessionId(storedSessionId);
  }, []);

  const handleButtonClick = async (command: string) => {
    try {
      const response = await fetch('http://localhost:8080/api/calculate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Session-ID': sessionId // Send session ID
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
      }
    } catch (error) {
      console.error("Failed to calculate:", error);
      setPrimaryDisplay("Error");
      setSecondaryDisplay("Connection failed");
    }
  };

  const handleKeyPress = (event: KeyboardEvent) => {
    let command = event.key;
    if (command === 'Enter') {
      command = '=';
    } else if (command === 'Backspace') {
      command = '<--';
    } else if (command === 'Escape') {
      command = 'C';
    }

    // Map some keys for better UX
    if (command === '/') command = '/';
    if (command === '*') command = '*';

    // Check if the key corresponds to a button
    if (buttons.flat().includes(command)) {
      handleButtonClick(command);
    }
  };

  useEffect(() => {
    window.addEventListener('keydown', handleKeyPress);
    return () => {
      window.removeEventListener('keydown', handleKeyPress);
    };
  }, [sessionId]); // Re-bind if session ID changes (rare)

  const buttons = [
    ["DEG/RAD", "MC", "MR", "M+", "M-"],
    ["C", "CE", "%", "/", "sqrt"],
    ["7", "8", "9", "*", "1/x"],
    ["4", "5", "6", "-", "x^y"],
    ["1", "2", "3", "+", "log"],
    ["PI", "E", "(", ")", "ln"],
    ["sin", "cos", "tan", "!", "="],
    ["0", ".", "+/-", "<--"]
  ];

  const getButtonClassName = (btn: string) => {
    if (['/', '*', '-', '+', '=', 'x^y', 'sqrt', '1/x', '%'].includes(btn)) {
      return 'operator';
    }
    if (['C', 'CE', '<--', 'DEG/RAD', 'MC', 'MR', 'M+', 'M-'].includes(btn) || !/^[0-9.]$/.test(btn)) {
      return 'special';
    }
    return '';
  };

  const [mode, setMode] = useState("BASIC"); // BASIC, QUADRATIC, CUBIC
  const [solverInputs, setSolverInputs] = useState({ a: "", b: "", c: "", d: "" });

  const handleSolverSubmit = () => {
    let command = "";
    if (mode === "QUADRATIC") {
      command = `SOLVE_QUAD ${solverInputs.a},${solverInputs.b},${solverInputs.c}`;
    } else if (mode === "CUBIC") {
      command = `SOLVE_CUBIC ${solverInputs.a},${solverInputs.b},${solverInputs.c},${solverInputs.d}`;
    }
    handleButtonClick(command);
  };

  return (
    <div className="calculator-container">
      <div className="mode-selector">
        <button onClick={() => setMode("BASIC")} className={mode === "BASIC" ? "active" : ""}>Basic</button>
        <button onClick={() => setMode("QUADRATIC")} className={mode === "QUADRATIC" ? "active" : ""}>Quadratic</button>
        <button onClick={() => setMode("CUBIC")} className={mode === "CUBIC" ? "active" : ""}>Cubic</button>
      </div>

      <div className="calculator">
        <div className="display">
          <div className="secondary-display">{secondaryDisplay}</div>
          <div className="primary-display">{primaryDisplay}</div>
        </div>

        {mode === "BASIC" && (
          <div className="button-grid">
            {buttons.flat().map((btn) => (
              <button
                key={btn}
                onClick={() => handleButtonClick(btn)}
                className={`${getButtonClassName(btn)} ${btn === '=' ? 'equals' : ''} ${btn === '0' ? 'zero' : ''}`}
              >
                {btn}
              </button>
            ))}
          </div>
        )}

        {mode === "QUADRATIC" && (
          <div className="solver-inputs">
            <h3>Quadratic Solver (ax² + bx + c = 0)</h3>
            <input placeholder="a" value={solverInputs.a} onChange={e => setSolverInputs({ ...solverInputs, a: e.target.value })} />
            <input placeholder="b" value={solverInputs.b} onChange={e => setSolverInputs({ ...solverInputs, b: e.target.value })} />
            <input placeholder="c" value={solverInputs.c} onChange={e => setSolverInputs({ ...solverInputs, c: e.target.value })} />
            <button onClick={handleSolverSubmit}>Solve</button>
          </div>
        )}

        {mode === "CUBIC" && (
          <div className="solver-inputs">
            <h3>Cubic Solver (ax³ + bx² + cx + d = 0)</h3>
            <input placeholder="a" value={solverInputs.a} onChange={e => setSolverInputs({ ...solverInputs, a: e.target.value })} />
            <input placeholder="b" value={solverInputs.b} onChange={e => setSolverInputs({ ...solverInputs, b: e.target.value })} />
            <input placeholder="c" value={solverInputs.c} onChange={e => setSolverInputs({ ...solverInputs, c: e.target.value })} />
            <input placeholder="d" value={solverInputs.d} onChange={e => setSolverInputs({ ...solverInputs, d: e.target.value })} />
            <button onClick={handleSolverSubmit}>Solve</button>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;