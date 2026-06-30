# Interactive Visual Sandbox — Desmos & GeoGebra Inspired Features

A development-ready feature spec for an AI coding agent to implement an interactive graphing and geometry sandbox inside the scientific calculator app (React/`App.tsx`-based).

---

## 1. Dynamic Parameter Sliders

### Concept
Allow users to type equations containing free variables/parameters (e.g. `y = a·sin(b·x) + c`). The UI auto-detects the parameters (`a`, `b`, `c`) and dynamically generates a slider for each one beneath the graph. Dragging a slider re-renders the graph in real time, helping users visualize transformations (amplitude, frequency, vertical shift, etc.).

### Functional requirements

| Requirement | Detail |
|---|---|
| Equation parser | Parse expressions like `y = a*sin(b*x) + c` and detect free variables not equal to `x`/`y`/`t` |
| Auto-slider generation | For each detected parameter, generate a labeled slider with default range (e.g. −10 to 10) and step (0.1) |
| Manual range override | Let user set custom min/max/step per slider (e.g. `a: -5 to 5, step 0.5`) |
| Real-time re-render | Graph updates on every slider drag — debounce/throttle for performance (e.g. requestAnimationFrame) |
| Multi-equation support | Support multiple equations simultaneously, each with its own parameter set; shared parameter names sync across equations |
| Parameter value display | Show current numeric value next to each slider, editable via direct text input |
| Animate button | Optional "play" button per slider to auto-increment the parameter over time (like Desmos) |
| Equation types supported | Explicit `y = f(x)`, implicit `f(x,y) = 0`, parametric `(x(t), y(t))`, polar `r = f(θ)` |
| Color-coded curves | Each equation gets a distinct, user-assignable color |
| Trace/point inspection | Click on the curve to show coordinates at that point |

### Suggested architecture

```
components/
  GraphSandbox/
    GraphCanvas.tsx       // renders the plot (SVG or Canvas, e.g. via D3 or a charting lib)
    EquationInput.tsx     // text input + parser trigger
    ParameterSliders.tsx  // dynamically rendered slider list
    useEquationParser.ts  // hook: parses expression, extracts free variables
    useGraphRenderer.ts   // hook: evaluates f(x) over a domain, returns points
```

### Parsing approach
- Use a math expression parser (e.g. `mathjs`) to compile the expression into an evaluable function.
- Walk the parsed AST/symbol table to extract all free symbols.
- Exclude reserved symbols: `x`, `y`, `t`, `θ`, and known math constants/functions (`pi`, `e`, `sin`, `cos`, etc.).
- Remaining free symbols become slider parameters.

### Rendering approach
- Sample `f(x)` across the visible domain (e.g. 500–1000 points depending on canvas width) on every parameter or viewport change.
- Use `mathjs.compile(expr)` once per equation, then call `.evaluate({x, a, b, c})` per sample point for performance (avoid re-parsing per point).
- Support pan/zoom on the graph canvas; re-sample the domain whenever zoom changes to keep curve smoothness.

### Integration point
Implement as a new component tree mounted from `App.tsx`, e.g. a `<GraphSandbox />` route/tab. Sliders live in `ParameterSliders.tsx` and write to shared React state (`parameterValues: Record<string, number>`) consumed by `useGraphRenderer.ts`.

---

## 2. Dynamic Geometric Construction Board

### Concept
An interactive canvas/workspace mode (GeoGebra-style) where users can construct geometric objects — points, lines, segments, rays, circles, polygons, vectors — and get live measurements (angle, length, area, intersection coordinates) as they drag and edit the construction.

### Functional requirements

| Requirement | Detail |
|---|---|
| Tool palette | Point, Line, Segment, Ray, Circle (center+radius / 3-point), Polygon, Vector, Angle marker, Perpendicular/Parallel line, Midpoint, Tangent line |
| Click-to-construct | Click on canvas to place points; click two points to form a line/segment/vector; click center then edge for a circle |
| Drag to edit | Every constructed point is draggable; dependent objects (lines, intersections) update live |
| Snapping | Snap to grid points, existing points, and intersections for precision |
| Live measurements | Display length of segments, angle between lines/rays (in degrees and radians), area of polygons, circle radius/circumference/area |
| Intersection detection | Auto-compute and label intersection points between any two constructible objects (line-line, line-circle, circle-circle) |
| Coordinate display | Show live (x, y) coordinates of all points, updating on drag |
| Construction history / undo-redo | Step-by-step undo/redo of each construction action |
| Object dependency graph | Maintain a dependency model so derived objects (e.g. midpoint, intersection) auto-update when a parent point moves |
| Labeling | Auto-label points A, B, C…; allow renaming |
| Export | Export construction as image (PNG/SVG) or as a list of coordinates/equations |

### Suggested architecture

```
components/
  GeometryBoard/
    GeometryCanvas.tsx      // main interactive canvas (SVG recommended for hit-testing + accessibility)
    ToolPalette.tsx         // tool selection UI (point/line/circle/polygon/etc.)
    useGeometryEngine.ts    // core state + dependency graph + geometry math
    measurements/
      length.ts
      angle.ts
      area.ts
      intersection.ts       // line-line, line-circle, circle-circle solvers
    GeometryObject.ts        // base types: Point, Line, Segment, Circle, Polygon, Vector
```

### Core data model
- Represent every constructed object as a node in a dependency graph:
  - **Free objects**: points placed directly by the user (have x, y state).
  - **Dependent objects**: computed from free/other dependent objects (e.g. `Midpoint(A, B)`, `Intersection(line1, line2)`).
- On any free object's drag event, recompute the entire dependency graph downstream (topological sort + re-evaluate).
- Store geometry as immutable computed values each render frame; avoid storing redundant coordinates that can drift out of sync.

### Intersection math (must implement)
| Pair | Method |
|---|---|
| Line–Line | Solve linear system from two parametric/general line equations; handle parallel (no solution) case |
| Line–Circle | Substitute line equation into circle equation, solve quadratic; handle 0/1/2 intersection cases |
| Circle–Circle | Use radical line method; handle no-intersection, tangent, and two-intersection cases |
| Segment/Ray bounds | After solving infinite-line intersection, clip the result to the segment/ray's valid parameter range |

### Measurement formulas
| Measurement | Formula |
|---|---|
| Segment length | √((x₂−x₁)² + (y₂−y₁)²) |
| Angle between two lines/vectors | θ = arccos((u·v)/(‖u‖‖v‖)) |
| Polygon area (shoelace formula) | Area = ½|Σ(xᵢyᵢ₊₁ − xᵢ₊₁yᵢ)| |
| Circle circumference / area | 2πr / πr² |
| Triangle properties | Perimeter, area, centroid, circumcenter, incenter, orthocenter |

### Integration point
Implement as a separate workspace tab from `App.tsx`, e.g. `<GeometryBoard />`, mounted alongside the graphing sandbox. Use SVG (not raw Canvas) for the rendering surface to get free hit-testing, accessibility, and easy event binding per shape.

---

## 3. Shared Infrastructure Between Both Modules

| Component | Purpose |
|---|---|
| Math expression engine (`mathjs` or custom) | Shared by sliders (function evaluation) and geometry board (coordinate computation) |
| Coordinate system / viewport manager | Shared pan/zoom logic, screen-to-math coordinate transforms, grid rendering |
| Undo/redo manager | Generic command-pattern history stack usable by both graph and geometry state |
| Theming | Consistent color palette, grid styling, and typography across both workspaces |
| Export manager | Shared logic for exporting canvas content as PNG/SVG |

### Suggested shared hook
```
hooks/
  useViewport.ts        // pan, zoom, screen<->math coordinate conversion (shared)
  useUndoRedo.ts         // generic history stack
  useMathEngine.ts       // wraps mathjs compile/evaluate calls
```

---

## 4. Tech Stack Recommendations

| Concern | Recommendation |
|---|---|
| Math parsing/evaluation | `mathjs` (supports symbolic parsing, complex numbers, matrices — reusable across the whole calculator app) |
| Graph rendering | SVG with D3 for scaling/axes, or Canvas (via `react-konva`) for high point-count performance |
| Geometry rendering | SVG (preferred for hit-testing draggable points/shapes) |
| State management | React state/context for small scope; Zustand/Redux if sandbox state needs to be shared app-wide |
| Drag interactions | `react-draggable` or custom pointer event handlers with SVG coordinate transforms |

---

## 5. Advanced Graphing Features

| Feature | Description |
|---|---|
| 3D surface plotting | Render `z = f(x,y)` as a rotatable 3D surface (e.g. via `three.js` or `plotly.js`); support orbit/zoom/pan camera controls |
| Implicit function plotting | Plot `f(x,y) = 0` curves (e.g. circles, conics) using marching squares algorithm instead of solving for y explicitly |
| Vector field visualization | Plot `(dx/dt, dy/dt)` as arrows across a grid — useful for differential equations |
| Slope field / direction field | For `dy/dx = f(x,y)`, draw short line segments showing local slope at grid points |
| Inequality shading | Plot `y > f(x)` or `f(x,y) < 0` by shading the satisfying region |
| Piecewise function support | Parse and render functions defined differently on sub-domains (e.g. `f(x) = {x² if x<0, x if x≥0}`) |
| Polar & parametric animation | Animate parameter `t` to trace parametric/polar curves over time, showing the tracing point |
| Numerical calculus overlays | Show tangent line at a point (derivative), area-under-curve shading (definite integral), and Riemann sum rectangles |
| Critical point detection | Auto-mark local maxima, minima, inflection points, and roots on the curve |
| Asymptote detection | Auto-detect and draw dashed lines for vertical/horizontal/oblique asymptotes |
| Table of values | Auto-generate a synced (x, f(x)) table next to the graph, scrollable with the viewport |
| Multi-graph comparison overlay | Overlay multiple function families with adjustable opacity |
| Regression/curve fitting | Plot a scatter of points and fit linear/polynomial/exponential/logarithmic regression curves with R² display |
| Statistical distribution plots | Plot PDF/CDF of normal, binomial, Poisson, etc. with adjustable parameters (mean, σ, n, p) |
| Complex function plotting | Domain coloring for complex functions `f(z)` — color = argument, brightness = magnitude |
| Fourier series visualizer | Show partial sums of a Fourier series approximating a function, with term-count slider |
| Sound/audio mode | Convert a function to audible frequency (Web Audio API) — useful for waveform/signal equations |

---

## 6. Advanced Geometry Features

| Feature | Description |
|---|---|
| Transformations toolkit | Reflect, rotate, translate, dilate any constructed object about a point/line/center with a draggable handle |
| Locus construction | Trace the path of a dependent point as a free point moves along a constraint (classic GeoGebra "Locus" tool) |
| Conic section tools | Construct ellipse, parabola, hyperbola via foci/directrix or general conic equation `Ax²+Bxy+Cy²+Dx+Ey+F=0` |
| 3D geometry mode | Extend construction board to 3D: planes, spheres, polyhedra, with orbit camera |
| Compass-and-straightedge mode | Restrict tools to classical construction (no direct coordinate entry) for geometry-proof style exercises |
| Geometric theorem checker | Auto-verify properties post-construction (e.g. "is this triangle equilateral?", "are these lines parallel?") |
| Symbolic coordinate mode | Allow points with symbolic coordinates (e.g. point at `(a, b)`) for proof-style algebraic geometry |
| Animation/path tracing | Animate a free point moving along a defined path, recording trace of all dependent objects |
| Area between curves / shapes | Compute and shade overlapping or bounded regions between multiple constructed shapes |
| Polygon transformations | Auto-compute regular polygon construction (n-gon) from center + radius, inscribed/circumscribed circle |
| Vector operations on canvas | Visual vector addition (parallelogram method), dot/cross product as draggable, see resultant magnitude/direction live |
| Coordinate geometry solver | Given 3 points, auto-derive triangle classification, perpendicular bisectors, circumcircle, etc., as an annotated overlay |
| Tessellation / pattern tools | Repeat a constructed unit shape across the canvas in a grid/tiling pattern |
| Constraint-based construction | Let user fix constraints (e.g. "this segment must always be 5 units," "this angle = 90°") that persist through drags |

---

## 7. Cross-Cutting Advanced Capabilities

| Feature | Description |
|---|---|
| Computer Algebra System (CAS) integration | Symbolic simplify, expand, factor, solve, differentiate, integrate — shown alongside numeric graph |
| Step-by-step solution mode | For solving equations, finding derivatives/integrals, or geometric proofs, show worked steps |
| Voice/natural-language input | Parse spoken or typed natural language ("graph y equals x squared minus 4") into valid expressions |
| Collaborative / multiplayer mode | Real-time shared canvas (WebSocket/CRDT-based) so multiple users can edit the same graph/construction |
| Save/load session state | Persist full sandbox state (equations, sliders, geometry objects) to JSON; shareable via link |
| Embeddable widget mode | Export a constructed graph/geometry scene as an embeddable iframe widget for external sites |
| Accessibility: sonification | Audio cues representing graph shape for visually impaired users (pitch maps to y-value as x sweeps) |
| Performance: WebGL acceleration | For 3D plots and large point counts, offload rendering to WebGL via `three.js`/`regl` instead of SVG/Canvas2D |
| Offline-first / PWA support | Cache the math engine and UI so the sandbox works without network access |
| Plugin/extension architecture | Allow custom tool registration without modifying core engine |

---

## 8. Suggested Build Order (for the AI agent)

1. **Phase 1 — Graph Sandbox core:** Equation input → parser → static plot rendering (no sliders yet)
2. **Phase 2 — Sliders:** Auto-detect parameters → generate sliders → wire to real-time re-render
3. **Phase 3 — Geometry Board core:** Canvas + point/line/circle tools → static construction (no live measurement yet)
4. **Phase 4 — Geometry intelligence:** Dependency graph, drag-to-update, intersection detection, live measurements
5. **Phase 5 — Polish:** Snapping, undo/redo, animate button, export, multi-equation color coding, accessibility (keyboard navigation for sliders and tool selection)
6. **Phase 6 — Advanced graphing:** Implicit plotting, calculus overlays, regression/statistics, 3D surfaces
7. **Phase 7 — Advanced geometry:** Transformations toolkit, locus, conics, constraint-based construction
8. **Phase 8 — Platform features:** CAS integration, collaborative mode, save/load, WebGL acceleration, plugin architecture

---

*Spec prepared for AI agent implementation in the `App.tsx`-based frontend graphing/geometry workspace.*
