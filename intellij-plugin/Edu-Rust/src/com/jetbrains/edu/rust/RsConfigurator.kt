package com.jetbrains.edu.rust

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.attributesEvaluator.AttributesEvaluator
import com.jetbrains.edu.rust.checker.RsTaskCheckerProvider
import com.jetbrains.edu.rust.environment.RsLanguageEnvironment
import org.rust.cargo.CargoConstants
import javax.swing.Icon

class RsConfigurator : EduConfigurator<RsLanguageEnvironment> {
  override val taskCheckerProvider: RsTaskCheckerProvider
    get() = RsTaskCheckerProvider()

  override val testFileName: String
    get() = ""

  override val courseBuilder: EduCourseBuilder<RsLanguageEnvironment>
    get() = RsCourseBuilder()

  override val testDirs: List<String>
    get() = listOf("tests")

  override val sourceDir: String
    get() = "src"

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Rust

  override val courseFileAttributesEvaluator: AttributesEvaluator = AttributesEvaluator(super.courseFileAttributesEvaluator) {
    dir(".cargo", direct = true) {
      name(CargoConstants.CONFIG_TOML_FILE, CargoConstants.CONFIG_FILE, direct = true) {
        @Suppress("DEPRECATION")
        undoLegacyExcludeFromArchive()
        archiveInclusionPolicy(ArchiveInclusionPolicy.SHOULD_BE_INCLUDED)
      }
      @Suppress("DEPRECATION")
      undoLegacyExcludeFromArchive()
    }

    dirAndChildren(CargoConstants.ProjectLayout.target) {
      @Suppress("DEPRECATION")
      legacyExcludeFromArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }

    file(CargoConstants.LOCK_FILE) {
      @Suppress("DEPRECATION")
      legacyExcludeFromArchive()
    }

    file("Cargo.toml") {
      archiveInclusionPolicy(ArchiveInclusionPolicy.SHOULD_BE_INCLUDED)
    }
  }

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

}
