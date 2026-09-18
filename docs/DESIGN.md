# RentManager — Design System

Source of truth for the UI (Phase 16). Derived from the approved design reference. Any developer or AI implementing screens must use these tokens and patterns instead of inventing values.

## 1. Brand & direction

- Premium, modern, calm. Editorial real-estate feeling built around a deep green.
- Typography: **Inter** (design reference mandate), weights 400/500/600/700. Loaded from Google Fonts with `display=swap` + preconnect; system stack as fallback.
- One accent color, generous whitespace, soft ambient shadows, no harsh borders, no pure black.
- UI language: data screens stay tight and scannable (this is a management app); the public landing is the only place with macro-whitespace and marketing rhythm.

## 2. Color tokens

| Token | Value | Use |
|---|---|---|
| `--color-primary` | `#1F5D50` | Primary actions, sidebar, active nav |
| `--color-primary-hover` | `#174A40` | Hover/active primary |
| `--color-primary-soft` | `#E8F5E9` | Primary-tinted surfaces, selected rows |
| `--color-text` | `#173D2D` | Body and headings |
| `--color-text-muted` | `#5E6C67` | Secondary text, labels, meta |
| `--color-border` | `#E0E6E4` | Hairlines, input borders, dividers |
| `--color-surface` | `#FFFFFF` | Cards, sheets |
| `--color-background` | `#F7F9FA` | App background |
| `--color-danger` / `-soft` | `#C0392B` / `#FDECEA` | Destructive, overdue |
| `--color-warning` / `-soft` | `#8A5910` / `#FEF3C7` | Pending, maintenance |
| `--color-info` / `-soft` | `#1D4ED8` / `#E3EDFF` | Rented, in progress |
| `--color-success` / `-soft` | `#1F7A4D` / `#E6F4EC` | Available, paid, completed |
| `--color-neutral` / `-soft` | `#5B6B66` / `#EEF2F1` | Draft, inactive, cancelled |

Status → variant mapping (used by `<app-status-badge>`):

| Enum value | Variant |
|---|---|
| property `AVAILABLE` | success |
| property `RENTED` | info |
| property `MAINTENANCE` | warning |
| property `INACTIVE` | neutral |
| contract `DRAFT` | neutral |
| contract `ACTIVE` | success |
| contract `EXPIRED` | warning |
| contract `TERMINATED` | danger |
| payment `PENDING` | warning |
| payment `PAID` | success |
| payment `OVERDUE` | danger |
| payment `CANCELLED` | neutral |
| maintenance `OPEN` | warning |
| maintenance `IN_PROGRESS` | info |
| maintenance `COMPLETED` | success |
| maintenance `CANCELLED` | neutral |
| role `ADMIN` | success · `OWNER` | info · `TENANT` | neutral |

## 3. Typography scale

| Token | Size / line-height | Weight | Use |
|---|---|---|---|
| `--text-xs` | 12px / 16px | 500 | Chips, meta, table headers (uppercase, tracking 0.04em) |
| `--text-sm` | 13px / 18px | 400 | Table cells, secondary |
| `--text-base` | 14px / 20px | 400 | Body, inputs, buttons |
| `--text-md` | 16px / 24px | 500 | Card titles, nav |
| `--text-lg` | 18px / 26px | 600 | Section titles |
| `--text-xl` | 22px / 30px | 600 | Page titles |
| `--text-2xl` | 28px / 36px | 700 | KPI numbers |
| `--text-hero` | 40px / 48px | 700 | Landing hero (desktop) |

## 4. Spacing, radius, shadow

- Spacing scale (px): 4, 8, 12, 16, 20, 24, 32, 40, 56, 80. Page padding 24px (16px mobile); card padding 24px; grid gap 20px.
- Radius: `--radius-sm` 8 (inputs, buttons), `--radius-md` 12 (cards), `--radius-lg` 16 (panels, modals), `--radius-xl` 24 (landing cards), `--radius-full` pills/chips.
- Shadows: `--shadow-xs` for inputs/cards, `--shadow-sm` hover elevation, `--shadow-md` modals/dropdowns. Never harsh black shadows.
- Card architecture ("double bezel"): outer shell with hairline border + 6px padding + `--radius-lg`, inner core with `--radius-md` and `--shadow-xs`. Landing cards use `--radius-xl` / inner `--radius-lg`.

## 5. Layout

| Region | Spec |
|---|---|
| Sidebar (desktop ≥1024px) | 264px fixed, `--color-primary` background, white text, active item = `rgba(255,255,255,0.14)` pill + left 3px accent |
| Topbar | 64px, surface + hairline bottom border; search (client-side, 320px), language selector, user menu, logout |
| Content | max-width 1200px, padding 24px, background `--color-background` |
| Mobile (<768px) | Sidebar becomes a drawer (overlay); bottom nav with the 4 main destinations (Inicio, Propiedades, Pagos, Mantenimiento); 16px page padding |
| Landing (public) | full-width sections, hero min-height 560px, section padding 80px desktop / 40px mobile, max-width 1200px inner |

## 6. Components

- **Buttons**: primary (solid `--color-primary`, white text, radius-sm, height 40px, padding 0 20px), secondary (surface + hairline border), ghost (transparent, primary text), danger (solid `--color-danger`). Hover = `--color-primary-hover`; active = `scale(0.98)`; disabled = 50% opacity. Trailing icon sits inside a 28px circular wrapper (button-in-button).
- **Inputs/selects/textarea**: height 44px (input/select), radius-sm, hairline border, 1px focus ring in `--color-primary` + 3px soft glow, label above (`--text-sm`, 500), `.field-error` in `--color-danger` with `--text-xs`, optional leading icon (`<app-icon>` inside the field).
- **Cards**: double bezel as in §4; title `--text-md`, meta `--text-sm` muted.
- **Chips / status badges**: pill, `--text-xs`, soft background + darker text of the same family; optional 6px dot.
- **Tables**: header row `--text-xs` uppercase muted on `--color-background`; rows 52px; hairline dividers; row hover `--color-primary-soft`; numeric columns right-aligned.
- **KPI card**: label `--text-sm` muted, value `--text-2xl`, optional trend/delta line.
- **Modal (confirm dialog)**: overlay `rgba(23,61,45,0.4)` + 4px blur, panel surface `--radius-lg` + `--shadow-md`, max-width 420px, focus trap, Esc closes.
- **Toast**: bottom-right (mobile: top, full width), surface, hairline, left 4px accent by kind, auto-dismiss 4s.
- **Skeleton**: `--color-neutral-soft` block with shimmer sweep, radius-sm; use for tables and cards while loading.
- **Empty state**: centered icon (muted), title `--text-md`, hint `--text-sm`, optional action button.
- **Chart (dashboard)**: pure CSS/SVG bars, primary color, 8px gap, hover tooltip via `title`; 12 months of collected income aggregated client-side from `/api/payments`.

## 7. Motion

| Token | Value |
|---|---|
| `--ease-premium` | `cubic-bezier(0.32, 0.72, 0, 1)` — default for reveals, hover elevation |
| `--ease-standard` | `cubic-bezier(0.2, 0, 0, 1)` — micro state changes |
| Durations | 150ms (micro), 250ms (component), 500ms (page/reveal) |

- Animate only `transform` and `opacity`.
- Route changes: `withViewTransitions()` (native View Transitions API).
- List/card entry: `animate.enter` with a staggered fade-slide (30–40ms per item, cap 300ms).
- Removal: `animate.leave` fade-out (never instant).
- Skeletons shimmer; toasts slide-in from the right; modal scales 0.98→1.
- **All motion must be disabled under `@media (prefers-reduced-motion: reduce)`** (WCAG).

## 8. Accessibility

- Minimum contrast 4.5:1 for text (the palette above meets AA on white and on `--color-primary`). Verified with a WCAG relative-luminance calculation:

| Pair | Ratio |
|---|---|
| text on background / surface | 11.4:1 / 12.1:1 |
| muted text on surface / background | 5.5:1 / 5.2:1 |
| white on primary / primary on white | 7.7:1 |
| chip success / warning / danger / info / neutral | 4.7 / 5.4 / 4.8 / 5.7 / 5.0 |

- Visible focus: 2px `--color-primary` outline, 2px offset, never removed.
- Keyboard: skip-link to `#main-content`, logical tab order, modal focus trap (ConfirmDialog), `inert` on the background while the mobile drawer is open, Esc closes overlays and the drawer.
- Semantic HTML: `header/nav/main/section/table`, one `h1` per page, no skipped heading levels.
- Informative images need `alt`; decorative ones `alt=""`. Property placeholders are decorative.
- Private screens are never indexable (`robots: noindex`); public pages are marked `data: { public: true }` and receive meta description + Open Graph + canonical through `SeoService`.
- Motion is disabled under `prefers-reduced-motion` (including route view transitions).

## 9. Content & i18n rules

- No hardcoded user-facing text: keys only (`properties.title`, `enums.paymentStatus.OVERDUE`, `errors.PROPERTY_NOT_FOUND`).
- Money: locale-formatted number + translated `/mes` suffix (`properties.perMonth`) — there is no currency in the data model.
- Dates: `date` pipe with the active locale. Relative dates ("hace 2 días") are not used.
- Status labels come from `enums.<kind>Status.<CODE>`; never print raw codes.

## 10. Do / don't

- Do keep data screens dense and quiet; reserve expressive layout for the landing.
- Don't add decorative controls that do nothing (notifications bell, fake search) — every visible control must work.
- Don't introduce UI libraries or icon packs: icons are inline SVGs (1.5px stroke) in `<app-icon>`.
- Don't animate layout properties, don't use harsh shadows or gray 1px borders without radius.
