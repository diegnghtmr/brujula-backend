# ADR-004: React 19, TypeScript Strict, Vite 8, Tailwind v4, shadcn/ui

**Status**: Accepted
**Date**: 2025-11-01
**Deciders**: Brújula architecture team

---

## Context

The frontend must be a modern SPA with strong type safety, a consistent design system, good developer experience, and a sustainable component library. We evaluated:
- **Framework**: React (meta-framework ecosystem) vs Vue 3 vs Svelte
- **TypeScript**: strict mode vs permissive
- **Build tool**: Vite vs webpack vs Turbopack
- **CSS**: Tailwind v4 (CSS-first) vs styled-components vs CSS modules
- **Component library**: shadcn/ui vs Radix-only vs Material UI vs Ant Design

The decision must align with the Brújula design system (DESIGN.md §7) which defines canonical tokens, specific border-radius overrides, and academic state badge variants.

---

## Decision

We adopt **React 19** with **TypeScript strict mode**, **Vite 8**, **Tailwind CSS v4**, and **shadcn/ui** (new-york style, CSS variables enabled) as the frontend stack.

Key rationale:
- React 19's compiler eliminates manual memoization (`useMemo`/`useCallback`) boilerplate
- TypeScript strict mode catches null/undefined errors at compile time
- Vite 8 provides sub-second HMR and native ES module builds
- Tailwind v4 is CSS-first (no `tailwind.config.js`): design tokens are pure CSS custom properties in `src/styles/globals.css`, exposed to utility classes via `@theme inline`
- shadcn/ui is a component source distribution (not a package), allowing mandatory overrides: `Card → rounded-none`, `Button/Badge → rounded-full`, academic state variants

Node 24 is the minimum runtime (matches the backend's modern-tooling philosophy).

---

## Consequences

**Positive**:
- Tailwind v4 CSS-first approach aligns perfectly with the design system token strategy
- shadcn/ui components are fully owned (not a black-box package) — overrides are first-class
- React Compiler eliminates the largest source of React performance bugs
- Vite proxy eliminates CORS complexity in local development

**Negative**:
- Tailwind v4 docs and community resources are less mature than v3
- shadcn/ui overrides must be maintained manually when shadcn releases upstream updates
- React 19 is relatively new; some ecosystem libraries may lag in React 19 compatibility

**Neutral**:
- `pnpm` is the package manager (deterministic installs, workspace support for future monorepo)
- ESLint enforces no direct `axios` imports outside `src/api/client.ts` (FCC-001)
