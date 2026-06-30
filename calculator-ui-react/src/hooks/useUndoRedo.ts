import { useState, useCallback } from 'react';

export function useUndoRedo<T>(initialState: T) {
  const [state, setState] = useState<T>(initialState);
  const [past, setPast] = useState<T[]>([]);
  const [future, setFuture] = useState<T[]>([]);

  const updateState = useCallback((newState: T | ((prev: T) => T)) => {
    setState((prevPresent) => {
      const nextPresent = typeof newState === 'function' ? (newState as Function)(prevPresent) : newState;
      
      // If no change, return previous state
      if (JSON.stringify(prevPresent) === JSON.stringify(nextPresent)) {
        return prevPresent;
      }
      
      setPast((prevPast) => [...prevPast, prevPresent]);
      setFuture([]); // clear redo stack
      return nextPresent;
    });
  }, []);

  const undo = useCallback(() => {
    if (past.length === 0) return;

    setPast((prevPast) => {
      const newPast = [...prevPast];
      const previous = newPast.pop()!;
      setFuture((prevFuture) => [state, ...prevFuture]);
      setState(previous);
      return newPast;
    });
  }, [past, state]);

  const redo = useCallback(() => {
    if (future.length === 0) return;

    setFuture((prevFuture) => {
      const newFuture = [...prevFuture];
      const next = newFuture.shift()!;
      setPast((prevPast) => [...prevPast, state]);
      setState(next);
      return newFuture;
    });
  }, [future, state]);

  const canUndo = past.length > 0;
  const canRedo = future.length > 0;

  const reset = useCallback((newInitialState: T) => {
    setState(newInitialState);
    setPast([]);
    setFuture([]);
  }, []);

  return {
    state,
    set: updateState,
    undo,
    redo,
    canUndo,
    canRedo,
    reset,
  };
}
