package com.jetbrains.edu.python.hyperskill

import com.intellij.lang.Language
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyHyperskillNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = PythonLanguage.INSTANCE
  override val settings: Any get() = PyNewProjectSettings()
  override val courseProducer: () -> Course = ::HyperskillCourse

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    // Hyperskill python support is not available in Android Studio
    if (!EduUtils.isAndroidStudio()) {
      super.runTestRunnable(context)
    }
  }

  fun `test create edu task`() {
    val useHtml = CCSettings.getInstance().useHtmlAsDefaultTaskFormat()
    try {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(true)

      checkEduTaskCreation(
        fullTaskStructure = {
          file("task.html")
          file("task.py")
          dir("hstest") {
            file("tests.py")
          }
        },
        taskStructureWithoutSources = {
          file("task.html")
          dir("hstest") {
            file("tests.py")
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
          file("main.py")
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
          file("main.py")
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
          file("main.py")
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
          file("main.py")
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
