# Proposal

## Why

`CheckAction` and `RetryAction` get their check panel progress icon by extending the `ActionWithProgressIcon` abstract
class. Because Kotlin has no multiple class inheritance, this ties both actions to one superclass and blocks upcoming
refactorings of the action classes that need them to extend other base classes (EDU-9225). Moving the progress icon into
a separate component removes this constraint.

## What Changes

- Move the spinner panel and its started/finished state from the `ActionWithProgressIcon` abstract class into a
  standalone progress icon component.
- Turn `ActionWithProgressIcon` into an interface that only declares that an action has such a component.
- `CheckAction` and `RetryAction` extend `AnAction` directly, implement the interface and hold an instance of the new
  component instead of inheriting the spinner logic.
- `CheckPanelButtonComponent` takes such an action via a factory method instead of a constructor.
- The spinner panel is created lazily on first use (on EDT) instead of in the action constructor.
- `RetryAction` keeps only the default constructor and takes its text from the message bundle like other registered
  actions.
- No user-visible behavior changes: the check and retry buttons, their layout and the spinner shown while checking or
  retrying stay the same.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is a pure refactoring without spec-level behavior changes, so the change sets `skip_specs: true`.

## Impact

- `intellij-plugin/educational-core`:
  - `learning/actions/ActionWithProgressIcon.kt` (becomes an interface), new `learning/actions/ActionProgressIcon.kt`
  - `learning/actions/CheckAction.kt`, `learning/actions/RetryAction.kt`
  - `resources/messages/EduCoreBundle.properties` (new `action.Educational.Retry.text` key)
  - `learning/taskToolWindow/ui/check/CheckPanelButtonComponent.kt`, `learning/taskToolWindow/ui/check/CheckPanel.kt`
- No changes in other modules, plugin XML registrations, action IDs or tests (no code outside these
  files references `ActionWithProgressIcon`).
