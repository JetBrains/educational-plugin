package com.jetbrains.edu.yaml.inspections

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import org.jetbrains.yaml.schema.YamlJsonSchemaHighlightingInspection

class YamlJsonSchemaInspection : YamlInspectionsTestBase(YamlJsonSchemaHighlightingInspection::class) {

  fun `test course with one wrong property`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {}
    }

    testHighlighting(getCourse(), """
      |title: Test Course
      |type: coursera
      |language: Russian
      |summary: sum
      |<warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property: prop</warning>
      |programming_language: Plain text
      |programming_language_version: 1.42
      |environment: Android
      |content:
      |- lesson1
      |
    """.trimMargin("|"))
  }

  fun `test section with one wrong property`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {}
      }
    }

    testHighlighting(getCourse().items[0], """
      |<warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property: prop</warning>
      |content:
      |- lesson1
    """.trimMargin("|"))
  }

  fun `test lesson with one wrong property`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {  }
      }
    }

    testHighlighting(getCourse().items[0], """
      |type: framework
      |<warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property: prop</warning>
      |content:
      |- task1
    """.trimMargin("|"))
  }

  fun `test edu task with wrong properties on each level`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>f()</p>") {
            placeholder(0, "test")
          }
        }
      }
      lesson {
        eduTask {
          taskFile("Test.java", "<p>f()</p>") {
            placeholder(0, placeholderText = "type here", dependency = "lesson1#task1#Test.java#1")
          }
        }
      }
    }

    testHighlighting(findTask(1, 0), """
    |type: edu
    |<warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property: prop</warning>
    |files:
    |- name: Test.java
    |  visible: true
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    <warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property: prop</warning>
    |    placeholder_text: type here
    |    dependency:
    |      lesson: lesson1
    |      task: task1
    |      file: Test.java
    |      <warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property: prop</warning>
    |      placeholder: 1
    |      is_visible: true
    |""".trimMargin("|"))
  }

  fun `test choice task with wrong properties on each level`() {
    courseWithFiles(CCUtils.COURSE_MODE) {
      lesson {
        choiceTask(choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)) {
          taskFile("Test.java", "")
        }
      }
    }

    testHighlighting(findTask(0, 0), """
      |type: choice
      |<warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property: prop</warning>
      |is_multiple_choice: false
      |options:
      |- text: 1
      |  is_correct: true
      |- text: 2
      |  is_correct: false
      |  <warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property: prop</warning>
      |message_correct: Congratulations!
      |message_incorrect: Incorrect solution
      |files:
      |- name: Test.java
      |  <warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property: prop</warning>
      |  visible: true
      |""".trimMargin("|"))
  }
}
