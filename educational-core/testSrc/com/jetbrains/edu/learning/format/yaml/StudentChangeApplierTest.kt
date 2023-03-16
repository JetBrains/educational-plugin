package com.jetbrains.edu.learning.format.yaml

import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.yaml.YamlTestCase
import com.jetbrains.edu.learning.yaml.format.getChangeApplierForItem

class StudentChangeApplierTest : YamlTestCase() {

  fun `test edu task cc fields applied`() {
    val existingItem = courseWithFiles {
      lesson {
        eduTask("task1")
      }
    }.lessons.first().taskList.first()
    val deserializedItem = EduTask("task1")
    deserializedItem.customPresentableName = "custom name"
    deserializedItem.feedbackLink = "test"

    getChangeApplierForItem(project, existingItem).applyChanges(existingItem, deserializedItem)

    assertEquals(deserializedItem.record, existingItem.record)
    @Suppress("DEPRECATION")
    assertEquals(deserializedItem.customPresentableName, existingItem.customPresentableName)
    assertEquals(deserializedItem.feedbackLink, existingItem.feedbackLink)
  }

  fun `test edu task student fields applied`() {
    val existingItem = courseWithFiles {
      lesson {
        eduTask("task1")
      }
    }.lessons.first().taskList.first()
    val deserializedItem = EduTask("task1")
    deserializedItem.record = 1

    getChangeApplierForItem(project, existingItem).applyChanges(existingItem, deserializedItem)

    assertEquals(deserializedItem.record, existingItem.record)
  }

  fun `test choice task student fields applied`() {
    val existingItem = courseWithFiles {
      lesson {
        choiceTask(isMultipleChoice = true, choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT))
      }
    }.lessons.first().taskList.first()
    val deserializedItem = ChoiceTask()
    deserializedItem.name = "task1"
    deserializedItem.choiceOptions = listOf(ChoiceOption("right", ChoiceOptionStatus.CORRECT),
                                            ChoiceOption("second", ChoiceOptionStatus.INCORRECT))
    deserializedItem.record = 1

    getChangeApplierForItem(project, existingItem).applyChanges(existingItem, deserializedItem)

    assertEquals(deserializedItem.record, existingItem.record)
    assertEquals(deserializedItem.choiceOptions, (existingItem as ChoiceTask).choiceOptions)
  }

  fun `test sorting task student fields applied`() {
    val existingItem = courseWithFiles {
      lesson {
        sortingTask(options = listOf("1", "2"))
      }
    }.lessons.first().taskList.first()
    val deserializedItem = SortingTask()
    deserializedItem.name = "task1"
    deserializedItem.options = listOf("right", "second")
    deserializedItem.record = 1

    getChangeApplierForItem(project, existingItem).applyChanges(existingItem, deserializedItem)

    assertEquals(deserializedItem.record, existingItem.record)
    assertEquals(deserializedItem.options, (existingItem as SortingTask).options)
    assertEquals(deserializedItem.ordering, existingItem.ordering)
  }

  fun `test matching task student fields applied`() {
    val existingItem = courseWithFiles {
      lesson {
        matchingTask(options = listOf("1", "2"))
      }
    }.lessons.first().taskList.first()
    val deserializedItem = MatchingTask()
    deserializedItem.name = "task1"
    deserializedItem.options = listOf("right", "second")
    deserializedItem.captions = listOf("1", "2")
    deserializedItem.record = 1

    getChangeApplierForItem(project, existingItem).applyChanges(existingItem, deserializedItem)

    assertEquals(deserializedItem.record, existingItem.record)
    assertEquals(deserializedItem.options, (existingItem as MatchingTask).options)
    assertEquals(deserializedItem.captions, existingItem.captions)
    assertEquals(deserializedItem.ordering, existingItem.ordering)
  }

  fun `test task file fields applied`() {
    val existingItem = courseWithFiles {
      lesson {
        eduTask("task1") {
          taskFile("taskFile.txt", "code")
        }
      }
    }.lessons.first().taskList.first()
    val deserializedItem = EduTask("task1")
    deserializedItem.customPresentableName = "custom name"
    deserializedItem.feedbackLink = "test"
    deserializedItem.taskFiles = linkedMapOf("taskFile.txt" to TaskFile("taskFile.txt", "new code"))

    getChangeApplierForItem(project, existingItem).applyChanges(existingItem, deserializedItem)

    assertEquals(deserializedItem.taskFiles.values.first().text, existingItem.taskFiles.values.first().text)
  }

  fun `test checkio mission`() {
    val existingMission = courseWithFiles(courseMode = CourseMode.STUDENT) {
      station("station") {
        mission("mission1")
      }
    }.lessons.first().taskList.first() as CheckiOMission

    val deserializedMission = CheckiOMission()
    deserializedMission.code = "code"
    deserializedMission.secondsFromLastChangeOnServer = 124

    getChangeApplierForItem(project, existingMission).applyChanges(existingMission, deserializedMission)

    assertEquals(deserializedMission.code, existingMission.code)
    assertEquals(deserializedMission.secondsFromLastChangeOnServer, existingMission.secondsFromLastChangeOnServer)
  }

}