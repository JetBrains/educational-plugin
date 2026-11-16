# Design

## Context

See proposal.md - Why. Current shape of the code:

- `ActionWithProgressIcon` (abstract, extends `AnAction`) holds a nullable `spinnerPanel: JPanel?`, creates it in the
  `@RequiresEdt` `setUpSpinnerPanel(message)` and toggles its visibility in `processStarted()` / `processFinished()`.
- `CheckAction` calls `setUpSpinnerPanel` in `init`. `CheckPanel` creates a new `CheckAction` instance for every check
  panel update.
- `RetryAction` calls `setUpSpinnerPanel` only in its secondary no-arg constructor, which is the one used by the platform
  for the `Educational.Retry` registration. The single registered instance is reused by `CheckPanel` through
  `EduActionUtils.getAction(RetryAction.ACTION_ID) as ActionWithProgressIcon`.
- Both actions toggle the spinner from `Task.Backgroundable` callbacks on EDT.
- `CheckPanelButtonComponent` has a dedicated constructor for `ActionWithProgressIcon` that puts the button to the west
  and the spinner panel to the center. It is the only reader of `spinnerPanel`.

## Goals / Non-Goals

**Goals:**

- `CheckAction` and `RetryAction` no longer depend on a shared superclass for the progress icon.
- The progress icon is a small self-contained component used by the actions and by `CheckPanelButtonComponent`.

**Non-Goals:**

- Introducing a common base class for actions or changing `update` / `actionPerformed` logic of `CheckAction` and
  `RetryAction`. This is the next iteration and is out of scope here.
- Changing the check panel layout or the spinner look.

## Decisions

### 1. A plain class `ActionProgressIcon` owns the spinner

New class in `learning/actions/ActionProgressIcon.kt` (name can be adjusted during review):

- constructor takes the `@NonNls` process message used for `AsyncProcessIcon`;
- `val panel: JPanel` - the same panel that `setUpSpinnerPanel` builds today (`AsyncProcessIcon` to the west, same
  borders, initially invisible);
- `@RequiresEdt fun processStarted()` / `@RequiresEdt fun processFinished()` - toggle `panel.isVisible`.

### 2. `ActionWithProgressIcon` becomes an interface

`ActionWithProgressIcon` is kept as an interface with a single property `val progressIcon: ActionProgressIcon`.
`CheckAction` and `RetryAction` extend `AnAction` directly, implement the interface with
`override val progressIcon = ActionProgressIcon(PROCESS_MESSAGE)` and call `progressIcon.processStarted()` /
`progressIcon.processFinished()` where they call the inherited methods today.

The interface keeps the knowledge "this action has a progress icon" in the action type, so callers like `CheckPanel` do
not access the icon directly, only `CheckPanelButtonComponent` does.

Alternative considered: no interface, actions expose a public `progressIcon` property and `CheckPanel` passes it to
`CheckPanelButtonComponent` as a separate argument. Rejected: every caller has to know about the icon and pass two
related arguments.

### 3. `CheckPanelButtonComponent` takes the action via a generic factory

Kotlin cannot declare a constructor parameter of the intersection type `AnAction & ActionWithProgressIcon`, which the
abstract class used to express. The progress-icon constructor is replaced with a companion factory:

```kotlin
fun <T> withProgressIcon(action: T, isDefault: Boolean = false, isEnabled: Boolean = true): CheckPanelButtonComponent
  where T : AnAction, T : ActionWithProgressIcon
```

It builds the same layout as before (button to the west, `action.progressIcon.panel` to the center). `CheckPanel` calls
`CheckPanelButtonComponent.withProgressIcon(CheckAction(label), ...)` and, for the retry button, casts
`EduActionUtils.getAction(RetryAction.ACTION_ID)` to `RetryAction` (a cast to the interface alone does not give
`AnAction`).

Alternatives considered:

- A top-level function named `CheckPanelButtonComponent` to keep the constructor call syntax. Rejected: it overlaps with
  the existing `(action: AnAction, ...)` constructor, so the chosen overload for `CheckAction` would be non-obvious.
- A single `(action: AnAction, ...)` constructor that checks `action is ActionWithProgressIcon` at runtime. Rejected: no
  compile-time guarantee and two different layouts mixed in one constructor.
- A constructor `(action: ActionWithProgressIcon, ...)` that casts to `AnAction` inside. Rejected: unsafe cast.

### 4. Lazy panel creation

`panel` is created with `by lazy` on first access instead of in the action constructor. All accesses
(`CheckPanelButtonComponent`, `processStarted`, `processFinished`) already happen on EDT.

Rationale: today the `@RequiresEdt` `setUpSpinnerPanel` runs in constructors, and the platform may instantiate a
registered action (`RetryAction`) outside EDT. Lazy creation makes construction thread-agnostic and also avoids creating
Swing components for `CheckAction` instances that never get into the check panel (tests, `CourseValidationHelper`).

Behavior is preserved: if `processStarted` runs before the panel was shown, the panel is created already visible and is
shown as such once added to the check panel, which matches the current behavior of an eagerly created panel.

### 5. `RetryAction` gets only the default constructor

`RetryAction` is created only by the platform from its `Educational.Retry` registration, so its primary constructor
parameters are effectively constants:

- `processMessage` is always `PROCESS_MESSAGE`, which is used directly for `progressIcon`;
- `expectedTaskStatus` is always `CheckStatus.Failed`, which is inlined;
- `actionText` is replaced with the `action.Educational.Retry.text` key in `EduCoreBundle`, the same way registered
  actions like `Educational.RefreshTask` get their text. The generic `retry` key stays since other code uses it.

## Risks / Trade-offs

- [The cast in `CheckPanel` changes from `ActionWithProgressIcon` to `RetryAction`] -> Same module, same registered
  class; a wrong registration would fail the same way as today (`ClassCastException`).
- [No automated tests cover the spinner] -> Verify by compiling the module, running its tests and checking the check and
  retry buttons manually in a sandbox IDE.
