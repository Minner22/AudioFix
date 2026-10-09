---
name: Fluent Desktop Precision
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#3f4850'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#707881'
  outline-variant: '#bfc7d2'
  surface-tint: '#006398'
  primary: '#006194'
  on-primary: '#ffffff'
  primary-container: '#007bb9'
  on-primary-container: '#fdfcff'
  inverse-primary: '#93ccff'
  secondary: '#a73a00'
  on-secondary: '#ffffff'
  secondary-container: '#fd651e'
  on-secondary-container: '#571a00'
  tertiary: '#006860'
  on-tertiary: '#ffffff'
  tertiary-container: '#248279'
  on-tertiary-container: '#f3fffc'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#cce5ff'
  primary-fixed-dim: '#93ccff'
  on-primary-fixed: '#001d31'
  on-primary-fixed-variant: '#004b73'
  secondary-fixed: '#ffdbce'
  secondary-fixed-dim: '#ffb599'
  on-secondary-fixed: '#370e00'
  on-secondary-fixed-variant: '#7f2b00'
  tertiary-fixed: '#9cf2e8'
  tertiary-fixed-dim: '#80d5cb'
  on-tertiary-fixed: '#00201d'
  on-tertiary-fixed-variant: '#00504a'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  headline-lg:
    fontFamily: Segoe UI
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
  headline-md:
    fontFamily: Segoe UI
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  headline-sm:
    fontFamily: Segoe UI
    fontSize: 13px
    fontWeight: '600'
    lineHeight: 18px
  body-lg:
    fontFamily: Segoe UI
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  body-md:
    fontFamily: Segoe UI
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  body-sm:
    fontFamily: Segoe UI
    fontSize: 11px
    fontWeight: '400'
    lineHeight: 14px
  label-lg:
    fontFamily: Segoe UI
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
  label-md:
    fontFamily: Segoe UI
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.2px
  label-sm:
    fontFamily: Segoe UI
    fontSize: 10px
    fontWeight: '600'
    lineHeight: 12px
    letterSpacing: 0.3px
  code-sm:
    fontFamily: Consolas
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

This design system targets utility-first desktop software running within the Windows ecosystem and built with pure JavaFX CSS styling. It replaces outdated skeuomorphic gradients and heavy drop shadows with a crisp, restrained aesthetic aligned with modern desktop productivity standards.

The visual language emphasizes:
- **Clean Flat Structure**: Replaces glossy beveled chrome with 1px hairline borders (`-fx-border-color`), flat background surfaces, and precise grid alignment.
- **Desktop Density**: Tight spatial cadence engineered for mouse-and-keyboard interactions, dense data tables, track inspect lists, and status consoles.
- **Localization-Resilient Ergonomics**: Flexible auto-sizing button containers accommodating multisyllabic Polish terminology (e.g., *Ustawienia ffmpeg*, *Usuń z kolejki*) and concise English labels without clipping or awkward line wrapping.
- **Functional Semantics**: Neutral corporate backdrop punctuated by focused conversion/warning hues (`#ea580c`) highlighting legacy and high-bandwidth codec states (DTS, TrueHD) requiring automated transcode pipelines.

## Colors

The palette establishes an intentional desktop workspace:

- **Primary (`#0284c7`)**: Precision desktop blue used for primary CTA execution (*Start*, *Dodaj pliki...*), active focus rings, and selected table row outlines.
- **Secondary / Warning Accent (`#ea580c`)**: Rich amber-orange dedicated to transcoding notifications, high-bitrate conversion requirements (DTS-HD MA, Dolby TrueHD), and pending stream changes.
- **Tertiary / Success (`#0f766e`)**: Direct stream copy indicator and passthrough codec confirmations (e.g., AAC, AC3 passthrough).
- **Neutral Frame & Surface**:
  - Main Window Background: `#f8fafc` (`-fx-background: #f8fafc;`)
  - Content Containers & Table Canvas: `#ffffff` (`-fx-background-color: #ffffff;`)
  - Surface Muted / Inactive Panels: `#f1f5f9`
  - Subtle Hairline Borders: `#cbd5e1` (`-fx-border-color: #cbd5e1;`)
  - Subtle Row Hover: `#f8fafc`
  - Selected Row: `#e0f2fe`
  - Text Primary: `#0f172a`
  - Text Muted / Empty States: `#64748b`

## Typography

Typography prioritizes standard Windows platform font rendering (`Segoe UI` with fallback to system sans-serif, and `Consolas` for CLI console logs and technical audio stream metadata).

- **Hierarchy Rules**: 
  - Section headers (`Kolejka`, `Ścieżki`) leverage `headline-sm` (13px, weight 600) with muted slate coloring (`#334155`).
  - Table headers and column headers use `label-md` (11px, weight 600) uppercase-adjusted for high-density tabular sorting.
  - Cell data, inputs, and standard buttons operate strictly at `body-md` (12px, regular or medium weight).
  - Terminal logs and stream codec outputs at the bottom of the window use `code-sm` (`Consolas`, 11px) for character alignment across ffmpeg progress reports.

## Layout & Spacing

Desktop utility layouts demand zero wasted space while maintaining clarity between operational zones:

- **Window Layout**: A structured horizontal split plane (`SplitPane` in JavaFX):
  - Left pane: Queue drawer (`Kolejka`), minimum 220px, defaults to 260px.
  - Right pane: Active media stream table (`Ścieżki`) taking the remaining fluid width.
  - Bottom docked strip: Destination path selector, progress indicators, action buttons (`Start`, `Anuluj`), and collapsible log console.
- **Desktop Grid & Docking**:
  - Window edge padding: `0.75rem` (`12px`).
  - Container panel gaps: `0.5rem` (`8px`).
  - Component inner padding: `0.375rem 0.625rem` (`6px 10px`) for standard buttons, ensuring label changes between Polish and English fit without truncation.
- **Window Resizing Behavior**: Table columns resize proportionally with explicit minimum widths for fixed technical columns (`#`, `Typ`, `Kanały`, `Akcja`).

## Elevation & Depth

To guarantee a clean desktop environment without GPU composite overhead:

- **Zero Drop-Shadow Rule**: Standard controls (`.button`, `.text-field`, `.table-view`, `.progress-bar`) do not use drop shadows or Gaussian blurs.
- **Structural Outlines**: Depth is represented exclusively via 1px solid hairline borders:
  - Base border: `#cbd5e1`
  - Active / Focus ring border: `#0284c7`
  - Warning border: `#ea580c`
- **Surface Layering**:
  - Canvas / Window frame: `#f8fafc`
  - Work area containers & Tables: `#ffffff`
  - Control resting state: `#ffffff`
  - Control hover state: `#f1f5f9`
  - Control pressed state: `#e2e8f0`
- **Modals & Overlays**: When a sub-dialog appears (e.g., *Ustawienia ffmpeg*), use a 1px border `#94a3b8` accompanied by a flat subtle overlay `#0f172a1a` without backdrop blur.

## Shapes

The design system employs a disciplined `Soft` (`roundedness: 1`) curvature matching Windows desktop standards:

- **Buttons, Text Inputs, and Select Boxes**: Corner radii are set to `3px` (`-fx-background-radius: 3px; -fx-border-radius: 3px;`).
- **Panels, GroupBoxes, and Table Views**: Radius `4px` or completely squared `0px` when flush against window edges.
- **Badges and Codec Pills**: Compact rectangular tags with `2px` corners rather than rounded pill capsules, preserving a technical tool identity.

## Components

### Buttons (`.button`)
- **Resting**: Background `#ffffff`, border `1px solid #cbd5e1`, text `#0f172a`, radius `3px`, padding `5px 12px`.
- **Hover**: Background `#f8fafc`, border `1px solid #94a3b8`.
- **Pressed**: Background `#f1f5f9`, border `1px solid #64748b`.
- **Primary CTA (`.button-primary`, e.g., Start)**: Background `#0284c7`, border `1px solid #0369a1`, text `#ffffff`, font-weight `600`.
- **Destructive / Danger**: Background `#ffffff`, border `1px solid #fca5a5`, text `#b91c1c`. Hover background `#fef2f2`.
- **Localization Handling**: Buttons declare `-fx-min-width: -Infinity;` with auto-sizing content flow so that Polish verbs (*Usuń z kolejki*, *Ustawienia ffmpeg*) do not truncate.

### Tables (`.table-view`)
- **Headers (`.table-view .column-header`)**: Flat `#f8fafc` background, border bottom `1px solid #cbd5e1`, border right `1px solid #e2e8f0`, text `#475569`, padding `6px 8px`. No gradient headers.
- **Cells (`.table-cell`)**: Height `28px`, text `#0f172a`, border bottom `1px solid #f1f5f9`.
- **Row Hover**: Background `#f8fafc`.
- **Row Selected**: Background `#e0f2fe`, text `#0369a1`.
- **Grid Lines**: Horizontal line `#f1f5f9`, vertical line `#f8fafc`.

### Status Badges & Codec Chips
- **Conversion Warning (`.badge-convert`)**: Used for DTS, TrueHD, DTS-HD MA. Background `#fff7ed`, border `1px solid #fdba74`, text `#ea580c`, font-weight `600`, radius `2px`, padding `1px 6px`.
- **Passthrough / Copy (`.badge-copy`)**: Used for AC3, AAC, untouched tracks. Background `#f0fdf4`, border `1px solid #bbf7d0`, text `#15803d`, font-weight `600`, radius `2px`, padding `1px 6px`.
- **Muted / Subtitle (`.badge-sub`)**: Background `#f8fafc`, border `1px solid #e2e8f0`, text `#64748b`.

### Text Inputs & Path Bars (`.text-field`)
- Background `#ffffff`, border `1px solid #cbd5e1`, radius `3px`, padding `4px 8px`, prompt text `#94a3b8`.
- **Focus**: Border `1px solid #0284c7`, background `#ffffff`.

### Progress Bar (`.progress-bar`)
- Track: Height `6px`, background `#e2e8f0`, radius `2px`.
- Indicator: Flat `#0284c7` (or `#ea580c` during transcode processing), radius `2px`, no indeterminate striped gradient animations.

### Console Output Box (`.text-area.console`)
- Background `#0f172a`, border `1px solid #1e293b`, text `#f8fafc`, font-family `Consolas`, padding `8px`.