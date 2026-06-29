@file:JvmName("PyEduUtils")

package com.jetbrains.edu.python.learning

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.configuration.attributesEvaluator.AttributesEvaluator
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON_2_VERSION
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON_3_VERSION
import com.jetbrains.edu.learning.courseFormat.ext.findTaskFileInDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isTestsFile
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.python.learning.environment.PyLanguageEnvironmentCatalogProvider
import com.jetbrains.edu.python.learning.messages.EduPythonBundle
import com.jetbrains.python.Result
import com.jetbrains.python.errorProcessing.PyResult
import com.jetbrains.python.packaging.common.PythonPackage
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.isReadOnly
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val LOG = fileLogger()

fun Task.getCurrentTaskVirtualFile(project: Project): VirtualFile? {
  val taskDir = getDir(project.courseDir) ?: error("Failed to get task dir for `${name}` task")
  var resultFile: VirtualFile? = null
  for ((_, taskFile) in taskFiles) {
    val file = taskFile.findTaskFileInDir(taskDir) ?: continue
    if (file.isTestsFile(project) || !TextEditorProvider.isTextFile(file)) continue
    if (resultFile == null) {
      resultFile = file
    }

    val hasNewPlaceholder = taskFile.answerPlaceholders.any { p -> p.placeholderDependency == null }
    if (hasNewPlaceholder) return file
  }
  return resultFile
}

fun Task.getCurrentTaskFilePath(project: Project): String? {
  return getCurrentTaskVirtualFile(project)?.systemDependentPath
}

internal fun pythonAttributesEvaluator(baseEvaluator: AttributesEvaluator): AttributesEvaluator = AttributesEvaluator(baseEvaluator) {
  dirAndChildren(*FOLDERS_TO_EXCLUDE, direct = true) {
    @Suppress("DEPRECATION")
    legacyExcludeFromArchive()
    archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
  }

  extension("pyc") {
    @Suppress("DEPRECATION")
    legacyExcludeFromArchive()
    archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
  }
}

fun installRequiredPackages(project: Project, sdk: Sdk) {
  InstallPackageCoroutineScope.getCoroutineScope(project).launch {
    val result = executePackageInstallationCommand(project, sdk)
    if (result is Result.Failure) {
      LOG.warn("Failed to install required packages")
      EduNotificationManager.showErrorNotification(project, EduPythonBundle.message("installing.requirements.failed.title"), result.error.message)
      return@launch
    }

    withContext(Dispatchers.EDT) {
      val editorManager = FileEditorManager.getInstance(project)
      val analyzer = DaemonCodeAnalyzer.getInstance(project)

      if (editorManager.hasOpenFiles()) {
        editorManager.openFiles.forEach { file ->
          file.findPsiFile(project)?.let { psiFile ->
            analyzer.restart(psiFile, this)
          }
        }
      }
    }
  }
}

private suspend fun executePackageInstallationCommand(project: Project, sdk: Sdk): PyResult<List<PythonPackage>> {
  // Manually handle case with read-only python SDK to provide better error message
  if (sdk.isReadOnly) {
    return PyResult.localizedError(EduPythonBundle.message("installing.requirements.failed.message.read.only.sdk"))
  }

  edtWriteAction {
    FileDocumentManager.getInstance().saveAllDocuments()
  }

  val packageManager = PythonPackageManager.forSdk(project, sdk)

  if (!packageManager.hasRootDependencyFile()) {
    LOG.info("No Python dependencies file found, skipping package sync")
    return PyResult.success(emptyList())
  }

  return withBackgroundProgress(project, EduPythonBundle.message("installing.requirements.progress")) {
    packageManager.syncLocked()
  }
}

fun getSupportedVersions(): List<String> {
  val pythonVersions = mutableListOf(PyLanguageEnvironmentCatalogProvider.ALL_VERSIONS, PYTHON_3_VERSION, PYTHON_2_VERSION)
  pythonVersions.addAll(LanguageLevel.entries.map { it.toString() }.reversed())
  return pythonVersions
}

private val VirtualFile.systemDependentPath: String get() = FileUtil.toSystemDependentName(path)

private val FOLDERS_TO_EXCLUDE: Array<String> = arrayOf("__pycache__", "venv")

@Service(Service.Level.PROJECT)
class InstallPackageCoroutineScope(private val coroutineScope: CoroutineScope) {
  companion object {
    fun getCoroutineScope(project: Project): CoroutineScope =  project.service<InstallPackageCoroutineScope>().coroutineScope
  }
}