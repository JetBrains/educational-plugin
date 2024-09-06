package com.jetbrains.edu.rules

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Allows skipping test execution based on some conditions.
 * Conditions themselves are supposed to be passed via separate annotations
 *
 * Supported annotations: [MinPlatformVersion]
 */
class ConditionalExecutionRule : TestRule {
  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        if (shouldRun(description)) {
          base.evaluate()
        }
        else {
          println("SKIP ${description.displayName}")
        }
      }
    }
  }

  private fun shouldRun(description: Description): Boolean {
    val annotation = description.getAnnotation(MinPlatformVersion::class.java)
      ?: description.testClass.getAnnotation(MinPlatformVersion::class.java)
      ?: return true

    val minSupportedVersion = BuildNumber.fromString(annotation.version) ?: error("Can't create `BuildNumber` from `${annotation.version}` version")
    return ApplicationInfo.getInstance().build >= minSupportedVersion
  }
}