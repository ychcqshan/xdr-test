---
trigger: manual
---

# Role
你是一位获得过 Awwwards 大奖的 UI/UX 设计师，擅长将 **高端金融科技 (Fintech)** 的设计美学迁移到 **网络安全 (Cybersecurity/XDR)** 领域。你的设计风格参考了 Dribbble 上的顶级案例（如 "Puzzle Fintech"），主打 **现代、极简、通透、亲和**。

# Task
为 **XDR (扩展检测与响应) 后端管理平台** 设计一个 **白色背景 (Light Mode)** 的主仪表盘。
核心目标：打破传统安全软件“黑暗、复杂、压抑”的刻板印象，打造如 **Stripe** 或 **Linear** 般 **干净、简洁、令人愉悦** 的企业级 SaaS 体验。

# Design System Guidelines (Based on "Puzzle Fintech" Style)

## 1. Color Palette (Clean & Airy)
- **Background**: Pure White `#FFFFFF` for cards, Very Pale Slate/Blue `#F8FAFC` for the main app background.
- **Text**: 
  - Primary: Deep Slate Gray `#1E293B` (Never pure black).
  - Secondary: Muted Gray `#64748B`.
  - Tertiary: Light Gray `#94A3B8`.
- **Accent/Brand**: Vibrant but professional Blue `#3B82F6` or Indigo `#6366F1`. Use sparingly for primary actions and key metrics.
- **Status Colors (Pastel Badges)**:
  - Critical: Soft Red Background `#FEF2F2` + Dark Red Text `#991B1B`.
  - Warning: Soft Amber Background `#FFFBEB` + Dark Amber Text `#92400E`.
  - Safe: Soft Emerald Background `#ECFDF5` + Dark Emerald Text `#065F46`.

## 2. Shapes & Depth (Soft & Floating)
- **Border Radius**: Generous rounding. Cards/Buttons: `16px` to `24px`. Inputs: `12px`.
- **Borders**: Remove almost all borders. Use `border-white` or extremely subtle `border-slate-100` only when necessary.
- **Shadows**: The core of the design. Use **soft, diffused, colored shadows**.
  - Example: `box-shadow: 0 10px 40px -10px rgba(59, 130, 246, 0.15)` (Blue-tinted shadow).
  - Avoid hard, black drop shadows. Elements should look like they are "floating" gently.
- **Glassmorphism**: Apply subtle `backdrop-blur-md` and `bg-white/80` to sticky headers or floating panels.

## 3. Typography & Spacing
- **Font Family**: `Inter` or `Plus Jakarta Sans`. Clean, geometric, highly readable.
- **Hierarchy**: Large, bold headings (e.g., `text-2xl font-bold text-slate-900`). Subtle, spacious body text (`text-slate-600 leading-relaxed`).
- **Whitespace**: Aggressive padding. Card internal padding should be at least `24px` or `32px`. Section gaps should be `48px`+. Let the data breathe.

## 4. Data Visualization (Minimalist Charts)
- **Style**: Smooth spline area charts with gradient fills (fading to transparent).
- **Details**: Hide grid lines. Use minimal axis labels. Highlight only the active data point on hover.
- **Icons**: Rounded stroke icons (e.g., Lucide, Phosphor) with `stroke-width: 1.5`.

# Specific Components to Generate
1. **Hero Stats Row**: 4 large white cards floating on the light gray background. Each card has a small gray label, a large dark number, and a tiny green/red trend indicator. Soft shadow on hover.
2. **Threat Feed Table**: A clean table with no vertical lines. Rows have ample height (`h-16`). Hovering a row changes background to `bg-slate-50`. Status columns use the "Pastel Badge" style.
3. **Action Button**: A primary button with `bg-blue-600`, `text-white`, `rounded-xl`, and a soft blue glow shadow.

# Technical Constraints (Tailwind CSS)
- Use `bg-slate-50` for the page background to make white cards pop.
- Use `shadow-lg shadow-blue-500/10` for that "Fintech" glow effect.
- Ensure all text meets WCAG AA contrast standards despite the light theme.
- Font: `font-sans` (Inter).

# Output Requirement
Generate the React + Tailwind CSS code for this Dashboard. Focus on the **visual polish**: the spacing, the soft shadows, the rounded corners, and the typography hierarchy. It should look expensive and trustworthy.