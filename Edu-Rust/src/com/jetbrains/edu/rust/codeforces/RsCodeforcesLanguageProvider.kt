package com.jetbrains.edu.rust.codeforces

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.rust.toPackageName
import org.rust.cargo.CargoConstants.MANIFEST_FILE
import org.rust.lang.RsConstants.MAIN_RS_FILE

class RsCodeforcesLanguageProvider : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> = listOf("Rust")
  override val languageId: String = EduNames.RUST
  override val templateFileName: String = "codeforces.main.rs"

  override fun createTaskFiles(task: Task): List<TaskFile> {
    val fileTemplate = GeneratorUtils.getInternalTemplateText(templateFileName)

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