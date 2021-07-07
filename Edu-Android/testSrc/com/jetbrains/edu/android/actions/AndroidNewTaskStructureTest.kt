package com.jetbrains.edu.android.actions

import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.coursecreator.actions.create.MockNewStudyItemUi
import com.jetbrains.edu.jvm.JdkProjectSettings
import org.jetbrains.kotlin.idea.KotlinLanguage

class AndroidNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = KotlinLanguage.INSTANCE
  override val settings: Any get() = JdkProjectSettings.emptySettings()
  override val environment: String = "Android"

  override fun createMockUi(taskName: String, taskType: String): MockNewStudyItemUi =
    MockAndroidNewStudyUi(taskName, "com.example", itemType = taskType)

  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("build.gradle")
      dir("src") {
        dir("main") {
          dir("java/com/example") {
            file("MainActivity.kt")
          }
          dir("res") {
            dir("layout") {
              file("activity_main.xml")
            }
            dir("values") {
              file("styles.xml")
              file("strings.xml")
              file("colors.xml")
            }
          }
          file("AndroidManifest.xml")
        }
        dir("test/java/com/example") {
          file("ExampleUnitTest.kt")
        }
        dir("androidTest/java/com/example") {
          file("AndroidEduTestRunner.kt")
          file("ExampleInstrumentedTest.kt")
        }
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("src") {
        dir("test/java/com/example") {
          file("ExampleUnitTest.kt")
        }
        dir("androidTest/java/com/example") {
          file("AndroidEduTestRunner.kt")
          file("ExampleInstrumentedTest.kt")
        }
      }
    }
  )

  // TODO: plugin should forbid creation of output task for Android
  //   because they have no sense
  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("build.gradle")
      dir("src") {
        dir("main") {
          dir("java/com/example") {
            file("MainActivity.kt")
          }
          dir("res") {
            dir("layout") {
              file("activity_main.xml")
            }
            dir("values") {
              file("styles.xml")
              file("strings.xml")
              file("colors.xml")
            }
          }
          file("AndroidManifest.xml")
        }
        dir("test") {
          file("output.txt")
        }
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("src/test") {
        file("output.txt")
      }
    }
  )

  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("build.gradle")
      dir("src") {
        dir("main") {
          dir("java/com/example") {
            file("MainActivity.kt")
          }
          dir("res") {
            dir("layout") {
              file("activity_main.xml")
            }
            dir("values") {
              file("styles.xml")
              file("strings.xml")
              file("colors.xml")
            }
          }
          file("AndroidManifest.xml")
        }
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("build.gradle")
      dir("src") {
        dir("main") {
          dir("java/com/example") {
            file("MainActivity.kt")
          }
          dir("res") {
            dir("layout") {
              file("activity_main.xml")
            }
            dir("values") {
              file("styles.xml")
              file("strings.xml")
              file("colors.xml")
            }
          }
          file("AndroidManifest.xml")
        }
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("build.gradle")
      dir("src") {
        dir("main") {
          dir("java/com/example") {
            file("MainActivity.kt")
          }
          dir("res") {
            dir("layout") {
              file("activity_main.xml")
            }
            dir("values") {
              file("styles.xml")
              file("strings.xml")
              file("colors.xml")
            }
          }
          file("AndroidManifest.xml")
        }
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )
}
