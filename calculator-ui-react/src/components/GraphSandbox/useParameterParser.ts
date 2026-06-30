import * as math from 'mathjs';

const RESERVED_VARIABLES = new Set(['x', 'y', 'z', 't', 'theta', 'θ']);

/**
 * Extracts free parameters from a mathjs expression string.
 * It compiles the AST, traverses SymbolNodes, and filters out:
 * 1. Reserved variables (x, y, t, theta)
 * 2. Known functions in the mathjs namespace (sin, cos, tan, etc.)
 * 3. Standard mathematical constants (pi, e, i)
 */
export function extractParameters(expression: string): string[] {
  if (!expression.trim()) return [];

  let exprToParse = expression.trim();
  if (exprToParse.includes('=')) {
    const parts = exprToParse.split('=');
    exprToParse = parts[1].trim();
  }

  try {
    const parsed = math.parse(exprToParse);
    const symbols = new Set<string>();

    parsed.traverse((node) => {
      if (node.type === 'SymbolNode') {
        const name = (node as any).name;
        
        // Check if it's a reserved coordinate variable
        if (RESERVED_VARIABLES.has(name.toLowerCase())) {
          return;
        }

        // Check if it's a function or constant in mathjs
        if (name in math) {
          const value = (math as any)[name];
          if (typeof value === 'function' || typeof value === 'number' || typeof value === 'object') {
            // Treat it as a built-in constant/function
            return;
          }
        }

        symbols.add(name);
      }
    });

    return Array.from(symbols);
  } catch (err) {
    // If expression doesn't parse yet, return no parameters
    return [];
  }
}
