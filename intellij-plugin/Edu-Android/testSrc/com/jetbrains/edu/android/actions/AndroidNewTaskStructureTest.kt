package com.jetbrains.edu.android.actions

import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.coursecreator.actions.create.MockNewStudyItemUi
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

class AndroidNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = KotlinLanguage.INSTANCE
  override val environment: String = "Android"

  override fun createMockUi(taskName: String, taskType: String): MockNewStudyItemUi =
    MockAndroidNewStudyUi(taskName, "com.example", itemType = taskType)

  @Test
  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.md")
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
              file("colors.xml")
              file("strings.xml")
              file("themes.xml")
            }
            dir("values-night") {
              file("themes.xml")
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
      file("task.md")
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
  @Test
  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.md")
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
              file("colors.xml")
              file("strings.xml")
              file("themes.xml")
            }
            dir("values-night") {
              file("themes.xml")
            }
          }
          file("AndroidManifest.xml")
        }
        dir("test") {
          file("output.txt")
          file("input.txt")
        }
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
      dir("src/test") {
        file("output.txt")
        file("input.txt")
      }
    }
  )

  @Test
  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file("task.md")
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
              file("colors.xml")
              file("strings.xml")
              file("themes.xml")
            }
            dir("values-night") {
              file("themes.xml")
            }
          }
          file("AndroidManifest.xml")
        }
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )

  @Test
  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file("task.md")
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
              file("colors.xml")
              file("strings.xml")
              file("themes.xml")
            }
            dir("values-night") {
              file("themes.xml")
            }
          }
          file("AndroidManifest.xml")
        }
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )

  @Test
  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file("task.md")
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
              file("colors.xml")
              file("strings.xml")
              file("themes.xml")
            }
            dir("values-night") {
              file("themes.xml")
            }
          }
          file("AndroidManifest.xml")
        }
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )
}
