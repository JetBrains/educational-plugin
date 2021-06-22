package com.jetbrains.edu.rust.codeforces

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.rust.RsConfigurator
import com.jetbrains.edu.rust.RsProjectSettings
import com.jetbrains.edu.rust.toPackageName
import icons.EducationalCoreIcons
import org.rust.cargo.CargoConstants.MANIFEST_FILE
import org.rust.lang.RsConstants.MAIN_RS_FILE
import javax.swing.Icon

class RsCodeforcesLanguageProvider : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> = listOf("Rust")
  override val configurator: EduConfigurator<RsProjectSettings> = RsConfigurator()
  override val languageId: String = EduNames.RUST
  override val templateFileName: String = "codeforces.Rust main.rs"
  override val templateName: String = "main.rs"
  override val languageIcon: Icon = EducationalCoreIcons.RustLogo

  override fun createTaskFiles(task: Task): List<TaskFile> {
    val fileTemplate = GeneratorUtils.getJ2eeTemplateText(templateFileName)

    val packageName = task.name.replace(TASK_LEADING_SYMBOLS, "").toPackageName()
    val manifestTemplate = GeneratorUtils.getInternalTemplateText(MANIFEST_FILE, mapOf("PACKAGE_NAME" to packageName))

    return listOf(
      TaskFile(GeneratorUtils.joinPaths(task.sourceDir, MAIN_RS_FILE), fileTemplate),
      TaskFile(MANIFEST_FILE, manifestTemplate)
    )
  }

  companion object {
    private val TASK_LEADING_SYMBOLS = """^.*?\.\s""".toRegex()
  }
}