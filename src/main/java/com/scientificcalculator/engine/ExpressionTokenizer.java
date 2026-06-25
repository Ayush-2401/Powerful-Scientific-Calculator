package com.scientificcalculator.engine;

import java.util.ArrayList;
import java.util.List;

public class ExpressionTokenizer {

    public static List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        if (expression == null) {
            return tokens;
        }

        int i = 0;
        int len = expression.length();
        while (i < len) {
            char ch = expression.charAt(i);
            if (Character.isWhitespace(ch)) {
                i++;
                continue;
            }

            // Handle numbers (integers and decimals)
            if (Character.isDigit(ch) || ch == '.') {
                StringBuilder sb = new StringBuilder();
                boolean hasDecimal = (ch == '.');
                sb.append(ch);
                i++;
                while (i < len) {
                    char next = expression.charAt(i);
                    if (Character.isDigit(next)) {
                        sb.append(next);
                        i++;
                    } else if (next == '.' && !hasDecimal) {
                        sb.append(next);
                        hasDecimal = true;
                        i++;
                    } else {
                        break;
                    }
                }
                tokens.add(sb.toString());
                continue;
            }

            // Handle alphabetic identifiers (functions, constants, variables)
            if (Character.isLetter(ch)) {
                StringBuilder sb = new StringBuilder();
                sb.append(ch);
                i++;
                while (i < len && (Character.isLetterOrDigit(expression.charAt(i)) || expression.charAt(i) == '_')) {
                    sb.append(expression.charAt(i));
                    i++;
                }
                tokens.add(sb.toString());
                continue;
            }

            // Handle operators and parentheses
            if (ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '^' || ch == '(' || ch == ')' || ch == '!' || ch == '%') {
                tokens.add(String.valueOf(ch));
                i++;
                continue;
            }

            // Unknown character
            throw new IllegalArgumentException("Unknown character in expression: " + ch);
        }
        return tokens;
    }
}
