package com.jetbrains.edu.cpp

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.jetbrains.cidr.cpp.cmake.projectWizard.CLionProjectWizardUtils
import com.jetbrains.cidr.cpp.toolchains.CMake
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains

interface TemplateConstant {
  val key: String
  val value: String

  fun toPair(): Pair<String, String> = key to value

  companion object {
    operator fun invoke(key: String, value: String): TemplateConstant =
      object : TemplateConstant {
        override val key: String = key
        override val value: String = value
      }
  }
}

object TestFrameworks {
  object GTest {
    val version = TemplateConstant(
      key = "GTEST_VERSION",
      value = "release-1.8.1"
    )

    val sourceDir = TemplateConstant(
      key = "GTEST_SOURCE_DIR",
      value = "googletest-src"
    )

    val buildDir = TemplateConstant(
      key = "GTEST_BUILD_DIR",
      value = "googletest-build"
    )
  }

  object Catch {
    val headerUrl = TemplateConstant(
      key = "CATCH_URL",
      value = "https://raw.githubusercontent.com/catchorg/Catch2/master/single_include/catch2/catch.hpp"
    )
  }

  val baseDir = TemplateConstant(
    key = "TEST_FRAMEWORK_DIR",
    value = "test-framework"
  )
}

object CMakeConstants {
  val minimumRequiredLine: TemplateConstant = object : TemplateConstant {
    override val key: String = "CMAKE_MINIMUM_REQUIRED_LINE"

    override val value: String by lazy {
      val cMakeVersionExtractor = {
        CLionProjectWizardUtils.getCMakeMinimumRequiredLine(CMake.readCMakeVersion(CPPToolchains.getInstance().defaultToolchain))
      }

      val progressManager = ProgressManager.getInstance()

      if (progressManager.hasProgressIndicator()) {
        progressManager.runProcess(cMakeVersionExtractor, null)
      }
      else {
        progressManager.run(object : Task.WithResult<String, Nothing>(null, "Getting CMake Minimum Required Version", false) {
          override fun compute(indicator: ProgressIndicator) = cMakeVersionExtractor()
        })
      }
    }
  }

  fun makeProjectNameConstant(projectName: String): TemplateConstant = TemplateConstant(
    key = "PROJECT_NAME",
    value = projectName
  )

  fun makeCppStandardLineConstant(cppStandardLine: String): TemplateConstant = TemplateConstant(
    key = "CPP_STANDARD",
    value = cppStandardLine
  )
}