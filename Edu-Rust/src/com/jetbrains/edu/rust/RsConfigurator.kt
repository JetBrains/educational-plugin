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
    // BACKCOMPAT: 2021.3. Replace string constants with `CargoConstants.CONFIG_TOML_FILE` and `CargoConstants.CONFIG_FILE`
    //  and bump minimal Rust plugin version to 174
    // Cargo config file should be included into course even it's located in "hidden" directory
    return cargoDir.findChild("config.toml") == this || cargoDir.findChild("config") == this
  }

  override val isEnabled: Boolean
    get() {
      val rustPluginVersion = pluginVersion("org.rust.lang") ?: return false
      val tomlPluginVersion = pluginVersion("org.toml.lang") ?: return false

      // Rust plugin has incompatibility in API:
      //   - before 2021.3 in 0.4.147
      //   - starting 2021.3 there is another incompatibility in 0.4.165
      val minimalSupportedRustPluginVersion = if (ApplicationInfo.getInstance().build >= BUILD_213) "0.4.165" else "0.4.147"

      return VersionComparatorUtil.compare(rustPluginVersion, minimalSupportedRustPluginVersion) >= 0 &&
             VersionComparatorUtil.compare(tomlPluginVersion, "0.2.147") >= 0
    }

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  companion object {
    // BACKCOMPAT 2021.2
    private val BUILD_213 = BuildNumber.fromString("213")!!
  }
}
