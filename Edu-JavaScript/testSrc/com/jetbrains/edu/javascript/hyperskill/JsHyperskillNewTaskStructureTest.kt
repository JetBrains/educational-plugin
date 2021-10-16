package com.jetbrains.edu.javascript.hyperskill

import com.intellij.lang.Language
import com.intellij.lang.javascript.JavascriptLanguage
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.javascript.learning.JsNewProjectSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class JsHyperskillNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = JavascriptLanguage.INSTANCE
  override val settings: Any get() = JsNewProjectSettings()
  override val courseProducer: () -> Course = ::HyperskillCourse

  fun `test create edu task`() {
    val useHtml = CCSettings.getInstance().useHtmlAsDefaultTaskFormat()
    try {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(true)

      checkEduTaskCreation(
        fullTaskStructure = {
          file("task.html")
          file("task.js")
          dir("hstest") {
            file("test.js")
          }
        },
        taskStructureWithoutSources = {
          file("task.html")
          dir("hstest") {
            file("test.js")
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
          file("task.js")
          dir("hstest") {
            file("output.txt")
          }
        },
        taskStructureWithoutSources = {
          file("task.html")
          dir("hstest") {
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
          file("task.js")
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
          file("task.js")
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
          file("task.js")
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
