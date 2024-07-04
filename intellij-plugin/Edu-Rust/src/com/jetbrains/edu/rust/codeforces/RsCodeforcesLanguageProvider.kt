package com.jetbrains.edu.rust.codeforces

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.RUST
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.rust.RsConfigurator
import com.jetbrains.edu.rust.RsProjectSettings
import org.rust.cargo.CargoConstants.MANIFEST_FILE
import org.rust.lang.RsConstants.MAIN_RS_FILE
import javax.swing.Icon

class RsCodeforcesLanguageProvider : CodeforcesLanguageProvider() {
  override val configurator: EduConfigurator<RsProjectSettings> = RsConfigurator()
  override val languageId: String = RUST
  override val templateFileName: String = "codeforces.Rust main.rs"
  override val displayTemplateName: String = "main.rs"
  override val languageIcon: Icon = EducationalCoreIcons.RustLogo

  override fun createTaskFiles(task: Task): List<TaskFile> {
    val moduleName = GeneratorUtils.getDefaultName(task)
    task.customPresentableName = task.name
    task.name = moduleName

    val fileTemplate = GeneratorUtils.getJ2eeTemplateText(templateFileName)

    val manifestTemplate = GeneratorUtils.getInternalTemplateText(MANIFEST_FILE, mapOf("PACKAGE_NAME" to task.name))

    return listOf(
      TaskFile(GeneratorUtils.joinPaths(task.sourceDir, MAIN_RS_FILE), fileTemplate),
      TaskFile(MANIFEST_FILE, manifestTemplate)
    )
  }
}