import * as math from 'mathjs';

export function useCAS() {
  const simplifyExpr = (expr: string): string => {
    let clean = expr.trim();
    if (clean.includes('=')) {
      clean = clean.split('=')[1].trim();
    }
    try {
      return math.simplify(clean).toString();
    } catch (e: any) {
      return `Error simplifying: ${e.message || e}`;
    }
  };

  const differentiateExpr = (expr: string, variable: string = 'x'): string => {
    let clean = expr.trim();
    if (clean.includes('=')) {
      clean = clean.split('=')[1].trim();
    }
    try {
      return math.derivative(clean, variable).toString();
    } catch (e: any) {
      return `Error differentiating: ${e.message || e}`;
    }
  };

  return {
    simplifyExpr,
    differentiateExpr,
  };
}
