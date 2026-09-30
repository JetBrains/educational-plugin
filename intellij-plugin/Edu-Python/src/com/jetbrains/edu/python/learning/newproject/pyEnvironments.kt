package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.python.community.services.systemPython.SystemPython
import com.intellij.python.community.services.systemPython.SystemPythonService
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON_2_VERSION
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON_3_VERSION
import com.jetbrains.edu.learning.newproject.environment.withCaching
import com.jetbrains.edu.python.learning.environment.PyLanguageEnvironment
import com.jetbrains.edu.python.learning.environment.PyLanguageEnvironmentCatalogProvider.Companion.ALL_VERSIONS
import com.jetbrains.python.packaging.PyVersionSpecifiers
import com.jetbrains.python.psi.LanguageLevel
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

private val SYSTEM_PYTHONS: Key<List<SystemPython>> = Key.create("edu.python.system_interpreters")

context(_: UserDataHolder)
suspend fun collectPyEnvironments(course: Course): Pair<List<PyLanguageEnvironment>, PyLanguageEnvironment?> {
  val systemPythons = withCaching(SYSTEM_PYTHONS) {
    SystemPythonService().findSystemPythons(forceRefresh = true)
  }

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

fun SystemPython.toExisting(): PyLanguageEnvironment.Existing {
  val version = pythonInfo.languageLevel.toString()

  return PyLanguageEnvironment.Existing(
    systemPython = this,
    title = "Python $version",
    secondaryText = pythonBinary.absolutePathString(),
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

    course.getMaxSupportedPythonLanguageLevel()?.let {
      // the language level 3.14 should allow versions such as 3.14.15, so we should compare them as "<3.15"
      val majorVersion = it.majorVersion
      val minorVersion = it.minorVersion + 1
      add("<$majorVersion.$minorVersion")
    }
  }

  return if (constraints.isEmpty()) {
    PyVersionSpecifiers.ANY_SUPPORTED
  }
  else {
    PyVersionSpecifiers(constraints.joinToString(","))
  }
}

suspend fun createDefaultSettings(sdkLocation: String): Result<PyLanguageEnvironment, String> {
  val sdkPath = Path(sdkLocation)
  val sdk = SystemPythonService().findSystemPythons(forceRefresh = true).firstOrNull {
    Files.isSameFile(it.pythonBinary, sdkPath)
  }
  return if (sdk == null) Err("No system python found") else Ok(sdk.toExisting())
}
