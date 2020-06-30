package com.jetbrains.edu.go.slow.checker

import com.goide.sdk.GoSdk
import com.jetbrains.edu.go.GoProjectSettings
import com.jetbrains.edu.slow.checker.EduCheckerFixture

/**
 * You need to set GO_SDK environment variable,
 * looks something like `/Users/user/sdk/go1.13.4`
 */
class GoCheckerFixture : EduCheckerFixture<GoProjectSettings>() {
  override val projectSettings: GoProjectSettings get() = GoProjectSettings(GoSdk.fromHomePath(DEFAULT_SDK_LOCATION))

  override fun getSkipTestReason(): String? {
    return if (DEFAULT_SDK_LOCATION == null) {
      "Go SDK location is not found. Use `$GO_SDK` environment variable to provide sdk location"
    } else {
      super.getSkipTestReason()
    }
  }

  companion object {
    private const val GO_SDK = "GO_SDK"
    private val DEFAULT_SDK_LOCATION = System.getenv(GO_SDK)
  }
}
