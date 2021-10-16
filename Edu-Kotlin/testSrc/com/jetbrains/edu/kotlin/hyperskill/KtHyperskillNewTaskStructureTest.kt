package com.jetbrains.edu.kotlin.hyperskill

import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtHyperskillNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = KotlinLanguage.INSTANCE
  override val settings: Any get() = JdkProjectSettings.emptySettings()
  override val courseProducer: () -> Course = ::HyperskillCourse

  fun `test create edu task`() {
    val useHtml = CCSettings.getInstance().useHtmlAsDefaultTaskFormat()
    try {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(true)

      checkEduTaskCreation(
        fullTaskStructure = {
          file("task.html")
          dir("src") {
            file("Task.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
        },
        taskStructureWithoutSources = {
          file("task.html")
          dir("test") {
            file("Tests.kt")
          }
        }
      )
    }
    finally {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(useHtml)
    }
  }

  fun `test create output task`() {
    val useHtml = CCSettings.getInstance().useHtmlAsDefaultTaskFormat()
    try {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(true)

      checkOutputTaskCreation(
        fullTaskStructure = {
          file("task.html")
          dir("src") {
            file("Main.kt")
          }
          dir("test") {
            file("output.txt")
          }
        },
        taskStructureWithoutSources = {
          file("task.html")
          dir("test") {
            file("output.txt")
          }
        }
      )
    }
    finally {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(useHtml)
    }
  }

  fun `test create theory task`() {
    val useHtml = CCSettings.getInstance().useHtmlAsDefaultTaskFormat()
    try {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(true)

      checkTheoryTaskCreation(
        fullTaskStructure = {
          file("task.html")
          dir("src") {
            file("Main.kt")
          }
        },
        taskStructureWithoutSources = {
          file("task.html")
        }
      )
    }
    finally {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(useHtml)
    }
  }

  fun `test create IDE task`() {
    val useHtml = CCSettings.getInstance().useHtmlAsDefaultTaskFormat()
    try {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(true)

      checkIdeTaskCreation(
        fullTaskStructure = {
          file("task.html")
          dir("src") {
            file("Main.kt")
          }
        },
        taskStructureWithoutSources = {
          file("task.html")
        }
      )
    }
    finally {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(useHtml)
    }
  }

  fun `test create choice task`() {
    val useHtml = CCSettings.getInstance().useHtmlAsDefaultTaskFormat()
    try {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(true)

      checkChoiceTaskCreation(
        fullTaskStructure = {
          file("task.html")
          dir("src") {
            file("Main.kt")
          }
        },
        taskStructureWithoutSources = {
          file("task.html")
        }
      )
    }
    finally {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(useHtml)
    }
  }
}
