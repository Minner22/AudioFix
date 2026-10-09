---
name: Flat Precision Dark
colors:
  surface: '#12131a'
  surface-dim: '#12131a'
  surface-bright: '#393841'
  surface-container-lowest: '#0d0e15'
  surface-container-low: '#1b1b23'
  surface-container: '#1f1f27'
  surface-container-high: '#292931'
  surface-container-highest: '#34343c'
  on-surface: '#e4e1ec'
  on-surface-variant: '#bdc8d1'
  inverse-surface: '#e4e1ec'
  inverse-on-surface: '#303038'
  outline: '#87929a'
  outline-variant: '#3e484f'
  surface-tint: '#7bd0ff'
  primary: '#8ed5ff'
  on-primary: '#00354a'
  primary-container: '#38bdf8'
  on-primary-container: '#004965'
  inverse-primary: '#00668a'
  secondary: '#ffb690'
  on-secondary: '#552100'
  secondary-container: '#ec6a06'
  on-secondary-container: '#4a1c00'
  tertiary: '#45e3ce'
  on-tertiary: '#003731'
  tertiary-container: '#07c7b2'
  on-tertiary-container: '#004d44'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#c4e7ff'
  primary-fixed-dim: '#7bd0ff'
  on-primary-fixed: '#001e2c'
  on-primary-fixed-variant: '#004c69'
  secondary-fixed: '#ffdbca'
  secondary-fixed-dim: '#ffb690'
  on-secondary-fixed: '#341100'
  on-secondary-fixed-variant: '#783200'
  tertiary-fixed: '#62fae3'
  tertiary-fixed-dim: '#3cddc7'
  on-tertiary-fixed: '#00201c'
  on-tertiary-fixed-variant: '#005047'
  background: '#12131a'
  on-background: '#e4e1ec'
  surface-variant: '#34343c'
  surface-deep: '#141418'
  surface-base: '#1e1e24'
  surface-raised: '#25252d'
  surface-elevated: '#2a2b36'
  surface-terminal: '#0d0e11'
  border-hairline: '#383a48'
  border-subtle: '#2e303c'
  border-active: '#0284c7'
  text-high-contrast: '#f1f5f9'
  text-muted: '#94a3b8'
  text-disabled: '#6b7280'
  status-convert: '#ea580c'
  status-convert-hover: '#f97316'
  status-passthrough: '#10b981'
  terminal-cyan: '#38bdf8'
  terminal-amber: '#fbbf24'
  terminal-text: '#e2e8f0'
typography:
  headline-lg:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
  headline-md:
    fontFamily: Geist
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  headline-sm:
    fontFamily: Geist
    fontSize: 13px
    fontWeight: '600'
    lineHeight: 18px
  body-lg:
    fontFamily: Geist
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  body-md:
    fontFamily: Geist
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  body-sm:
    fontFamily: Geist
    fontSize: 11px
    fontWeight: '400'
    lineHeight: 14px
  label-lg:
    fontFamily: Geist
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
  label-md:
    fontFamily: Geist
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Geist
    fontSize: 10px
    fontWeight: '600'
    lineHeight: 12px
    letterSpacing: 0.04em
  code-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  code-sm:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '400'
    lineHeight: 15px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  gutter: 0.5rem
  margin: 0.75rem
  space-xs: 0.25rem
  space-sm: 0.375rem
  space-md: 0.5rem
  space-lg: 0.75rem
  space-xl: 1rem
---

## Brand & Style

This design system defines a high-density, low-latency dark desktop interface designed for utility software, media transcoders, and precision audio/video multiplexing tools running on JavaFX. It departs from glossy skeuomorphism, soft blurs, and exaggerated container rounding in favor of crisp structural clarity, flat surface geometry, and strict hairline separation.

The visual direction centers on:
- **Engineered Flat Precision**: 1px sharp borders (`#383a48`), distinct layered dark matte surfaces (`#141418` to `#2a2b36`), and zero drop shadows or faux-3D bevels.
- **High-Density Information Architecture**: Compact padding and fixed vertical cadences tailored for mouse-driven workflows, multi-column stream inspection grids, and split panes.
- **Focused Semantic Accents**: An electric cyan/blue core (`#38bdf8`) driving active execution states, high-contrast amber/orange (`#f97316`) signaling transcodes and non-destructive stream conversions (DTS, TrueHD -> PCM), and muted gunmetal tones (`#6b7280`) suppressing deselected elements.
- **Hardware-Inspired Telemetry**: Integrated terminal logs with glowing phosphor-cyan monospaced telemetry against deep black backdrops for real-time process visibility.

## Colors

The color palette is engineered for prolonged operational focus in low-light environments, relying on strict brightness differentials rather than saturation shifts.

### Architectural Canvas & Surfaces
- **Window Base (`#141418`)**: Deep root canvas applied to main shell backdrops and inactive regions.
- **Panels & Dividers (`#1e1e24`)**: Work area fill for master list drawers, sidebars, and structural split panels.
- **Tables & Inputs (`#25252d`)**: Primary interactive surfaces, including grid viewports, text entry fields, and card containers.
- **Active / Hover Layer (`#2a2b36`)**: Interactive state feedback for hovered rows, unselected tabs, and secondary buttons.
- **Terminal Console (`#0d0e11`)**: High-contrast, near-black surface reserved strictly for output logs and CLI monitors.

### Borders & Dividers
- **Hairline Border (`#383a48`)**: Global structural border token for controls, table cells, and layout partitions.
- **Subtle Divider (`#2e303c`)**: Interior cell separators and horizontal log divisions.

### Functional Accents
- **Primary Electric Cyan (`#38bdf8` / `#0284c7`)**: Directs user intent. Used for start buttons, progress indicators, active radio selections, and active cell borders.
- **Conversion Warning (`#f97316` / `#ea580c`)**: Highlights mandatory stream processing states (e.g., DTS, DTS-HD MA, Dolby TrueHD needing stereo/multichannel PCM conversion).
- **Stream Passthrough (`#10b981`)**: Denotes unaltered stream copy (AAC, AC3 passthrough).
- **Muted Inactive (`#6b7280`)**: Unchecked audio streams, disabled metadata checkboxes, and low-priority tracks.

## Typography

The typography stack uses clean geometric monoline sans-serif for UI elements (`Geist`) and a calibrated technical monospace (`JetBrains Mono`) for logs, CLI telemetry, track indexes, and audio parameter matrices.

- **Primary Headings (`headline-lg`, `headline-md`)**: Employed for top-level utility headers, modal headers, and main window title bars in high-contrast slate white (`#f1f5f9`).
- **Section Headers (`headline-sm`)**: Applied to queue drawers, inspector panels, and container borders.
- **Tabular Data & Cell Inputs (`body-md`)**: Standard grid reading size. Ensures full horizontal scanning efficiency across dense multi-channel audio tracks.
- **Pills, Badges, & Column Headers (`label-md`, `label-sm`)**: Set with slight positive letter spacing and semi-bold weights for rapid legibility against dark slate frames.
- **Terminal & Stream Technical Data (`code-sm`, `code-md`)**: Formatted with fixed-width glyphs ensuring strict tabular alignment for audio bitrates, sample rates (48000 Hz, 96000 Hz), and ffmpeg transcode flags.

## Layout & Spacing

Layouts follow a zero-waste, desktop-first docking model optimized for multi-pane software:

- **Window Canvas & Docking**:
  - Outer application margin is strictly constrained to `0.75rem` (`12px`).
  - Interior gutters between docked functional regions use `0.5rem` (`8px`).
- **Split Pane Layout**:
  - **Left Rail (Job Queue)**: Minimum width 240px, default width 280px. Holds queue order, source file tags, and status dots.
  - **Main Canvas (Stream Inspector)**: Fluid-width viewport hosting the track table (`Ścieżki`), track properties, and codec target selectors.
  - **Bottom Dock (Action & Telemetry)**: Collapsible horizontal region housing destination selectors, progress meters, action triggers (`Start`, `Wstrzymaj`), and the log console.
- **Component Padding Rhythm**:
  - Standard button padding: `0.375rem 0.625rem` (`6px 10px`) accommodating varying string lengths across multilingual interfaces without visual overflow.
  - Compact icon buttons: `0.25rem` (`4px`) bounding box.
  - Table row height: Fixed `26px` to `28px` vertical stride for tabular density.

## Elevation & Depth

This design system uses a strict **zero drop-shadow** policy. In high-density technical software, Gaussian blurs introduce visual fuzziness and unnecessary rendering load. Depth is established purely through surface tonality and hairline borders.

- **Stacking Tiers**:
  - **Base Layer (Level 0)**: `#141418` for window chrome, scroll view trenches, and recessed console output tracks.
  - **Container Layer (Level 1)**: `#1e1e24` for panels, side drawers, and grouping boxes.
  - **Surface Layer (Level 2)**: `#25252d` for interactive list cells, text fields, and unselected buttons.
  - **Elevated Interactive (Level 3)**: `#2a2b36` for hover states, selected rows, and drop-down menu items.
- **Hairline Border Rules**:
  - Every surface transition is delimited by a 1px solid border (`#383a48`).
  - Active focus states drop the neutral border and apply a 1px solid `#38bdf8` outline.
  - Pending transcode elements feature an explicit 1px solid `#ea580c` boundary.
- **Overlay Panels & Modals**:
  - Sub-dialogs (e.g., Codec Configuration, FFmpeg Parameters) sit on `#25252d` framed with a 1px outline in `#475569`, backed by a flat non-blurred scrim (`rgba(10, 10, 14, 0.75)`).

## Shapes

The shape profile is set to **Soft** (`roundedness: 1`), enforcing tight, disciplined contours suitable for technical productivity tools.

- **Standard Controls (Buttons, Inputs, Select Boxes)**: Border radius is locked to `3px` (`0.1875rem`).
- **Surface Panels & Table Containers**: Radius is set to `4px` (`0.25rem`) when free-floating, and completely squared (`0px`) when docked flush to window boundaries.
- **Badges, Chips, and Codec Tags**: Clipped to `2px` corners. Circular pills are prohibited to maintain an uncompromising technical utility look.
- **Scrollbar Thumbs & Progress Trackers**: Square caps with `2px` corner softening.

## Components

### Buttons
- **Primary CTA (`.button-primary`, e.g., Start, Konwertuj)**:
  - Background: `#0284c7`, text: `#ffffff`, border: `1px solid #38bdf8`.
  - Hover: Background `#0369a1`, border: `1px solid #7dd3fc`.
  - Pressed: Background `#075985`.
- **Secondary Action (`.button-secondary`)**:
  - Background: `#25252d`, text: `#f1f5f9`, border: `1px solid #383a48`.
  - Hover: Background `#2a2b36`, border: `1px solid #4b4d61`.
  - Pressed: Background `#1e1e24`.
- **Warning / Transcode Button (`.button-warning`)**:
  - Background: `#ea580c`, text: `#ffffff`, border: `1px solid #f97316`.
  - Hover: Background `#c2410c`.
- **Destructive Button (`.button-danger`)**:
  - Background: `#25252d`, text: `#f87171`, border: `1px solid #7f1d1d`.
  - Hover: Background `#450a0a`, border: `1px solid #ef4444`.

### Tables & Track Viewport (`.table-view`)
- **Header Row**: Background `#1e1e24`, text: `#94a3b8`, border-bottom: `1px solid #383a48`, height: `28px`.
- **Standard Cell**: Background `#25252d`, text: `#f1f5f9`, border-bottom: `1px solid #2e303c`, height: `28px`.
- **Row Hover**: Background `#2a2b36`.
- **Selected Row**: Background `#1e293b`, border-left: `2px solid #38bdf8`, text: `#f1f5f9`.
- **Inactive / Unchecked Row**: Text muted to `#6b7280`, background `#1e1e24`.

### Codec Badges & Status Chips
- **Transcode Required (`.badge-convert`)**:
  - For DTS, DTS-HD MA, Dolby TrueHD needing conversion to PCM.
  - Background: `rgba(234, 88, 12, 0.15)`, text: `#fb923c`, border: `1px solid #ea580c`.
- **Direct Passthrough (`.badge-passthrough`)**:
  - For AC3, E-AC3, AAC copy streams.
  - Background: `rgba(16, 185, 129, 0.15)`, text: `#34d399`, border: `1px solid #059669`.
- **Subtitle / Metadata (`.badge-meta`)**:
  - Background: `#1e1e24`, text: `#94a3b8`, border: `1px solid #383a48`.

### Checkboxes & Selection Controls
- **Box Frame**: `14px x 14px`, background `#1e1e24`, border: `1px solid #383a48`, radius `2px`.
- **Checked (Active Track)**: Background `#0284c7`, border: `1px solid #38bdf8`, glyph `#ffffff`.
- **Checked (Convert Track)**: Background `#ea580c`, border: `1px solid #f97316`, glyph `#ffffff`.
- **Disabled / Muted**: Background `#141418`, border: `1px solid #2e303c`.

### Text Inputs & Path Selectors (`.text-field`)
- Background: `#1e1e24`, text: `#f1f5f9`, prompt text: `#6b7280`, border: `1px solid #383a48`, radius `3px`, height `28px`.
- Focus State: Border `1px solid #38bdf8`, background `#25252d`.

### Progress Bar (`.progress-bar`)
- Track: Height `6px`, background `#1e1e24`, border: `1px solid #383a48`, radius `2px`.
- Progress Fill (Standard): Solid `#38bdf8`, radius `1px`.
- Progress Fill (Transcoding Active): Solid `#f97316`, radius `1px`.

### Terminal Output Console (`.console-view`)
- Background: `#0d0e11`, border: `1px solid #383a48`, radius `3px`, padding `8px`.
- Primary Log Font: `JetBrains Mono` 11px, lineHeight 15px.
- Syntax Colors:
  - Timestamp / Info: `#94a3b8`
  - Stream / Progress Telemetry: `#38bdf8`
  - Warnings / Conversions: `#fbbf24`
  - Errors / Dropped Frames: `#f87171`