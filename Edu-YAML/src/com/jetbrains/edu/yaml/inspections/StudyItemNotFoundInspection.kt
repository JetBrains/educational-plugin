package com.jetbrains.edu.yaml.inspections

import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.Project
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.coursecreator.actions.CCCreateLesson
import com.jetbrains.edu.coursecreator.actions.CCCreateStudyItemActionBase
import com.jetbrains.edu.coursecreator.actions.CCCreateTask
import com.jetbrains.edu.coursecreator.actions.StudyItemType
import com.jetbrains.edu.coursecreator.actions.StudyItemType.*
import com.jetbrains.edu.coursecreator.actions.sections.CCCreateSection
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.COURSE_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.LESSON_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.SECTION_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlLoader
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.hasSections
import com.jetbrains.edu.yaml.ItemContainerContentReferenceProvider
import com.jetbrains.edu.yaml.parentOfType
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequenceItem

class StudyItemNotFoundInspection : UnresolvedFileReferenceInspection() {
  override val pattern: PsiElementPattern.Capture<YAMLScalar> get() = ItemContainerContentReferenceProvider.PSI_PATTERN
  override val supportedConfigs: List<String> = listOf(LESSON_CONFIG, SECTION_CONFIG, COURSE_CONFIG)

  override fun registerProblem(holder: ProblemsHolder, element: YAMLScalar) {
    val childType = when (element.itemType) {
      COURSE -> {
        val course = element.project.course ?: return
        if (course.hasSections) SECTION else LESSON
      }
      SECTION -> LESSON
      LESSON -> TASK
      else -> return
    }

    val message = "Cannot find '${element.textValue}' ${childType.presentableName}"
    val fix = if (isValidFilePath(element.textValue)) CreateStudyItemQuickFix(element, childType) else null
    holder.registerProblem(element, message, ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, *listOfNotNull(fix).toTypedArray())
  }

  private val YAMLScalar.itemType: StudyItemType get() {
    return when (containingFile.name) {
      COURSE_CONFIG -> COURSE
      SECTION_CONFIG -> SECTION
      LESSON_CONFIG -> LESSON
      else -> error("Unexpected containing file `${containingFile.name}`")
    }
  }

  private class CreateStudyItemQuickFix(element: YAMLScalar, private val itemType: StudyItemType) : LocalQuickFixOnPsiElement(element) {

    override fun getFamilyName(): String = "Create study item"
    override fun getText(): String = "Create ${itemType.presentableName}"
    // We show dialog in `invoke` so quick have to be launched not in write action
    // otherwise, this dialog will block all codeinsight in whole IDE
    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
      val scalar = startElement as YAMLScalar
      val index = scalar.parentOfType<YAMLSequenceItem>()?.itemIndex ?: return
      val name = scalar.textValue

      val action = when (itemType) {
        SECTION -> CCCreateSection()
        LESSON -> CCCreateLesson()
        TASK -> CCCreateTask()
        else -> return
      }

      val configFile = startElement.containingFile.originalFile.virtualFile
      val context = DataContext { dataId ->
        when {
          CommonDataKeys.PROJECT.`is`(dataId) -> project
          CommonDataKeys.VIRTUAL_FILE_ARRAY.`is`(dataId) -> arrayOf(configFile.parent)
          CCCreateStudyItemActionBase.ITEM_INDEX.`is`(dataId) -> index + 1
          CCCreateStudyItemActionBase.SUGGESTED_NAME.`is`(dataId) -> name
          else -> null
        }
      }
      ActionUtil.invokeAction(action, context, ActionPlaces.UNKNOWN, null, Runnable {
        // TODO: extract setting of yaml error into editor notification provider
        //  and just call `com.intellij.ui.EditorNotifications.updateNotifications` instead
        YamlLoader.loadItem(project, configFile)
      })
    }
  }
}
