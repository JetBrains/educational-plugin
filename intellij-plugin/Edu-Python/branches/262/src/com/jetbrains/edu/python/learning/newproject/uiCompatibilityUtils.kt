package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.util.UserDataHolder
import com.intellij.python.community.services.systemPython.SystemPython
import com.intellij.python.community.services.systemPython.SystemPythonService
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON_2_VERSION
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON_3_VERSION
import com.jetbrains.edu.python.learning.environment.PyLanguageEnvironment
import com.jetbrains.edu.python.learning.environment.PyLanguageEnvironmentCatalogProvider.Companion.ALL_VERSIONS
import com.jetbrains.python.PathShortener
import com.jetbrains.python.packaging.PyVersionSpecifiers
import com.jetbrains.python.psi.LanguageLevel

suspend fun collectPyEnvironments(course: Course, context: UserDataHolder): Pair<List<PyLanguageEnvironment>, PyLanguageEnvironment?> {
  val systemPythons = SystemPythonService().findSystemPythons()
  val existingEnvironments = systemPythons.map {
    it.toExisting()
  }
    .filter { isSdkApplicable(course, it.systemPython.pythonInfo.languageLevel) }

  if (existingEnvironments.isEmpty()) {
    val installSdk = PyLanguageEnvironment.Install(installVersionSpecifiers(course))
    return Pair(listOf(installSdk), installSdk)
  }

  return Pair(existingEnvironments, existingEnvironments.first())
}

suspend fun SystemPython.toExisting(): PyLanguageEnvironment.Existing {
  val version = pythonInfo.languageLevel.toString()

  return PyLanguageEnvironment.Existing(
    systemPython = this,
    title = "Python $version",
    secondaryText = PathShortener.shorten(pythonBinary),
  )
}

private fun isSdkApplicable(course: Course, sdkLanguageLevel: LanguageLevel): Boolean {
  val courseLanguageVersion = course.languageVersion
  val isPython2Sdk = sdkLanguageLevel.isPython2

  if (isVersionTooNewForCourse(course, sdkLanguageLevel)) {
    return false
  }

  return when (courseLanguageVersion) {
    null, ALL_VERSIONS -> true
    PYTHON_2_VERSION -> isPython2Sdk
    PYTHON_3_VERSION -> !isPython2Sdk
    else -> {
      val courseLanguageLevel = LanguageLevel.fromPythonVersion(courseLanguageVersion)
      when {
        courseLanguageLevel?.isPython2 != isPython2Sdk -> false
        sdkLanguageLevel.isAtLeast(courseLanguageLevel) -> true
        else -> false
      }
    }
  }
}

internal fun installVersionSpecifiers(course: Course): PyVersionSpecifiers {
  val constraints = buildList {
    when (val courseLanguageVersion = course.languageVersion) {
      null, ALL_VERSIONS -> {}
      PYTHON_2_VERSION -> add("<3")
      PYTHON_3_VERSION -> add(">=3")
      else -> add(">=$courseLanguageVersion")
    }

    getFirstUnsupportedPythonVersion(course)?.let {
      add("<${it.toPythonVersion()}")
    }
  }

  return if (constraints.isEmpty()) {
    PyVersionSpecifiers.ANY_SUPPORTED
  }
  else {
    PyVersionSpecifiers(constraints.joinToString(","))
  }
}
