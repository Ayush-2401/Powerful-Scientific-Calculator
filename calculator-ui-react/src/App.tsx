import { useState, useEffect } from 'react';
import './App.css';

function App() {
  const [primaryDisplay, setPrimaryDisplay] = useState("0");
  const [secondaryDisplay, setSecondaryDisplay] = useState("");

  const handleButtonClick = async (command: string) => {
    try {
      const response = await fetch('http://localhost:8080/api/calculate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
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
    }
    if (buttons.flat().includes(command)) {
      handleButtonClick(command);
    }
  };

  useEffect(() => {
    window.addEventListener('keydown', handleKeyPress);
    return () => {
      window.removeEventListener('keydown', handleKeyPress);
    };
  }, []);

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

  return (
    <div className="calculator">
      <div className="display">
        <div className="secondary-display">{secondaryDisplay}</div>
        <div className="primary-display">{primaryDisplay}</div>
      </div>
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
    </div>
  );
}

export default App;