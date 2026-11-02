# Tasks

## 1. Remove the module

- [ ] 1.1 Delete the tracked `fleet-plugin/` directory with `git rm -r fleet-plugin` and verify `git ls-files fleet-plugin` is empty
- [ ] 1.2 Delete `.idea/runConfigurations/runFleet.xml` and verify `git ls-files .idea/runConfigurations | grep -i fleet` is empty

## 2. Clean up build configuration

- [ ] 2.1 In `settings.gradle.kts`, remove the `fleetIntegration`-guarded `include("fleet-plugin")` and the `fleetIntegration`-guarded `pluginManagement` repositories (`intellij-dependencies`, `teamcity-rest-client`, `fleet-sdk`); verify `grep -n -i "fleet\|space" settings.gradle.kts` is empty
- [ ] 2.2 In the root `build.gradle.kts`, remove the comment and the `project(":fleet-plugin") { version = ... }` block; verify `grep -n -i fleet build.gradle.kts` is empty and no imports became unused
- [ ] 2.3 In `gradle.properties`, remove the `## Fleet properties` section (`fleetIntegration`, `fleetRuntimeVersion`, `fleetRuntimeMinVersion`, `fleetRuntimeMaxVersion`, `spaceUsername`, `spacePassword`, and the README reference comment); verify `grep -n -i "fleet\|space" gradle.properties` is empty

## 3. Verification

- [ ] 3.1 Verify `git grep -n -I -i fleet -- ':!openspec' ':!intellij-plugin/localization'` finds only false positives (e.g. CSS in `educatorsHelpPage.html`)
- [ ] 3.2 Verify `./gradlew testClasses` succeeds and that Gradle configures the project without the removed properties

## Workflow follow-up

- Archive the change with `/openspec-archive-change` after review (no main specs to sync since `skip_specs: true`).
