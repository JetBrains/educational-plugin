package com.jetbrains.edu.go.slow.checker

import com.goide.sdk.GoSdk
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.go.GoProjectSettings
import com.jetbrains.edu.go.checker.GoEduTaskChecker
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import java.nio.file.Paths

/**
 * You need to set GO_SDK environment variable,
 * looks something like `/Users/user/sdk/go1.13.4`
 */
class GoCheckerFixture : EduCheckerFixture<GoProjectSettings>() {

  private val sdkLocation: String? by lazy {
    val location = System.getenv(GO_SDK) ?: return@lazy null
    Paths.get(location).toRealPath().toString()
  }

  override val projectSettings: GoProjectSettings get() = GoProjectSettings(GoSdk.fromHomePath(sdkLocation))

  override fun getSkipTestReason(): String? {
    return if (sdkLocation == null) {
      "Go SDK location is not found. Use `$GO_SDK` environment variable to provide sdk location"
    } else {
      super.getSkipTestReason()
    }
  }

  override fun setUp() {
    super.setUp()
    // Prevent leaking of javax.swing.Timer from TerminalPanel
    val registryValue = Registry.get(GoEduTaskChecker.GO_RUN_WITH_PTY)
    val oldValue = registryValue.asBoolean()
    registryValue.setValue(false)
    Disposer.register(testRootDisposable) { registryValue.setValue(oldValue) }
  }

  companion object {
    private const val GO_SDK = "GO_SDK"
  }
}
