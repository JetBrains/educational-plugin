package com.jetbrains.edu.csharp.hyperskill

import com.jetbrains.edu.csharp.CSharpConfigurator
import com.jetbrains.edu.csharp.CSharpConfigurator.Companion.ASSETS_DIRECTORY
import com.jetbrains.edu.csharp.CSharpConfigurator.Companion.PACKAGES_DIRECTORY
import com.jetbrains.edu.csharp.CSharpConfigurator.Companion.PROJECT_SETTINGS_DIRECTORY
import com.jetbrains.edu.csharp.CSharpProjectSettings
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.configuration.CourseViewVisibility
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
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }

    dirAndChildren(PACKAGES_DIRECTORY, PROJECT_SETTINGS_DIRECTORY, ASSETS_DIRECTORY) {
      courseViewVisibility(CourseViewVisibility.VISIBLE_FOR_STUDENT)
    }
  }
}
