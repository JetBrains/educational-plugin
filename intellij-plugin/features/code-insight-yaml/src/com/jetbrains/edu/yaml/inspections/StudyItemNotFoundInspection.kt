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
import com.intellij.psi.util.parentOfType
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.StudyItemType.*
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateLesson
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateSection
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateStudyItemActionBase
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateTask
import com.jetbrains.edu.coursecreator.createItemMessage
import com.jetbrains.edu.coursecreator.failedToFindItemMessage
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.hasSections
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlLoader
import com.jetbrains.edu.yaml.ItemContainerContentReferenceProvider
import com.jetbrains.edu.yaml.messages.EduYAMLBundle
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequenceItem

class StudyItemNotFoundInspection : UnresolvedFileReferenceInspection() {
  override val pattern: PsiElementPattern.Capture<YAMLScalar> get() = ItemContainerContentReferenceProvider.PSI_PATTERN
  override val supportedConfigs: List<String> = listOf(LESSON_CONFIG, SECTION_CONFIG, COURSE_CONFIG)

  override fun registerProblem(holder: ProblemsHolder, element: YAMLScalar) {
    val childType = when (element.itemType) {
      COURSE_TYPE -> {
        val course = element.project.course ?: return
        if (course.hasSections) SECTION_TYPE else LESSON_TYPE
      }
      SECTION_TYPE -> LESSON_TYPE
      LESSON_TYPE -> TASK_TYPE
      else -> return
    }

    val message = childType.failedToFindItemMessage(element.textValue)
    val fix = if (isValidFilePath(element.textValue)) CreateStudyItemQuickFix(element, childType) else null
    holder.registerProblem(element, message, ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, *listOfNotNull(fix).toTypedArray())
  }

  private val YAMLScalar.itemType: StudyItemType
    get() {
      return when (containingFile.name) {
        COURSE_CONFIG -> COURSE_TYPE
        SECTION_CONFIG -> SECTION_TYPE
        LESSON_CONFIG -> LESSON_TYPE
        else -> error("Unexpected containing file `${containingFile.name}`")
      }
    }

  private class CreateStudyItemQuickFix(element: YAMLScalar, private val itemType: StudyItemType) : LocalQuickFixOnPsiElement(element) {

    override fun getFamilyName(): String = EduYAMLBundle.message("create.study.item.quick.fix.family.name")
    override fun getText(): String = itemType.createItemMessage
    // We show dialog in `invoke` so quick have to be launched not in write action
    // otherwise, this dialog will block all codeinsight in whole IDE
    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
      val scalar = startElement as YAMLScalar
      val index = scalar.parentOfType<YAMLSequenceItem>()?.itemIndex ?: return
      val name = scalar.textValue

      val actionId = when (itemType) {
        SECTION_TYPE -> CCCreateSection.ACTION_ID
        LESSON_TYPE -> CCCreateLesson.ACTION_ID
        TASK_TYPE -> CCCreateTask.ACTION_ID
        else -> return
      }
      val action = EduActionUtils.getAction(actionId)

      val configFile = startElement.containingFile.originalFile.virtualFile
      val context = DataContext { dataId ->
        when {
          CommonDataKeys.PROJECT.`is`(dataId) -> project
          CommonDataKeys.VIRTUAL_FILE_ARRAY.`is`(dataId) -> arrayOf(configFile.parent)
          CCCreateStudyItemActionBase.ITEM_INDEX.`is`(dataId) -> index
          CCCreateStudyItemActionBase.SUGGESTED_NAME.`is`(dataId) -> name
          CCCreateStudyItemActionBase.UPDATE_PARENT_CONFIG.`is`(dataId) -> false
          else -> null
        }
      }
      ActionUtil.invokeAction(action, context, ActionPlaces.UNKNOWN, null) {
        YamlLoader.loadItem(project, configFile, false)
      }
    }
  }
}
