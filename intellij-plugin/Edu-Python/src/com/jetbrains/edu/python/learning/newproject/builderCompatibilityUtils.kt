package com.jetbrains.edu.python.learning.newproject

import com.intellij.python.community.services.systemPython.SystemPythonService
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.python.learning.environment.PyLanguageEnvironment
import java.nio.file.Files
import kotlin.io.path.Path

suspend fun createDefaultSettings(sdkLocation: String): Result<PyLanguageEnvironment, String> {
  val sdkPath = Path(sdkLocation)
  val sdk = SystemPythonService().findSystemPythons(forceRefresh = true).firstOrNull {
    Files.isSameFile(it.pythonBinary, sdkPath)
  }
  return if (sdk == null) Err("No system python found") else Ok(sdk.toExisting())
}
