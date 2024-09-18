package com.jetbrains.edu.yaml.inspections

import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import org.jetbrains.yaml.schema.YamlJsonSchemaHighlightingInspection
import org.junit.Test

class YamlJsonSchemaInspection : YamlInspectionsTestBase(YamlJsonSchemaHighlightingInspection::class) {

  @Test
  fun `test course with one wrong property`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {}
    }

    testHighlighting(getCourse(), """
      |title: Test Course
      |type: coursera
      |language: Russian
      |summary: sum
      |<warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property</warning>: prop
      |programming_language: Plain text
      |programming_language_version: 1.42
      |environment: Android
      |content:
      |- lesson1
      |yaml_version: 1
      |
    """.trimMargin("|"))
  }

  @Test
  fun `test course with marketplace course type`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {}
    }

    testHighlighting(getCourse(), """
      |title: Test Course
      |type: marketplace
      |language: Russian
      |summary: sum
      |<warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property</warning>: prop
      |programming_language: Plain text
      |programming_language_version: 1.42
      |environment: Android
      |content:
      |- lesson1
      |yaml_version: 1
      |
    """.trimMargin("|"))
  }

  @Test
  fun `test course without yaml_version`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {}
    }

    testHighlighting(getCourse(), """
      |<warning descr="Schema validation: Missing required property 'yaml_version'">title: Test Course</warning>
      |type: coursera
      |language: Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |environment: Android
      |content:
      |- lesson1
      |
    """.trimMargin("|"))
  }

  @Test
  fun `test section with one wrong property`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson {}
      }
    }

    testHighlighting(getCourse().items[0], """
      |<warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property</warning>: prop
      |content:
      |- lesson1
    """.trimMargin("|"))
  }

  @Test
  fun `test lesson with one wrong property`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask { }
      }
    }

    testHighlighting(getCourse().items[0], """
      |type: framework
      |<warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property</warning>: prop
      |content:
      |- task1
    """.trimMargin("|"))
  }

  @Test
  fun `test edu task with wrong properties on each level`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>f()</p>") {
            placeholder(index = 0, possibleAnswer = "test")
          }
        }
      }
      lesson {
        eduTask {
          taskFile("Test.java", "<p>f()</p>") {
            placeholder(index = 0, placeholderText = "type here", dependency = "lesson1#task1#Test.java#1")
          }
        }
      }
    }

    testHighlighting(findTask(1, 0), """
    |type: edu
    |<warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property</warning>: prop
    |files:
    |- name: Test.java
    |  visible: true
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    <warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property</warning>: prop
    |    placeholder_text: type here
    |    dependency:
    |      lesson: lesson1
    |      task: task1
    |      file: Test.java
    |      <warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property</warning>: prop
    |      placeholder: 1
    |      is_visible: true
    |""".trimMargin("|"))
  }

  @Test
  fun `test choice task with wrong properties on each level`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        choiceTask(choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)) {
          taskFile("Test.java", "")
        }
      }
    }

    testHighlighting(findTask(0, 0), """
      |type: choice
      |<warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property</warning>: prop
      |is_multiple_choice: false
      |options:
      |- text: 1
      |  is_correct: true
      |- text: 2
      |  is_correct: false
      |  <warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property</warning>: prop
      |files:
      |- name: Test.java
      |  <warning descr="Schema validation: Property 'wrong_property' is not allowed">wrong_property</warning>: prop
      |  visible: true
      |""".trimMargin("|"))
  }
}
