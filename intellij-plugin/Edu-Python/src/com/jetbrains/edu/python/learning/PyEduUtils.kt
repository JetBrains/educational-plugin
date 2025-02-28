@file:JvmName("PyEduUtils")

package com.jetbrains.edu.python.learning

import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.util.progress.reportSequentialProgress
import com.jetbrains.edu.learning.configuration.attributesEvaluator.AttributesEvaluator
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON_2_VERSION
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON_3_VERSION
import com.jetbrains.edu.learning.courseFormat.ext.findTaskFileInDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isTestsFile
import com.jetbrains.edu.python.learning.messages.EduPythonBundle
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings
import com.jetbrains.python.packaging.PyPackageUtil
import com.jetbrains.python.packaging.common.PythonSimplePackageSpecification
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.psi.LanguageLevel

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
    excludeFromArchive()
  }

  extension("pyc") {
    excludeFromArchive()
  }
}

fun installRequiredPackages(project: Project, sdk: Sdk) {
  for (module in ModuleManager.getInstance(project).modules) {
    val requirements = runReadAction { PyPackageUtil.getRequirementsFromTxt(module) }
    if (requirements.isNullOrEmpty()) {
      continue
    }

    val packageManager = PythonPackageManager.forSdk(project, sdk)
    runWithModalProgressBlocking(project, EduPythonBundle.message("installing.requirements.progress")) {
      reportSequentialProgress(requirements.size) { reporter ->
        requirements.forEach {
          val spec = PythonSimplePackageSpecification(
            it.installOptions.joinToString(" "),
            version = null,
            repository = null,
            relation = null
          )

          reporter.itemStep(it.name) {
            packageManager.installPackage(spec, emptyList())
          }
        }
      }
    }

    // Clear file-level warning that might linger while skeletons are updating
    val editorManager = FileEditorManager.getInstance(project)
    val analyzer = DaemonCodeAnalyzerEx.getInstanceEx(module.project)
    if (editorManager.hasOpenFiles()) {
      editorManager.openFiles.forEach { file ->
        file.findPsiFile(project)?.let { psiFile ->
          analyzer.cleanFileLevelHighlights(Pass.LOCAL_INSPECTIONS, psiFile)
        }
      }
    }
  }
}

fun getSupportedVersions(): List<String> {
  val pythonVersions = mutableListOf(PyLanguageSettings.ALL_VERSIONS, PYTHON_3_VERSION, PYTHON_2_VERSION)
  pythonVersions.addAll(LanguageLevel.values().map { it.toString() }.reversed())
  return pythonVersions
}


private val VirtualFile.systemDependentPath: String get() = FileUtil.toSystemDependentName(path)

private val FOLDERS_TO_EXCLUDE: Array<String> = arrayOf("__pycache__", "venv")
