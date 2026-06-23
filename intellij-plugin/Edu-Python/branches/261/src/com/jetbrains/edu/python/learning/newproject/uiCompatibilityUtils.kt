package com.jetbrains.edu.python.learning.newproject

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.UserDataHolder
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON_2_VERSION
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON_3_VERSION
import com.jetbrains.edu.python.learning.environment.PyLanguageEnvironment
import com.jetbrains.edu.python.learning.environment.PyLanguageEnvironmentCatalogProvider.Companion.ALL_VERSIONS
import com.jetbrains.edu.python.learning.messages.EduPythonBundle
import com.jetbrains.python.PythonRuntimeService
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.*
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import org.jetbrains.annotations.Nls

// Inspired by `com.jetbrains.python.sdk.add.PyAddSdkPanelKt.addBaseInterpretersAsync` implementation
/**
 * @return list of all available python interpreters and a recommended one to select
 */
suspend fun collectPyEnvironments(course: Course, context: UserDataHolder): Pair<List<PyLanguageEnvironment>, PyLanguageEnvironment?> {
  val fakeSdk = createFakeSdk(course, context)?.toLanguageEnvironment()
  val fakeSdks = listOfNotNull(fakeSdk)

  val baseSdks = findBaseSdks(emptyList(), null, context)
    .sortedByDescending { it.languageLevel }
    .map { it.toLanguageEnvironment() }

  val sdks = (fakeSdks + baseSdks)
    // It's important to check validity here, in background thread,
    // because it caches a result of checking if python binary is executable.
    // If the first (uncached) invocation is invoked in EDT, it may throw exception and break UI rendering.
    // See https://youtrack.jetbrains.com/issue/EDU-6371
    .filter { it.sdk.sdkSeemsValid }
    .filter { isSdkApplicable(course, it.sdk.languageLevel) == OK }

  val collectedSdks = sdks.ifEmpty {
    getSdksToInstall().filter { isSdkApplicable(course, it.languageLevel) == OK }.map { it.toLanguageEnvironment() }
  }

  val recommendedSdk = collectedSdks.firstOrNull()

  return collectedSdks to recommendedSdk
}

private fun Sdk.toLanguageEnvironment() = PyLanguageEnvironment(this, languageLevel)

val Sdk.languageLevel: LanguageLevel
  get() {
    return when(this) {
      is PySdkToCreateVirtualEnv -> {
        val pythonVersion = versionString
        if (pythonVersion == null) {
          LanguageLevel.getDefault()
        }
        else {
          LanguageLevel.fromPythonVersion(pythonVersion) ?: LanguageLevel.getDefault()
        }
      }

      is PyDetectedSdk -> {
        // PyDetectedSdk has empty `sdk.versionString`, so we should manually get language level from homePath if it exists
        homePath?.let {
          sdkFlavor.getLanguageLevel(it)
        } ?: LanguageLevel.getDefault()
      }

      is PySdkToInstall -> {
        LanguageLevel.fromPythonVersion(installation.release.version) ?: LanguageLevel.getDefault()
      }

      else -> {
        PythonRuntimeService.getInstance().getLanguageLevelForSdk(this)
      }
    }
  }


private fun isSdkApplicable(course: Course, sdkLanguageLevel: LanguageLevel): Result<Unit, String> {
  val courseLanguageVersion = course.languageVersion
  val isPython2Sdk = sdkLanguageLevel.isPython2

  return when (courseLanguageVersion) {
    null, ALL_VERSIONS -> OK
    PYTHON_2_VERSION -> if (isPython2Sdk) OK else NoApplicablePythonError(2)
    PYTHON_3_VERSION -> if (!isPython2Sdk) OK else NoApplicablePythonError(3)
    else -> {
      val courseLanguageLevel = LanguageLevel.fromPythonVersion(courseLanguageVersion)
      when {
        courseLanguageLevel?.isPython2 != isPython2Sdk -> SpecificPythonRequiredError(courseLanguageVersion)
        sdkLanguageLevel.isAtLeast(courseLanguageLevel) -> OK
        else -> SpecificPythonRequiredError(courseLanguageVersion)
      }
    }
  }
}

@RequiresBackgroundThread
private fun getBaseSdk(course: Course, context: UserDataHolder? = null): PyBaseSdkDescriptor? {
  val baseSdks = PyBaseSdksProvider.getBaseSdks(context)
  if (baseSdks.isEmpty()) {
    return null
  }
  return baseSdks.filter { isSdkApplicable(course, it.languageLevel) == OK }.maxByOrNull { it.languageLevel }
}

private class NoApplicablePythonError(
  requiredVersion: Int,
  errorMessage: @Nls String = EduPythonBundle.message(
    "error.incorrect.python",
    requiredVersion
  )
) : Err<String>(errorMessage)

private class SpecificPythonRequiredError(
  requiredVersion: String,
  errorMessage: @Nls String = EduPythonBundle.message(
    "error.old.python",
    requiredVersion
  )
) : Err<String>(
  errorMessage
)

@RequiresBackgroundThread
private fun createFakeSdk(course: Course, context: UserDataHolder): Sdk? {
  val baseSdk = getBaseSdk(course, context) ?: return null
  val flavor = PythonSdkFlavor.getApplicableFlavors(false)[0]
  val prefix = flavor.name + " "
  val version = baseSdk.version
  if (prefix !in version) {
    return null
  }
  val pythonVersion = version.substring(prefix.length)
  val name = "new virtual env $pythonVersion"

  return PySdkToCreateVirtualEnv.create(name, baseSdk.path, pythonVersion)
}

private val OK = Ok(Unit)
