package com.jetbrains.edu.csharp.hyperskill

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.csharp.CSharpConfigurator
import com.jetbrains.edu.csharp.CSharpProjectSettings
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.configuration.attributesEvaluator.AttributesEvaluator
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator

/**
 * For now, we assume that Hyperskill courses can only be Unity-based
 */
class CSharpHyperskillConfigurator : HyperskillConfigurator<CSharpProjectSettings>(CSharpConfigurator()) {
  override val courseBuilder: EduCourseBuilder<CSharpProjectSettings>
    get() = CSharpHyperskillCourseBuilder()

  override val courseFileAttributesEvaluator: AttributesEvaluator = AttributesEvaluator(super.courseFileAttributesEvaluator) {
    extension("meta") {
      excludeFromArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }
  }

  override fun shouldFileBeVisibleToStudent(virtualFile: VirtualFile): Boolean =
    virtualFile.name == "Packages" || virtualFile.name == "ProjectSettings" || virtualFile.path.contains("/Assets/")
}
