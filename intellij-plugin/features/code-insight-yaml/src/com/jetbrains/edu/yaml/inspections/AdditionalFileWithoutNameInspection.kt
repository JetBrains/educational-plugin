package com.jetbrains.edu.yaml.inspections

import com.intellij.codeInspection.*
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.jetbrains.edu.codeInsight.psiElement
import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.isEduYamlProject
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames
import com.jetbrains.edu.yaml.keyValueWithName
import com.jetbrains.edu.yaml.messages.EduYAMLBundle
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequenceItem
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class AdditionalFileWithoutNameInspection : LocalInspectionTool() {

  override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
    if (!file.project.isEduYamlProject() || file.name != YamlConfigSettings.COURSE_CONFIG) return emptyList()
    return super.processFile(file, manager)
  }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = object : YamlPsiElementVisitor() {
    override fun visitScalar(scalar: YAMLScalar) {
      if (!pattern.accepts(scalar)) return

      holder.registerProblem(
        scalar,
        EduYAMLBundle.message("inspection.additional.file.without.name"),
        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
      )
    }
  }

  private val pattern: PsiElementPattern.Capture<YAMLScalar> = psiElement<YAMLScalar>()
    .withParent(psiElement<YAMLSequenceItem>())
    .withSuperParent(3, keyValueWithName(YamlMixinNames.ADDITIONAL_FILES))

}