package com.jetbrains.edu.rust

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.BuildNumber
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.text.VersionComparatorUtil
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.pluginVersion
import com.jetbrains.edu.rust.checker.RsTaskCheckerProvider
import org.rust.cargo.CargoConstants
import org.rust.ide.icons.RsIcons
import org.rust.lang.RsConstants
import javax.swing.Icon

class RsConfigurator : EduConfigurator<RsProjectSettings> {
  override val taskCheckerProvider: RsTaskCheckerProvider
    get() = RsTaskCheckerProvider()

  override val testFileName: String
    get() = ""

  override fun getMockFileName(text: String): String = RsConstants.MAIN_RS_FILE

  override val courseBuilder: EduCourseBuilder<RsProjectSettings>
    get() = RsCourseBuilder()

  override val testDirs: List<String>
    get() = listOf("tests")

  override val sourceDir: String
    get() = "src"

  override val logo: Icon
    get() = RsIcons.RUST

  override fun excludeFromArchive(project: Project, file: VirtualFile): Boolean {
    return super.excludeFromArchive(project, file) || file.name == CargoConstants.LOCK_FILE ||
           generateSequence(file, VirtualFile::getParent).any { it.name == CargoConstants.ProjectLayout.target }
  }

  override val isEnabled: Boolean
    get() {
      val rustPluginVersion = pluginVersion("org.rust.lang") ?: return false
      val tomlPluginVersion = pluginVersion("org.toml.lang") ?: return false
      // TOML plugin has incompatibility in API that we use before 0.2.140.
      // Also, starting with 203 platform, we uses API available only since 141 releases of Rust plugin.
      // so disable Rust support for all versions below 140 release for 202 platform and below 141 release for 203 branch
      val releaseNumber = if (ApplicationInfo.getInstance().build < BUILD_203) "140" else "141"

      return VersionComparatorUtil.compare(rustPluginVersion, "0.3.$releaseNumber") >= 0 &&
             VersionComparatorUtil.compare(tomlPluginVersion, "0.2.$releaseNumber") >= 0
    }

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  companion object {
    // BACKCOMPAT: 2020.2
    val BUILD_203: BuildNumber = BuildNumber.fromString("203")!!
  }
}
