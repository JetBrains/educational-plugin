package com.jetbrains.edu.rust

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.BuildNumber
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.text.VersionComparatorUtil
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseDir
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
    // Cargo config file should be included into course even it's located in "hidden" `.cargo` directory
    if (file.isCargoConfigDirOrFile(project)) return false
    return super.excludeFromArchive(project, file) || file.name == CargoConstants.LOCK_FILE ||
           generateSequence(file, VirtualFile::getParent).any { it.name == CargoConstants.ProjectLayout.target }
  }

  private fun VirtualFile.isCargoConfigDirOrFile(project: Project): Boolean {
    val courseDir = project.courseDir
    val cargoDir = courseDir.findChild(".cargo") ?: return false
    if (cargoDir == this) return true
    // Cargo config file should be included into course even it's located in "hidden" directory
    return cargoDir.findChild(CargoConstants.CONFIG_TOML_FILE) == this || cargoDir.findChild(CargoConstants.CONFIG_FILE) == this
  }

  override val isEnabled: Boolean
    get() {
      val rustPluginVersion = pluginVersion("org.rust.lang") ?: return false
      return if (ApplicationInfo.getInstance().build < BUILD_223) {
        // CargoConstants.CONFIG_TOML_FILE and CargoConstants.CONFIG_FILE used above appeared only in 0.4.174
        VersionComparatorUtil.compare(rustPluginVersion, "0.4.174") >= 0
      }
      else {
        // CargoCommandLine constructor was changed in 0.4.188.
        // As a result, `CargoCommandLine.copy` signature was changed as well,
        // which is used in several places in the plugin
        VersionComparatorUtil.compare(rustPluginVersion, "0.4.188") >= 0
      }
    }

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  companion object {
    // BACKCOMPAT: 2022.2
    private val BUILD_223: BuildNumber = BuildNumber.fromString("223")!!
  }
}
