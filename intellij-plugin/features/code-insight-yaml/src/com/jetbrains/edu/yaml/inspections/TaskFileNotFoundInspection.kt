package com.jetbrains.edu.yaml.inspections

import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.removeUserData
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.learning.yaml.YamlLoader
import com.jetbrains.edu.yaml.EduYamlEduFilePathReferenceProvider
import com.jetbrains.edu.yaml.messages.EduYAMLBundle
import org.jetbrains.yaml.psi.YAMLScalar
import java.io.IOException

class TaskFileNotFoundInspection : EduFileNotFoundInspection() {
  override val pattern: PsiElementPattern.Capture<YAMLScalar> get() = EduYamlEduFilePathReferenceProvider.TASK_FILES_PSI_PATTERN
  override val supportedConfigs: List<String> = listOf(YamlConfigSettings.TASK_CONFIG)
  override val isInTask: Boolean = true
}

class AdditionalFileNotFoundInspection : EduFileNotFoundInspection() {
  override val pattern: PsiElementPattern.Capture<YAMLScalar> get() = EduYamlEduFilePathReferenceProvider.ADDITIONAL_FILES_PSI_PATTERN
  override val supportedConfigs: List<String> = listOf(YamlConfigSettings.COURSE_CONFIG)
  override val isInTask: Boolean = false
}

abstract class EduFileNotFoundInspection : UnresolvedFileReferenceInspection() {

  abstract val isInTask: Boolean

  override fun registerProblem(holder: ProblemsHolder, element: YAMLScalar) {
    val path = element.textValue
    if (isValidFilePath(path)) {
      holder.registerProblem(element, EduYAMLBundle.message("cannot.find.file", path),
                             ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, *listOfNotNull(CreateTaskFileFix(element, isInTask)).toTypedArray())
    }
    else {
      holder.registerProblem(element, EduYAMLBundle.message("file.invalid.path", path), ProblemHighlightType.ERROR)
    }
  }

  private class CreateTaskFileFix(element: YAMLScalar, private val isInTask: Boolean) : LocalQuickFixOnPsiElement(element) {

    override fun getFamilyName(): String = EduYAMLBundle.message("create.file.quick.fix.family.name")
    override fun getText(): String = EduYAMLBundle.message("create.file.quick.fix.family.name")

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
      val scalar = startElement as YAMLScalar
      val rootDir = if (isInTask) {
        val task = file.virtualFile?.getContainingTask(project) ?: return
        task.getDir(project.courseDir) ?: return
      }
      else {
        project.courseDir
      }

      val path = scalar.textValue
      val configFile = file.originalFile.virtualFile

      try {
        // Creating a file in the task folder fires listeners that add the new task file to the task.
        // They also rewrite YAML, but we want to avoid this.
        // We thus do it while YAML is prevented from modifications.
        configFile?.putUserData(YamlFormatSynchronizer.SAVE_TO_CONFIG, false)
        GeneratorUtils.createTextChildFile(project, rootDir, path, "")

        // After we re reload the config file, the Task object will be updated automatically.
        // So we are not going to add a newly created TaskFile to the Task here explicitly.

        if (configFile != null) {
          YamlLoader.loadItem(project, configFile, false)
        }
      }
      catch (e: IOException) {
        LOG.warn(e)
        ApplicationManager.getApplication().invokeLater {
          Messages.showErrorDialog(EduYAMLBundle.message("failed.create.file.message", path),
                                   EduYAMLBundle.message("failed.create.file.title"))
        }
      }
      finally {
        configFile?.removeUserData(YamlFormatSynchronizer.SAVE_TO_CONFIG)
      }
    }

    companion object {
      private val LOG: Logger = Logger.getInstance(CreateTaskFileFix::class.java)
    }
  }
}
