package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.JavaUILibrary.*
import com.jetbrains.edu.learning.course
import org.jetbrains.annotations.NonNls
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets


@Suppress("ComponentNotRegistered") // registered in educational-core.xml
class PrintoutCourse : DumbAwareAction("Printout course") {

  override fun actionPerformed(e: AnActionEvent) {
    val course = e.project?.course!!

    val baos = ByteArrayOutputStream()
    val text = PrintStream(baos)

    text.println("COURSE ${course.name}")
    for (lesson in course.lessons) {
      text.println("  LESSON ${lesson.name}")
      for (task in lesson.taskList) {
        text.println("    TASK ${task.name}")
        for (taskFileEntry in task.taskFiles) {
          val taskFile = taskFileEntry.value
          text.print("      TASK FILE ${taskFileEntry.key}(${taskFile.name}):")
          if (taskFile.text == "")
            text.println(" empty text")
          else {
            text.println()
            text.println("---------------------------------")
            text.println(taskFile.text)
            text.println("---------------------------------")
          }
        }
      }
    }

    LOG.info(baos.toString(StandardCharsets.UTF_8))
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.PrintoutCourse"
    val LOG = Logger.getInstance(PrintoutCourse::class.java)
  }
}