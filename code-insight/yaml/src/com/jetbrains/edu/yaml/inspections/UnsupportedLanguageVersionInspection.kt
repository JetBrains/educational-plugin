package com.jetbrains.edu.yaml.inspections

import com.intellij.codeInspection.*
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.jetbrains.edu.codeInsight.inFileWithName
import com.jetbrains.edu.codeInsight.psiElement
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.messages.EduFormatBundle
import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.isEduYamlProject
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames
import com.jetbrains.edu.yaml.keyValueWithName
import com.jetbrains.edu.yaml.messages.EduYAMLBundle
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class UnsupportedLanguageVersionInspection : LocalInspectionTool() {

  override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
    if (!file.project.isEduYamlProject() || file.name != YamlConfigSettings.COURSE_CONFIG) return emptyList()
    return super.processFile(file, manager)
  }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : YamlPsiElementVisitor() {
      override fun visitScalar(scalar: YAMLScalar) {
        if (!pattern.accepts(scalar)) return
        val course = scalar.project.course ?: return
        val supportedLanguageVersions = course.configurator?.courseBuilder?.getSupportedLanguageVersions()
                                        ?: formatError(
                                          EduFormatBundle.message(
                                            "yaml.editor.invalid.unsupported.language", course.languageDisplayName
                                          )
                                        )
        val languageVersion = course.languageVersion ?: return
        if (!supportedLanguageVersions.contains(languageVersion)) {
          holder.registerProblem(
            scalar,
            EduYAMLBundle.message("yaml.editor.invalid.unsupported.language.with.version", course.languageDisplayName, scalar.textValue),
            ProblemHighlightType.ERROR
          )
        }
      }
    }
  }

  val pattern: PsiElementPattern.Capture<YAMLScalar> = psiElement<YAMLScalar>()
    .inFileWithName(YamlConfigSettings.COURSE_CONFIG)
    .withParent(
      keyValueWithName(YamlMixinNames.PROGRAMMING_LANGUAGE_VERSION)
    )

}
