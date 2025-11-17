# Kanban Frontend – React/TypeScript Development Standards

## Technology Stack
- React 19 with function components and hooks everywhere.
- TypeScript 5.3+, Vite 5 build tooling, Tailwind CSS 3.4 for styling, Fetch or Axios for HTTP.
- State managed with React context plus custom hooks; no external state library unless justified.

## Design Reference
- Consult frontend/DESIGN.md before altering visuals, animations, colors, or interaction patterns.
- Reuse the documented motion tokens, CSS variables, and helper utilities (page-transition, modal-overlay, panel-slide, dropdown-panel, etc.).
- Honor prefers-reduced-motion by relying on the shared hooks (useModalAnimation, usePresenceTransition, usePrefersReducedMotion) and providing graceful fallbacks.

## Project Structure
- components: reusable UI split into common, board, card, and other feature folders; colocate tests and stories with implementation files and provide barrel exports for each folder.
- pages: route-level components that orchestrate data fetching and layout composition only.
- hooks, contexts, services, types, utils: each folder houses a single responsibility layer; avoid circular imports.
- public/index assets stay static; configuration files (Vite, Tailwind, TSConfig) remain in the project root.

## Coding Standards
### Components
- Always export named components in PascalCase and keep prop interfaces near the component definition.
- Destructure props in the signature, provide sensible defaults, and avoid spreading arbitrary objects onto DOM nodes.
- Compose small building blocks instead of large monolith components; lift state only when multiple children truly share it.
- Keep conditional UI logic declarative and store derived values in memoized helpers when they are expensive to compute.

### Custom Hooks
- Prefix every hook with use, encapsulate a single concern, and surface a predictable API (state, actions, metadata such as loading/error flags).
- Handle async side effects with abort controllers or guards to avoid state updates on unmounted components.
- Return stable references (useCallback/useMemo) when hooks expose functions that consumers pass down further.

### User Dialogs and Confirmations
- Never use native browser alert() or confirm() functions; they break UI consistency and lack styling control.
- Always use the useDialog hook from @/hooks/useDialog for all user confirmations and alerts.
- Import useDialog and call showConfirm() for yes/no decisions or showAlert() for simple notifications.
- The hook provides Promise-based API with variant options (danger, warning, info, error, success) for semantic meaning.
- Example confirm: `const confirmed = await showConfirm({ title: 'Delete Item', message: 'Are you sure?', variant: 'danger' });`
- Example alert: `await showAlert({ title: 'Error', message: 'Operation failed', variant: 'error' });`
- All dialogs automatically include accessibility features, keyboard support, and consistent animations.

### Service Layer
- Centralize HTTP calls inside services, one file per resource, and keep base URLs plus interceptors in a shared client module.
- Each service method returns typed data (DTOs defined in types/) and throws domain-specific errors that components can handle.
- Apply consistent error mapping, retry rules, and pagination helpers, and ensure all requests route through the same auth/header configuration.

### Authentication & HTTP Client
- **CRITICAL**: Never use native fetch() or custom HTTP clients for API calls; always use the shared axiosInstance from @/utils/axios.
- The axiosInstance is pre-configured with authentication interceptors that automatically inject Authorization Bearer tokens into all requests.
- Before implementing any new service, examine existing service files (cardService.ts, boardService.ts, userService.ts) to follow the established pattern.
- Every API call must follow this pattern: `const response = await axiosInstance.get/post/put/delete<ResponseType>(endpoint, options); return response.data;`
- Missing authentication headers (401 Unauthorized errors) almost always indicate incorrect use of fetch() instead of axiosInstance.
- The axios configuration handles token refresh, CSRF protection, error normalization, and request/response transformation automatically.

### Type Definitions
- Store API contracts, DTOs, and shared interfaces inside types/ and import them everywhere instead of redefining shapes.
- Prefer exact property names that mirror backend responses; use utility types for partials or derived data, and avoid the any type entirely.

### Context & State Management
- Use React context only for state that truly spans distant branches (auth, theme, realtime presence, etc.); everything else should live in hooks or local component state.
- Memoize provider values, split context responsibilities to prevent re-renders, and expose accompanying hooks (useAuth, useTheme) for ergonomics.

### Styling with Tailwind
- Compose Tailwind utility classes and shared component primitives instead of custom CSS whenever possible.
- Define tokens and design decisions in src/index.css or DESIGN.md first, then reference them through CSS variables or Tailwind config extensions.
- Keep responsive breakpoints, dark-mode rules, and motion preferences consistent with the design reference.

### Error Handling
- Normalize API errors in the service layer, surface user-friendly messages, and log detailed context through a single telemetry helper.
- Provide error boundaries for route-level components and render recovery UI where appropriate.

### Testing Standards
- Use React Testing Library for component tests, covering rendering, interaction, accessibility attributes, and critical state transitions.
- Mock network calls with MSW or lightweight fetch mocks, keep snapshots minimal, and focus on behavioral assertions.
- Include tests for hooks with React Testing Library utilities, and ensure form logic, error states, and accessibility features are exercised.

## Best Practices
### Performance
- Memoize expensive calculations, use useMemo/useCallback sparingly but consistently, and split bundles by route or feature to keep the initial payload small.
- Defer non-critical data fetching, virtualize large lists, and prefer streaming or suspense patterns when available.

### Accessibility
- Use semantic HTML elements, ARIA roles/labels where required, logical tab order, and keyboard-accessible controls.
- Manage focus on modal/dialog open and close, trap focus where necessary, and announce dynamic updates to assistive tech when they impact the UI.

### Code Organization
- Keep files focused on a single concern, enforce a predictable folder hierarchy, and use index.ts files for re-exporting modules.
- Maintain clean import ordering (React, third-party, absolute aliases, relative paths) and delete unused utilities promptly.

### Environment Variables
- Prefix every client-side environment variable with VITE_, document them in README, and type them in vite-env declarations.
- Store private values only in .env.local or platform secrets; never commit sensitive strings.

## Code Review Checklist
- Components follow naming, structure, and typing conventions.
- Hooks manage loading/error states and clean up side effects.
- Services centralize network logic and apply consistent error handling.
- **All API calls use axiosInstance, never native fetch() or custom HTTP clients.**
- Accessibility, responsiveness, and performance considerations are addressed.
- Tests cover new behavior, and no stray console logs or unused dependencies remain.

## Development Workflow
- Install dependencies, run the Vite dev server, execute tests, lint, and build through the npm scripts declared in package.json.
- Preview production builds locally before merging and ensure Tailwind/Vite caches are cleared if styles or config files change.

## Git Workflow
- Branch from develop, keep changes scoped, follow conventional commits, and include updated tests plus documentation before opening a pull request.
- Request review, address comments promptly, and squash or rebase only when the team agrees.

## Troubleshooting
- Missing modules typically indicate incorrect alias paths or forgotten installs; verify tsconfig and vite config entries.
- Tailwind classes that do not apply usually mean the file path is absent from the content array; restart the dev server after edits.
- TypeScript complaints often track back to outdated type definitions or mismatched versions; re-run type checking after dependency updates.

## Debug Tips
- Use React DevTools, browser Network panels, and Vite's built-in overlay for runtime issues.
- Enable strict mode and source maps to catch regressions early, and rely on MSW plus Storybook (when available) for isolated debugging of components.
