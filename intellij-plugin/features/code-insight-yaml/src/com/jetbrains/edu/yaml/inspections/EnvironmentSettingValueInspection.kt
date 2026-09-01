package com.jetbrains.edu.yaml.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.jetbrains.edu.codeInsight.psiElement
import com.jetbrains.edu.learning.configuration.EnvironmentSettingValueType
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.isEduYamlProject
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames
import com.jetbrains.edu.yaml.messages.EduYAMLBundle
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class EnvironmentSettingValueInspection : LocalInspectionTool() {

  private val pattern: PsiElementPattern.Capture<YAMLScalar> = psiElement<YAMLScalar>()
    .withParent(
      psiElement<YAMLKeyValue>().withParent(
        psiElement<YAMLMapping>().withParent(
          psiElement<YAMLKeyValue>()
            .withName(YamlMixinNames.ENVIRONMENT_SETTINGS)
            .withSuperParent(2, psiElement<YAMLDocument>())
        )
      )
    )

  override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
    if (!file.project.isEduYamlProject() || file.name != YamlConfigSettings.COURSE_CONFIG) return emptyList()
    return super.processFile(file, manager)
  }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    val defaultSettings = holder.project.course?.configurator?.getEnvironmentSettings(holder.project) ?: return PsiElementVisitor.EMPTY_VISITOR

    return object : YamlPsiElementVisitor() {
      override fun visitScalar(settingValue: YAMLScalar) {
        if (!pattern.accepts(settingValue)) return
        val keyValue = settingValue.parent as YAMLKeyValue

        val defaultValue = defaultSettings[keyValue.keyText] ?: return
        if (defaultValue.valueType != EnvironmentSettingValueType.COURSE_DEFINED || settingValue.textValue == defaultValue.value) return

        holder.registerProblem(
          settingValue,
          EduYAMLBundle.message("yaml.editor.incorrect.environment.setting.value", defaultValue.value),
          ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        )
      }
    }
  }
}
