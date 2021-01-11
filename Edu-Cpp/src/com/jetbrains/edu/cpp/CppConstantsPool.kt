package com.jetbrains.edu.cpp

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.jetbrains.cidr.cpp.cmake.projectWizard.CLionProjectWizardUtils
import com.jetbrains.cidr.cpp.toolchains.CMake
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains

const val GTEST_VERSION_KEY: String = "GTEST_VERSION"
const val GTEST_VERSION_VALUE: String = "release-1.8.1"

const val GTEST_SOURCE_DIR_KEY: String = "GTEST_SOURCE_DIR"
const val GTEST_SOURCE_DIR_VALUE: String = "googletest-src"

const val GTEST_BUILD_DIR_KEY: String = "GTEST_BUILD_DIR"
const val GTEST_BUILD_DIR_VALUE: String = "googletest-build"

const val CATCH_HEADER_URL_KEY: String = "CATCH_URL"
const val CATCH_HEADER_URL_VALUE: String =
  "https://raw.githubusercontent.com/catchorg/Catch2/de6fe184a9ac1a06895cdd1c9b437f0a0bdf14ad/single_include/catch2/catch.hpp"

const val TEST_FRAMEWORKS_BASE_DIR_KEY: String = "TEST_FRAMEWORK_DIR"
const val TEST_FRAMEWORKS_BASE_DIR_VALUE: String = "test-framework"

const val CMAKE_MINIMUM_REQUIRED_LINE_KEY: String = "CMAKE_MINIMUM_REQUIRED_LINE"
val CMAKE_MINIMUM_REQUIRED_LINE_VALUE: String by lazy {
  val cMakeVersionExtractor = {
    CLionProjectWizardUtils.getCMakeMinimumRequiredLine(CMake.readCMakeVersion(CPPToolchains.getInstance().defaultToolchain))
  }

  val progressManager = ProgressManager.getInstance()
  if (progressManager.isUnderProgress) {
    progressManager.runProcess(cMakeVersionExtractor, null)
  }
  else {
    progressManager.run(object : Task.WithResult<String, Nothing>(null, "Getting CMake Minimum Required Version", false) {
      override fun compute(indicator: ProgressIndicator) = cMakeVersionExtractor()
    })
  }
}

const val CMAKE_PROJECT_NAME_KEY: String = "PROJECT_NAME"

const val CMAKE_CPP_STANDARD_KEY: String = "CPP_STANDARD"

private val ProgressManager.isUnderProgress: Boolean
  get() = hasProgressIndicator() || hasModalProgressIndicator() || hasUnsafeProgressIndicator()
