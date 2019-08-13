package com.jetbrains.edu.yaml.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.util.PathUtil
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.isEduYamlProject
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

abstract class UnresolvedFileReferenceInspection : LocalInspectionTool() {

  override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
    if (!file.project.isEduYamlProject() || file.name !in supportedConfigs) return emptyList()
    return super.processFile(file, manager)
  }

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : YamlPsiElementVisitor() {
      override fun visitScalar(scalar: YAMLScalar) {
        if (!pattern.accepts(scalar)) return
        checkPathElement(holder, scalar)
      }
    }
  }

  protected open fun checkPathElement(holder: ProblemsHolder, element: YAMLScalar) {
    for (reference in element.references) {
      if (reference is FileReference && !reference.isSoft && reference.isLast && reference.multiResolve(false).isEmpty()) {
        registerProblem(holder, element)
      }
    }
  }

  protected fun isValidFilePath(path: String): Boolean {
    val segments = path.split(VfsUtil.VFS_SEPARATOR_CHAR)
    for (segment in segments) {
      // TODO: Maybe we want to check it for all OS?
      if (!PathUtil.isValidFileName(segment)) return false
    }
    return true
  }

  protected abstract val pattern: PsiElementPattern.Capture<YAMLScalar>
  protected abstract val supportedConfigs: List<String>
  protected abstract fun registerProblem(holder: ProblemsHolder, element: YAMLScalar)
}
