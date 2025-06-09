package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.hasParams
import com.jetbrains.edu.learning.pathWithoutPrams
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import com.jetbrains.edu.learning.update.UpdateTestBase
import kotlinx.coroutines.runBlocking
import org.junit.Test

class HyperskillTopicUpdateTest : UpdateTestBase<HyperskillCourse>() {
  override fun getUpdater(localCourse: HyperskillCourse) = HyperskillCourseUpdaterNew(project, localCourse)

  override fun initiateLocalCourse() {
    localCourse = createBasicHyperskillCourse {
      section(HYPERSKILL_TOPICS) {
        lesson("Topic 1", id = 1) {
          codeTask("codeTask", stepId = 11) {
            taskFile("main.kt", "fun main() {}")
          }
        }
        lesson("Topic 2", id = 2) {
          choiceTask(
            "choiceTask", stepId = 22, choiceOptions = mapOf(
              "Option1" to ChoiceOptionStatus.UNKNOWN,
              "Option2" to ChoiceOptionStatus.UNKNOWN,
            )
          )
        }
      }
    }
  }

  @Test
  fun `test remote version of local downloaded topics added to remote course`() {
    // the local course has two topics downloaded
    initiateLocalCourse()

    // a remote course does not have a topic section
    val remoteCourse = toRemoteCourse {
      removeSection(sections[0])
    }

    val mockConnector = HyperskillConnector.getInstance() as MockHyperskillConnector
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      val responseFileName = when (request.pathWithoutPrams) {
        "/api/steps" -> when {
          request.hasParams("ids" to "11") -> "steps_topics_update11.json"
          request.hasParams("ids" to "22") -> "steps_topics_update22.json"
          else -> return@withResponseHandler MockResponseFactory.notFound()
        }
        else -> return@withResponseHandler MockResponseFactory.notFound()
      }
      mockResponse(responseFileName)
    }

    runBlocking {
      HyperskillCourseUpdater(project, localCourse).addTopicSectionToRemoteCourseIfAbsent(remoteCourse)
    }

    val remoteTopicsSection = remoteCourse.getTopicsSection()

    assertNotNull(remoteTopicsSection)
    assertEquals(2, remoteTopicsSection!!.lessons.size)

    checkTopicSectionStructure(remoteTopicsSection)

    updateCourse(remoteCourse)

    checkTopicSectionStructure(localCourse.getTopicsSection()!!)

    val expectedStructure = fileTree {
      dir("Topics") {
        dir("Topic 1") {
          dir("Pets in boxes") {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
        }
        dir("Topic 2") {
          dir("Upper-bound") {
            dir("src") {
              file("Task.kt")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  private fun checkTopicSectionStructure(section: Section) {
    val topic1 = section.lessons[0]
    assertEquals("Topic 1", topic1.name)
    val task1 = topic1.taskList.single()
    assertEquals("Pets in boxes", task1.name)
    assertEquals(11, task1.id)

    val topic2 = section.lessons[1]
    assertEquals("Topic 2", topic2.name)
    val task2 = topic2.taskList.single()
    assertEquals("Upper-bound", task2.name)
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/hyperskill/"
}