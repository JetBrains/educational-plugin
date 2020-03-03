package com.jetbrains.edu.cpp.constants

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
}

object TestFrameworks {
  object GTest {
    val version: TemplateConstant = object : TemplateConstant {
      override val key: String = "GTEST_VERSION"

      override val value: String
        get() = CppConstantsBundle.getConstant("cpp.gtest.version")
    }

    val sourceDir: TemplateConstant = object : TemplateConstant {
      override val key: String = "GTEST_SOURCE_DIR"

      override val value: String
        get() = CppConstantsBundle.getConstant("cpp.gtest.source.dir")
    }

    val buildDir: TemplateConstant = object : TemplateConstant {
      override val key: String = "GTEST_BUILD_DIR"

      override val value: String
        get() = CppConstantsBundle.getConstant("cpp.gtest.build.dir")
    }
  }

  object Catch {
    val headerUrl: TemplateConstant = object : TemplateConstant {
      override val key: String = "CATCH_URL"

      override val value: String
        get() = CppConstantsBundle.getConstant("cpp.catch.header.link")
    }
  }

  val baseDir: TemplateConstant = object : TemplateConstant {
    override val key: String = "TEST_FRAMEWORK_DIR"

    override val value: String
      get() = CppConstantsBundle.getConstant("cpp.test.framework.dir")
  }
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

  fun makeProjectNameConstant(projectName: String): TemplateConstant = object : TemplateConstant {
    override val key: String = "PROJECT_NAME"
    override val value: String = projectName
  }

  fun makeCppStandardLineConstant(cppStandardLine: String): TemplateConstant = object : TemplateConstant {
    override val key: String = "CPP_STANDARD"
    override val value: String = cppStandardLine
  }
}