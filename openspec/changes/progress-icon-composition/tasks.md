# Tasks

All tasks of groups 1 and 2 land in a single implementation commit (`EDU-9225: ...`): the intermediate states do not
compile. Group 3 lands in a separate implementation commit.

## 1. Extract the progress icon component

- [x] 1.1 Add `learning/actions/ActionProgressIcon.kt` in `intellij-plugin/educational-core` with a lazily created `panel`
  (same `AsyncProcessIcon` + borders + initially invisible as `ActionWithProgressIcon.setUpSpinnerPanel` today) and
  `@RequiresEdt` `processStarted()` / `processFinished()`; verify `get_file_problems` reports no errors for the file
- [x] 1.2 Turn `learning/actions/ActionWithProgressIcon.kt` into an interface with a single
  `val progressIcon: ActionProgressIcon` property; verify `get_file_problems` reports no errors for the file
- [x] 1.3 Make `CheckAction` extend `AnAction` directly and implement `ActionWithProgressIcon` with
  `override val progressIcon = ActionProgressIcon(PROCESS_MESSAGE)`, remove the `setUpSpinnerPanel` call from `init` and
  switch `processStarted()` / `processFinished()` calls to `progressIcon`; verify `get_file_problems` reports no errors
  in `CheckAction.kt`
- [x] 1.4 Make `RetryAction` extend `AnAction` directly and implement `ActionWithProgressIcon` with
  `override val progressIcon = ActionProgressIcon(processMessage)`, remove `setUpSpinnerPanel` from the secondary
  constructor (keep the constructor itself) and switch the `processStarted()` / `processFinished()` calls to
  `progressIcon`; verify `get_file_problems` reports no errors in `RetryAction.kt`

## 2. Update check panel UI

- [x] 2.1 Replace the progress-icon constructor of `CheckPanelButtonComponent` with a companion factory
  `withProgressIcon(action: T, isDefault, isEnabled) where T : AnAction, T : ActionWithProgressIcon` keeping the same
  layout (button west, `action.progressIcon.panel` center); verify `git grep -n "spinnerPanel\|setUpSpinnerPanel"` is
  empty
- [x] 2.2 In `CheckPanel`, create the check and retry buttons with `CheckPanelButtonComponent.withProgressIcon(...)`
  (retry: cast `EduActionUtils.getAction(RetryAction.ACTION_ID)` to `RetryAction`); verify `CheckPanel` does not
  reference `progressIcon`

## 3. Simplify `RetryAction` construction

- [ ] 3.1 Remove the primary constructor parameters of `RetryAction`: use `PROCESS_MESSAGE` directly for `progressIcon`,
  inline `expectedTaskStatus` as `CheckStatus.Failed` and drop the secondary constructor; verify `RetryAction.kt` no
  longer mentions `expectedTaskStatus` or `processMessage`
- [ ] 3.2 Add `action.Educational.Retry.text=Retry` to `EduCoreBundle.properties` instead of passing `actionText`; verify
  `educational-core.xml` declares `messages.EduCoreBundle` as the plugin resource bundle, so the text is resolved by the
  action ID like for `Educational.RefreshTask`
- [ ] 3.3 Verify `./gradlew testClasses` succeeds for the whole project

## 4. Verification

- [x] 4.1 Verify `./gradlew :intellij-plugin:educational-core:testClasses` succeeds
- [x] 4.2 Verify `./gradlew :intellij-plugin:educational-core:test --continue -PexcludeTests=**/slow/**` passes (retry
  failed tests up to 2 times before treating them as a regression)
- [x] 4.3 Verify `./gradlew testClasses` succeeds for the whole project (other modules use `CheckAction` in tests)
- [x] 4.4 Manually check in a sandbox IDE that the spinner appears next to the Check button while checking and next to
  the Retry button while retrying (for a task with `isChangedOnFailed` after a failed check), and disappears afterwards

## Workflow follow-up

- Archive the change with `/openspec-archive-change` after review (no main specs to sync since `skip_specs: true`).
