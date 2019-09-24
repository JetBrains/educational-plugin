package com.jetbrains.edu.learning.format.yaml

import com.jetbrains.edu.coursecreator.yaml.YamlTestCase
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.yaml.format.getChangeApplierForItem

class StudentChangeApplierTest() : YamlTestCase() {

  fun `test edu task cc fields applied`() {
    val existingItem = courseWithFiles {
      lesson {
        eduTask("task1")
      }
    }.lessons.first().taskList.first()
    val deserializedItem = EduTask("task1")
    deserializedItem.customPresentableName = "custom name"
    deserializedItem.feedbackLink = FeedbackLink("test")

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
    val deserializedItem = ChoiceTask("task1")
    deserializedItem.choiceOptions = listOf ( ChoiceOption("right", ChoiceOptionStatus.CORRECT), ChoiceOption("second", ChoiceOptionStatus.INCORRECT))
    deserializedItem.record = 1

    getChangeApplierForItem(project, existingItem).applyChanges(existingItem, deserializedItem)

    assertEquals(deserializedItem.record, existingItem.record)
    assertEquals(deserializedItem.choiceOptions, (existingItem as ChoiceTask).choiceOptions)
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
    deserializedItem.feedbackLink = FeedbackLink("test")
    deserializedItem.taskFiles = mapOf("taskFile.txt" to TaskFile("taskFile.txt", "new code"))

    getChangeApplierForItem(project, existingItem).applyChanges(existingItem, deserializedItem)

    assertEquals(deserializedItem.taskFiles.values.first().text, existingItem.taskFiles.values.first().text)
  }

  fun `test checkio mission`() {
    val existingMission = courseWithFiles(courseMode = EduNames.STUDY) {
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