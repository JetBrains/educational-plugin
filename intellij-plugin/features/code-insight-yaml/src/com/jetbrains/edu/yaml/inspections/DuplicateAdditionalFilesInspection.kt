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
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class DuplicateAdditionalFilesInspection : LocalInspectionTool() {

  override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
    if (!file.project.isEduYamlProject() || file.name != YamlConfigSettings.COURSE_CONFIG) return emptyList()
    return super.processFile(file, manager)
  }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    // collect and count all names of additional files
    val configFile = holder.file as? YAMLFile ?: return PsiElementVisitor.EMPTY_VISITOR
    val configDocument = configFile.documents.firstOrNull() ?: return PsiElementVisitor.EMPTY_VISITOR
    val configRoot = configDocument.topLevelValue as? YAMLMapping ?: return PsiElementVisitor.EMPTY_VISITOR
    val additionalFilesSequence = configRoot.getKeyValueByKey(YamlMixinNames.ADDITIONAL_FILES)?.value as? YAMLSequence ?: return PsiElementVisitor.EMPTY_VISITOR

    val allNamesCount = additionalFilesSequence.items.groupingBy {
      val fileConfig = it.value as? YAMLMapping ?: return@groupingBy null
      val name = fileConfig.getKeyValueByKey(YamlMixinNames.NAME)?.value as? YAMLScalar ?: return@groupingBy null

      name.textValue
    }.eachCount()
    val duplicateNames = allNamesCount.filter { it.value > 1 }.keys

    return object : YamlPsiElementVisitor() {
      override fun visitScalar(scalar: YAMLScalar) {
        if (!pattern.accepts(scalar)) return

        val thisName = scalar.textValue
        if (thisName in duplicateNames) {
          holder.registerProblem(
            scalar,
            EduYAMLBundle.message("yaml.editor.duplicate.additional.file", thisName),
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
          )
        }
      }
    }
  }

  private val pattern: PsiElementPattern.Capture<YAMLScalar> = psiElement<YAMLScalar>()
    .withParent(keyValueWithName(YamlMixinNames.NAME))
    .withSuperParent(5, keyValueWithName(YamlMixinNames.ADDITIONAL_FILES))

}