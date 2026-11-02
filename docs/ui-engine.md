# Native UI runtime

Every `WidgetScreen` owns a `UiScene`. The scene owns invalidation, viewport
constraints and input lifetime; it is not a theme widget. `WWidget` remains the
component API so existing settings and addon controls can participate without
duplicating their business logic.

The native runtime is independent of LiquidBounce's Chromium/Svelte runtime.
It implements the same kinds of layout, stacking and event responsibilities in
Java using Neko's GPU and text backends. No Chromium installation is required.

## Frame lifecycle

1. `UiScene` processes pending layout invalidations.
2. `UiLayout` passes available width down before measuring children, then arranges
   the tree. Settings tables stack their fields when columns cannot fit.
3. `UiInput` picks the frontmost eligible branch using intersected clip rectangles.
   Only that branch gets hover and bubbling pointer events. A handled press captures
   its matching release, including outside the original viewport.
4. `UiCompositor` paints a stable layer snapshot. Stacking contexts flush geometry,
   text and deferred draws before the following layer. HUD elements use the same
   paint-order contract and reverse-order picking.
5. `UiMotion` advances scrolling and expansion with elapsed time. Layout-changing
   animation invalidates the next measure/arrange pass.

The renderer intersects both edges of nested clip regions. Frame opacity is
composed with component opacity instead of being overwritten by hover effects.
Detached dropdown/completion controls are registered through `WWidget.popup()`;
the runtime picks them ahead of normal content. Their owners keep their existing
controls and callback APIs.

## State and interaction

Panel position, expansion, scroll offset and stacking order are stored in the
existing window configuration. Older configurations default missing fields safely.
Dragging displays the positioning grid, and panel release clamps to the viewport.
HUD selection uses the frontmost painted element and brings it forward on drag.
Screen removal cancels capture and clears focus.

## Verification

`:common:uiEngineTest` runs the headless geometry, picking, clipping, capture,
paint-order and time-step regressions. It is attached to `:common:check`.
GPU output and Minecraft interaction still require an in-game smoke test: open
settings, resize, drag panels across one another, drag sliders outside their views,
open dropdowns, scroll expanded groups, and close/reopen the HUD editor.
