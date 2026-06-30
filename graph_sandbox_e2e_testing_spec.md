# Interactive Visual Sandbox — End-to-End Testing Specification

A full backend-to-frontend testing plan for the Graph Sandbox (parameter sliders) and Geometric Construction Board features. Covers unit, integration, E2E, performance, and accessibility testing layers, written for an AI dev agent to implement directly as test suites.

---

## 1. Testing Strategy Overview

| Layer | Scope | Tooling Recommendation |
|---|---|---|
| Unit tests | Pure functions: parser, math engine, intersection solvers, measurement formulas | Vitest / Jest |
| Component tests | Individual React components in isolation (sliders, canvas, tool palette) | React Testing Library + Vitest/Jest |
| Integration tests | Multiple components working together (equation input → parser → slider → graph) | React Testing Library |
| API/backend tests | If equation evaluation, CAS, or session persistence is server-side | Supertest / Postman / Jest (backend) |
| End-to-end (E2E) tests | Full user flows in a real browser | Playwright or Cypress |
| Visual regression tests | Pixel-level graph/canvas rendering checks | Playwright screenshot comparison / Percy / Chromatic |
| Performance tests | Render speed, large dataset handling, slider drag responsiveness | Lighthouse, Playwright trace, custom FPS measurement |
| Accessibility tests | Keyboard nav, screen reader, ARIA | axe-core, Playwright + axe |

### Test pyramid target ratio
- 60% unit tests (fast, isolated math/parser logic)
- 25% integration/component tests
- 15% E2E tests (critical user journeys only)

---

## 2. Backend / API Testing (if equation evaluation or persistence is server-side)

### 2.1 Equation evaluation endpoint (if applicable)

| Test ID | Test Case | Expected Result |
|---|---|---|
| BE-EQ-01 | POST valid equation `y = a*sin(b*x)+c` with params `{a:1,b:1,c:0}` | 200 OK, returns array of `(x,y)` sample points |
| BE-EQ-02 | POST equation with division by zero domain (e.g. `y = 1/x` at x=0) | 200 OK, point excluded or marked as undefined/null, no server crash |
| BE-EQ-03 | POST malformed equation syntax (`y = a*sin(*x)`) | 400 Bad Request with descriptive parser error message |
| BE-EQ-04 | POST equation with undeclared/unsupported function (`y = foo(x)`) | 400 Bad Request, error names the unsupported function |
| BE-EQ-05 | POST equation with extremely large domain range (x: -1e10 to 1e10) | Request either rejected with 413/400 or safely clamped — no timeout/crash |
| BE-EQ-06 | POST equation with NaN/Infinity producing inputs | Response sanitizes NaN/Infinity to null, valid JSON returned |
| BE-EQ-07 | Load test: 100 concurrent equation evaluation requests | All complete within SLA (e.g. <500ms p95), no 5xx errors |
| BE-EQ-08 | POST request missing required fields | 400 Bad Request with field-level validation errors |
| BE-EQ-09 | SQL/code injection attempt in equation string (e.g. `require('fs')`) | Rejected safely; confirm no arbitrary code execution (sandboxed `mathjs` eval only) |

### 2.2 Session persistence / save-load endpoint (if applicable)

| Test ID | Test Case | Expected Result |
|---|---|---|
| BE-SESS-01 | Save a sandbox session with valid equations + slider values | 201 Created, returns session ID |
| BE-SESS-02 | Load a session by valid ID | 200 OK, returns exact saved state (equations, params, geometry objects) |
| BE-SESS-03 | Load a session by invalid/non-existent ID | 404 Not Found |
| BE-SESS-04 | Save session exceeding max payload size (e.g. 1000+ geometry objects) | 413 Payload Too Large or graceful pagination, not a silent failure |
| BE-SESS-05 | Concurrent save requests to same session ID | Last-write-wins or conflict response — no data corruption |
| BE-SESS-06 | Unauthorized user attempts to load another user's private session | 403 Forbidden |

### 2.3 Collaborative mode (WebSocket, if applicable)

| Test ID | Test Case | Expected Result |
|---|---|---|
| BE-WS-01 | Two clients connect to same session, client A drags a point | Client B receives updated coordinates within acceptable latency (<200ms) |
| BE-WS-02 | Client disconnects mid-edit | Other clients are not blocked; session state remains consistent |
| BE-WS-03 | Simultaneous edits from two clients to the same object | Conflict resolved deterministically (e.g. CRDT merge or last-write-wins documented behavior) |
| BE-WS-04 | Reconnect after network drop | Client re-syncs to current authoritative state without duplicating objects |

---

## 3. Unit Tests — Math & Parsing Engine

### 3.1 Equation parser (`useEquationParser`)

| Test ID | Test Case | Expected Result |
|---|---|---|
| UT-PARSE-01 | Parse `y = a*sin(b*x) + c` | Returns free variables `['a', 'b', 'c']`, excludes `x`, `y` |
| UT-PARSE-02 | Parse `f(x,y) = x^2 + y^2 - r^2` | Correctly identified as implicit form |
| UT-PARSE-03 | Parse parametric `(x(t), y(t)) = (cos(t), sin(t))` | Correctly identified as parametric, parameter `t` excluded from sliders |
| UT-PARSE-04 | Parse polar `r = a + b*cos(theta)` | Correctly identified as polar, `theta` excluded |
| UT-PARSE-05 | Parse equation with reserved constants (`pi`, `e`) | `pi`/`e` NOT treated as free slider variables |
| UT-PARSE-06 | Parse invalid syntax `y = +*x` | Throws/returns parser error, does not crash |
| UT-PARSE-07 | Parse equation with nested functions `y = sin(cos(a*x))` | Correctly parses nested function calls |
| UT-PARSE-08 | Parse equation with nonstandard whitespace/casing `Y=A*X` | Handles or normalizes gracefully (define expected case-sensitivity behavior) |
| UT-PARSE-09 | Parse piecewise function | Correctly splits into sub-domain expressions |
| UT-PARSE-10 | Re-parse after parameter rename mid-session | Old parameter removed from slider list, new one added without duplicate state |

### 3.2 Graph renderer / sampling (`useGraphRenderer`)

| Test ID | Test Case | Expected Result |
|---|---|---|
| UT-REND-01 | Evaluate `y = x^2` over domain [-10,10] with 500 samples | Returns 500 (x,y) points, values match expected `x^2` within float tolerance |
| UT-REND-02 | Evaluate function with vertical asymptote (`y = 1/x`) near x=0 | Points near asymptote excluded/null, no Infinity values in output array |
| UT-REND-03 | Evaluate function with no real solution at certain x (e.g. `y = sqrt(x)` for x<0) | Those x-values excluded from output, no NaN leaking into render |
| UT-REND-04 | Re-evaluate after parameter change | New points reflect updated parameter value |
| UT-REND-05 | Domain resampling on zoom-in | Sample density increases appropriately for visible range |
| UT-REND-06 | Multiple equations evaluated simultaneously | Each equation's points remain isolated, no parameter cross-contamination unless intentionally shared |

### 3.3 Geometry math (intersection, measurement)

| Test ID | Test Case | Expected Result |
|---|---|---|
| UT-GEO-01 | Line-line intersection, non-parallel lines | Correct (x,y) intersection point within float tolerance |
| UT-GEO-02 | Line-line intersection, parallel lines | Returns "no intersection" (null/empty), not a crash or divide-by-zero |
| UT-GEO-03 | Line-line intersection, identical/overlapping lines | Returns "infinite intersections" status, handled distinctly from "no intersection" |
| UT-GEO-04 | Line-circle intersection, 2 intersection points (secant) | Returns both correct points |
| UT-GEO-05 | Line-circle intersection, tangent (1 point) | Returns single point, correctly flagged as tangent |
| UT-GEO-06 | Line-circle intersection, no intersection | Returns empty/null, no NaN |
| UT-GEO-07 | Circle-circle intersection, 2 points | Returns both correct points via radical line method |
| UT-GEO-08 | Circle-circle intersection, tangent externally/internally | Returns single point, correctly classified |
| UT-GEO-09 | Circle-circle intersection, concentric circles | Returns "no intersection" or "infinite" per defined spec, no crash |
| UT-GEO-10 | Segment intersection clipped outside valid range | Correctly excludes intersection points beyond segment endpoints |
| UT-GEO-11 | Segment length calculation | Matches Euclidean distance formula exactly |
| UT-GEO-12 | Angle between two vectors (0°, 90°, 180° cases) | Matches expected degree/radian values within tolerance |
| UT-GEO-13 | Polygon area via shoelace formula (convex polygon) | Matches known reference area |
| UT-GEO-14 | Polygon area, self-intersecting polygon | Defined behavior documented (either absolute value or explicit warning) |
| UT-GEO-15 | Triangle special points (centroid, circumcenter, incenter, orthocenter) | Match known geometric reference values for test triangles |
| UT-GEO-16 | Dependency graph recompute after dragging a free point | All downstream dependent objects (midpoints, intersections) update correctly |
| UT-GEO-17 | Dependency graph with circular reference attempt | Detected and rejected, no infinite loop |

### 3.4 Slider/parameter logic

| Test ID | Test Case | Expected Result |
|---|---|---|
| UT-SLIDE-01 | Default slider range/step assigned to new parameter | Matches spec default (-10 to 10, step 0.1) |
| UT-SLIDE-02 | Manual override of slider range | New min/max/step correctly applied and persisted |
| UT-SLIDE-03 | Shared parameter name across two equations | Both equations re-render when shared slider changes |
| UT-SLIDE-04 | Animate/play mode increments value over time | Value cycles within min/max bounds, direction reverses or loops per spec |
| UT-SLIDE-05 | Manual text input out-of-range value | Either clamps to range or rejects with validation message (per spec) |

---

## 4. Component / Integration Tests (Frontend)

### 4.1 Equation input → slider generation flow

| Test ID | Test Case | Expected Result |
|---|---|---|
| INT-01 | Type valid equation with 2 parameters into input | Exactly 2 sliders rendered, correctly labeled |
| INT-02 | Type equation with 0 parameters (e.g. `y = x^2`) | No sliders rendered, graph still renders |
| INT-03 | Type invalid equation | Inline error message shown, no crash, previous valid graph (if any) remains visible or clears per spec |
| INT-04 | Edit equation to remove a parameter | Corresponding slider is removed from UI |
| INT-05 | Add second equation with same parameter name as first | Single shared slider controls both equations (per spec) or separate sliders if scoped — verify against intended design |
| INT-06 | Drag slider | Graph line updates within target frame budget (e.g. <16ms per frame for 60fps, or documented threshold) |
| INT-07 | Use animate button on a slider | Value increments visibly, graph re-renders each tick, stop button halts it |
| INT-08 | Switch equation type from explicit to parametric mid-session | UI correctly reconfigures parameter list and render mode |

### 4.2 Geometry board interactions

| Test ID | Test Case | Expected Result |
|---|---|---|
| INT-09 | Select "Point" tool, click canvas | New point created at clicked coordinates, labeled "A" |
| INT-10 | Select "Line" tool, click two existing points | Line segment created connecting them |
| INT-11 | Select "Circle" tool, click center then edge point | Circle created with correct radius |
| INT-12 | Drag a free point | All dependent objects (lines, intersections, measurements) update live |
| INT-13 | Construct two intersecting lines | Intersection point auto-computed and displayed |
| INT-14 | Construct polygon and read live area measurement | Displayed area matches shoelace formula calculation |
| INT-15 | Undo after each construction step | Steps reverse one at a time correctly, redo restores them |
| INT-16 | Snap-to-grid toggle on, place point near grid line | Point snaps exactly to nearest grid intersection |
| INT-17 | Export construction as image | Downloaded file matches visible canvas state |
| INT-18 | Rename a labeled point | New label appears everywhere the point is referenced (measurements, dependent object names) |

### 4.3 Cross-feature integration

| Test ID | Test Case | Expected Result |
|---|---|---|
| INT-19 | Switch between Graph Sandbox tab and Geometry Board tab | State of each tab is preserved independently (no data loss) |
| INT-20 | Pan/zoom on graph, switch tabs, return | Viewport state for graph tab is preserved |
| INT-21 | Save session containing both equations and geometry objects, reload | Both modules restore to exact prior state |

---

## 5. End-to-End (E2E) User Journey Tests

| Test ID | User Journey | Steps | Expected Result |
|---|---|---|---|
| E2E-01 | First-time grapher | Open app → navigate to Graph Sandbox → type `y = a*sin(b*x)+c` → adjust sliders → observe curve change | Curve updates smoothly in real time; no console errors |
| E2E-02 | Multi-curve comparison | Add 3 equations with distinct colors → toggle visibility of each → zoom/pan | All curves render correctly, color-coded, visibility toggles work independently |
| E2E-03 | Geometry construction from scratch | Open Geometry Board → construct a triangle → measure all 3 angles → verify they sum to 180° (within float tolerance) | Angle sum matches geometric expectation |
| E2E-04 | Intersection workflow | Construct two circles that overlap → verify 2 intersection points appear and are correctly labeled | Intersection points rendered and numerically correct |
| E2E-05 | Drag-and-recompute | Construct a midpoint of two points → drag one parent point → verify midpoint updates live | Dependent point recomputes in real time |
| E2E-06 | Undo/redo across session | Perform 10 construction actions → undo 5 → redo 3 → verify state matches expected step | Final state exactly matches the 8th action's expected state |
| E2E-07 | Save and reload | Build a graph + geometry scene → save session → reload page → load session by link | Full scene restored exactly, including slider positions and geometry |
| E2E-08 | Export deliverables | Export graph as PNG, export geometry construction as SVG | Both files download correctly and visually match on-screen state |
| E2E-09 | Mobile/touch interaction | On a touch viewport, drag a slider and drag a geometry point via touch events | Both interactions register correctly with touch, no desktop-only event reliance |
| E2E-10 | Error recovery | Enter a malformed equation, then correct it | App recovers gracefully without requiring a page reload |
| E2E-11 | Collaborative session (if implemented) | Open same session in two browser tabs, drag a point in tab A | Tab B reflects the update within acceptable latency |

---

## 6. Visual Regression Testing

| Test ID | Test Case | Method |
|---|---|---|
| VIS-01 | Baseline graph render for standard equation set | Screenshot diff against approved baseline, fail on >0.1% pixel difference |
| VIS-02 | Slider drag mid-animation frame | Capture frame at fixed parameter value, compare to baseline |
| VIS-03 | Geometry board with full construction (triangle + circle + intersections) | Screenshot diff against baseline |
| VIS-04 | Dark mode / light mode rendering | Separate baselines per theme, verify no contrast/visibility regressions |
| VIS-05 | Responsive breakpoints (mobile, tablet, desktop) | Capture and compare layout at each breakpoint |

---

## 7. Performance Testing

| Test ID | Test Case | Target Metric |
|---|---|---|
| PERF-01 | Slider drag responsiveness | Maintain ≥30fps during continuous drag (measure via Playwright trace or `requestAnimationFrame` timing) |
| PERF-02 | Graph render with 5 simultaneous equations | Full re-render completes within 100ms |
| PERF-03 | Geometry board with 100+ constructed objects | Drag-to-recompute completes within 50ms (dependency graph traversal) |
| PERF-04 | Initial page load (Graph Sandbox) | Time to Interactive (TTI) under 3s on standard test connection (e.g. simulated 4G) |
| PERF-05 | 3D surface plot rendering (if implemented) | Maintains ≥24fps during camera orbit on mid-tier hardware profile |
| PERF-06 | Large session load (1000+ saved geometry objects) | Load completes within 2s, UI remains responsive (no main-thread block >50ms) |
| PERF-07 | Memory leak check | Repeated equation changes (100 iterations) do not cause unbounded memory growth (verify via heap snapshot diff) |

---

## 8. Accessibility (a11y) Testing

| Test ID | Test Case | Expected Result |
|---|---|---|
| A11Y-01 | Keyboard-only navigation through slider controls | All sliders reachable and adjustable via Tab + Arrow keys |
| A11Y-02 | Keyboard-only geometry tool selection and point placement | Tool palette navigable via keyboard; point placement has a keyboard-accessible fallback (not mouse-only) |
| A11Y-03 | Screen reader announces slider value changes | ARIA live region announces updated value on drag |
| A11Y-04 | Screen reader announces geometry measurements | Length/angle/area values exposed via ARIA labels, not just visual text |
| A11Y-05 | Color contrast check on curve colors against background | Meets WCAG AA contrast ratio (4.5:1 minimum) or provides alternate distinguishing pattern |
| A11Y-06 | Sonification mode (if implemented) | Audio pitch correctly maps to y-value as x sweeps across domain |
| A11Y-07 | Automated axe-core scan on Graph Sandbox and Geometry Board pages | Zero critical/serious violations |
| A11Y-08 | Focus indicator visibility | All interactive elements (sliders, tool buttons, canvas points) show a visible focus ring |

---

## 9. Security & Input Sanitization Testing

| Test ID | Test Case | Expected Result |
|---|---|---|
| SEC-01 | Equation input containing script tags or HTML (`<script>alert(1)</script>`) | Rendered as literal text/rejected, no XSS execution |
| SEC-02 | Equation input attempting to access JS globals (`window`, `document`, `eval`) | Sandboxed math evaluator blocks access, returns parser error |
| SEC-03 | Oversized equation string (10,000+ characters) | Rejected with length validation error, no performance degradation |
| SEC-04 | Session ID brute-force/enumeration attempt | Rate-limited or uses non-sequential/UUID session IDs to prevent guessing |
| SEC-05 | Cross-user session access via manipulated session ID in URL | Authorization check blocks access to sessions not owned by/shared with the requesting user |

---

## 10. Test Data Reference Set

Use these standard equations and constructions as fixtures across unit/integration/E2E suites for consistency:

### Equations
```
y = x^2
y = a*sin(b*x) + c           (params: a=1, b=1, c=0)
y = 1/x                       (test asymptote handling)
y = sqrt(x)                   (test domain restriction)
x^2 + y^2 = r^2                (implicit, params: r=5)
(x(t), y(t)) = (cos(t), sin(t))   (parametric, unit circle)
r = a + b*cos(theta)          (polar, params: a=2, b=1)
f(x) = { x^2 if x<0, x if x>=0 }  (piecewise)
```

### Geometry fixtures
```
Triangle: A(0,0), B(4,0), C(0,3)   → expect area=6, right angle at A
Circle 1: center(0,0), r=5
Circle 2: center(6,0), r=5          → expect 2 intersection points
Line 1: through (0,0) and (4,4)
Line 2: through (0,4) and (4,0)     → expect intersection at (2,2)
Parallel lines: y=x and y=x+3       → expect no intersection
```

---

## 11. Test Execution Checklist (for CI/CD pipeline)

- [ ] All unit tests pass (math engine, parser, geometry solvers)
- [ ] All component/integration tests pass
- [ ] E2E suite passes on Chromium, Firefox, WebKit (cross-browser)
- [ ] Visual regression suite shows no unapproved diffs
- [ ] Performance budgets met (see Section 7 targets)
- [ ] Accessibility scan shows zero critical/serious violations
- [ ] Security/sanitization tests pass
- [ ] Backend API tests pass (if applicable) including load test thresholds
- [ ] Manual exploratory testing pass on touch/mobile device
- [ ] No console errors/warnings logged during any E2E run

---

*Testing spec prepared for full backend-to-frontend coverage of the Interactive Visual Sandbox (Graph Sandbox + Geometric Construction Board) feature set.*
