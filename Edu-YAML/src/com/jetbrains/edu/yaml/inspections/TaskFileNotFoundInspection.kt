package com.jetbrains.edu.yaml.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.util.PathUtil
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.isEduYamlProject
import com.jetbrains.edu.coursecreator.yaml.YamlLoader
import com.jetbrains.edu.coursecreator.yaml.format.VISIBLE
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.yaml.EduYamlTaskFilePathReferenceProvider
import com.jetbrains.edu.yaml.parentOfType
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequenceItem
import org.jetbrains.yaml.psi.YamlPsiElementVisitor
import java.io.IOException

class TaskFileNotFoundInspection : LocalInspectionTool() {

  override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
    if (!file.project.isEduYamlProject() || file.name != YamlFormatSettings.TASK_CONFIG) return emptyList()
    return super.processFile(file, manager)
  }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : YamlPsiElementVisitor() {
      override fun visitScalar(scalar: YAMLScalar) {
        if (!EduYamlTaskFilePathReferenceProvider.PSI_PATTERN.accepts(scalar)) return
        for (reference in scalar.references) {
          if (reference is FileReference && !reference.isSoft && reference.isLast && reference.multiResolve(false).isEmpty()) {
            val fix = if (isValidFilePath(scalar.textValue)) CreateTaskFileFix(scalar) else null
            // TODO: shouldn't be reported if path is invalid
            holder.registerProblem(scalar, "Cannot find '${scalar.textValue}' file",
                                   ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, *listOfNotNull(fix).toTypedArray())
          }
        }
      }
    }
  }

  private fun isValidFilePath(path: String): Boolean {
    val segments = path.split(VfsUtil.VFS_SEPARATOR_CHAR)
    for (segment in segments) {
      // TODO: Maybe we want to check it for all OS?
      if (!PathUtil.isValidFileName(segment)) return false
    }
    return true
  }

  private class CreateTaskFileFix(element: YAMLScalar) : LocalQuickFixOnPsiElement(element) {

    override fun getFamilyName(): String = "Create file"
    override fun getText(): String = familyName

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
      val scalar = startElement as YAMLScalar
      val task = EduUtils.getTaskForFile(project, file.virtualFile) ?: return
      val taskDir = task.getTaskDir(project) ?: return

      val mapping = scalar.parentOfType<YAMLMapping>() ?: return
      val isVisible = mapping.getKeyValueByKey(VISIBLE)?.valueText != "false"
      val sequenceItem = mapping.parentOfType<YAMLSequenceItem>() ?: return

      val index = sequenceItem.itemIndex
      val path = scalar.textValue
      val taskFile = TaskFile(path, "")
      taskFile.isVisible = isVisible
      // We have to add task file first to keep order
      task.addTaskFile(taskFile, index)
      try {
        GeneratorUtils.createChildFile(taskDir, path, "")
        val virtualFile = file.originalFile.virtualFile
        if (virtualFile != null) {
          updateEditorNotifications(project, virtualFile)
        }
      } catch (e: IOException) {
        LOG.warn(e)
        // Remove added task file to keep correct task structure
        task.taskFiles.remove(path)
        ApplicationManager.getApplication().invokeLater {
          Messages.showErrorDialog("Failed to create `$path` file", "Failed to create file")
        }
      }
    }

    // TODO: extract setting of yaml error into editor notification provider
    //  and just call `com.intellij.ui.EditorNotifications.updateNotifications` instead
    private fun updateEditorNotifications(project: Project, file: VirtualFile) {
      YamlLoader.loadItem(project, file)
    }

    companion object {
      private val LOG: Logger = Logger.getInstance(CreateTaskFileFix::class.java)
    }
  }
}
