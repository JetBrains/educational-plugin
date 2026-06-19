package com.jetbrains.edu.python.learning.newproject

import com.intellij.python.community.services.systemPython.SystemPythonService
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import kotlin.io.path.Path

suspend fun createDefaultSettings(sdkPath: String): Result<PyProjectSettings, String> {
  val sdk = SystemPythonService().findSystemPythons(forceRefresh = true).firstOrNull {
    it.pythonBinary == Path(sdkPath)
  }
  if (sdk == null) {
    return Err("No system python found")
  }
  return Ok(PyProjectSettings(sdk))
}