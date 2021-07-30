package com.jetbrains.edu.learning.format.yaml

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.CheckResultDiff
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TASK_TYPE
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TASK_TYPE_WITH_FILE_IO
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTaskWithFileIO
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.VideoTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.time.ZonedDateTime
import java.util.*

class StudentYamlSerializationTest : EduTestCase() {

  fun `test student course`() {
    val course = course {}

    doTest(course, """
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |mode: Study
      |
    """.trimMargin())
  }

  fun `test hyperskill course`() {
    val course = course(courseProducer = ::HyperskillCourse) {} as HyperskillCourse

    val hyperskillProject = HyperskillProject()
    hyperskillProject.id = 111
    hyperskillProject.ideFiles = "ideFiles"
    hyperskillProject.isTemplateBased = true

    course.hyperskillProject = hyperskillProject

    doTest(course, """
      |type: hyperskill
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |mode: Study
      |
    """.trimMargin())
  }

  fun `test codeforces course`() {
    val course = course(courseProducer = ::CodeforcesCourse) {} as CodeforcesCourse
    val endDateTime = ZonedDateTime.parse("2019-08-11T15:35+03:00[Europe/Moscow]")
    course.apply {
      languageCode = "en"
      this.endDateTime = endDateTime
    }

    doTest(course, """
      |type: codeforces
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |end_date_time: ${endDateTime.toEpochSecond()}.000000000
      |mode: Study
      |
    """.trimMargin())
  }

  fun `test codeforces course without endDateTime`() {
    val course = course(courseProducer = ::CodeforcesCourse) {} as CodeforcesCourse
    course.apply {
      languageCode = "en"
    }

    doTest(course, """
      |type: codeforces
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |mode: Study
      |
    """.trimMargin())
  }

  fun `test checkio station`() {
    val station = CheckiOStation()
    station.name = "station"

    val mission = CheckiOMission()
    mission.name = "mission"

    station.addMission(mission)

    doTest(station, """
      |type: checkiO
      |content:
      |- mission
      |
    """.trimMargin())
  }

  fun `test framework lesson`() {
    val frameworkLesson = FrameworkLesson()
    frameworkLesson.addTask(EduTask("task1"))
    frameworkLesson.addTask(EduTask("task2"))
    frameworkLesson.currentTaskIndex = 1

    doTest(frameworkLesson, """
      |type: framework
      |content:
      |- task1
      |- task2
      |current_task: 1
      |
    """.trimMargin())
  }

  fun `test task`() {
    val task = courseWithFiles {
      lesson {
        eduTask()
      }
    }.findTask("lesson1", "task1")
    task.status = CheckStatus.Solved
    task.record = 1

    doTest(task, """
    |type: edu
    |status: Solved
    |record: 1
    |""".trimMargin())
  }

  fun `test choice task`() {
    val task: ChoiceTask = courseWithFiles {
      lesson {
        choiceTask(choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT))
      }
    }.findTask("lesson1", "task1") as ChoiceTask
    task.status = CheckStatus.Solved
    task.record = 1
    task.selectedVariants = mutableListOf(1)

    doTest(task, """
    |type: choice
    |is_multiple_choice: false
    |options:
    |- text: 1
    |  is_correct: true
    |- text: 2
    |  is_correct: false
    |message_correct: Congratulations!
    |message_incorrect: Incorrect solution
    |status: Solved
    |record: 1
    |selected_options:
    |- 1
    |""".trimMargin())
  }

  fun `test task with feedback`() {
    val message = "My error message"
    val expected = "A"
    val actual = "B"
    val time = Date(0)
    val task = courseWithFiles {
      lesson {
        eduTask()
      }
    }.findTask("lesson1", "task1")
    task.status = CheckStatus.Failed
    task.feedback = CheckFeedback(time, CheckResult(task.status, message, diff = CheckResultDiff(expected, actual)))
    task.record = 1

    doTest(task, """
    |type: edu
    |status: Failed
    |feedback:
    |  message: $message
    |  time: Thu, 01 Jan 1970 00:00:00 UTC
    |  expected: $expected
    |  actual: $actual
    |record: 1
    |""".trimMargin())
  }

  fun `test task with incomplete feedback`() {
    val time = Date(0)
    val task = courseWithFiles {
      lesson {
        eduTask()
      }
    }.findTask("lesson1", "task1")
    task.status = CheckStatus.Failed
    task.feedback = CheckFeedback(time, CheckResult(task.status, ""))
    task.record = 1

    doTest(task, """
    |type: edu
    |status: Failed
    |feedback:
    |  time: Thu, 01 Jan 1970 00:00:00 UTC
    |record: 1
    |""".trimMargin())
  }

  fun `test video task`() {
    val firstSrc = "https://stepikvideo.blob.core.windows.net/video/29279/1080/f3d83.mp4"
    val firstRes = "1080"
    val secondSrc = "https://stepikvideo.blob.core.windows.net/video/29279/720/8c1aa1.mp4"
    val secondRes = "720"
    val thumbnail = "https://stepikvideo.blob.core.windows.net/thumbnail/29279.jpg"

    val task: VideoTask = courseWithFiles {
      lesson {
        videoTask(sources = mapOf(firstSrc to firstRes, secondSrc to secondRes),
                  thumbnail = thumbnail)
      }
    }.findTask("lesson1", "task1") as VideoTask
    task.status = CheckStatus.Solved
    task.record = 1

    doTest(task, """
    |type: video
    |thumbnail: $thumbnail
    |sources:
    |- src: $firstSrc
    |  res: $firstRes
    |  type: video/mp4
    |  label: ${firstRes}p
    |- src: $secondSrc
    |  res: $secondRes
    |  type: video/mp4
    |  label: ${secondRes}p
    |currentTime: 0
    |status: Solved
    |record: 1
    |post_submission_on_open: true
    |""".trimMargin())
  }

  fun `test checkio mission`() {
    val checkiOMission = CheckiOMission()
    checkiOMission.code = "code"

    doTest(checkiOMission, """
    |type: checkiO
    |status: Unchecked
    |code: code
    |seconds_from_change: 0
    |
    """.trimMargin())
  }

  fun `test codeforces task`() {
    val taskFileName = "src/Task.kt"
    val taskSolution = "Task solution"
    val feedbackUrl = "https://codeforces.com/contest/1218/problem/A?locale=en"
    val status = CheckStatus.Unchecked

    val codeforcesTask = CodeforcesTask().apply {
      feedbackLink = FeedbackLink(feedbackUrl)
      this.status = status
    }
    codeforcesTask.addTaskFile(TaskFile(taskFileName, taskSolution).apply { isVisible = true })

    doTest(codeforcesTask, """
    |type: $CODEFORCES_TASK_TYPE
    |files:
    |- name: $taskFileName
    |  visible: true
    |  text: $taskSolution
    |  learner_created: false
    |feedback_link: $feedbackUrl
    |status: $status
    |""".trimMargin())
  }

  fun `test codeforces task with file io`() {
    val taskFileName = "src/Task.kt"
    val taskSolution = "Task solution"
    val feedbackUrl = "https://codeforces.com/contest/1228/problem/F?locale=ru"
    val status = CheckStatus.Unchecked

    val inputFileName = "in.txt"
    val outputFileName = "out.txt"

    val codeforcesTask = CodeforcesTaskWithFileIO(inputFileName, outputFileName).apply {
      feedbackLink = FeedbackLink(feedbackUrl)
      this.status = status
    }
    codeforcesTask.addTaskFile(TaskFile(taskFileName, taskSolution).apply { isVisible = true })

    doTest(codeforcesTask, """
    |type: $CODEFORCES_TASK_TYPE_WITH_FILE_IO
    |files:
    |- name: $taskFileName
    |  visible: true
    |  text: $taskSolution
    |  learner_created: false
    |feedback_link: $feedbackUrl
    |status: $status
    |input_file: $inputFileName
    |output_file: $outputFileName
    |""".trimMargin())
  }

  fun `test task with task files`() {
    val task = courseWithFiles {
      lesson {
        eduTask {
          taskFile("task.txt", "text")
        }
      }
    }.findTask("lesson1", "task1")

    doTest(task, """
    |type: edu
    |files:
    |- name: task.txt
    |  visible: true
    |  text: text
    |  learner_created: false
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

  fun `test task with placeholders`() {
    val task = courseWithFiles {
      lesson {
        eduTask {
          taskFile("task.txt", "<p>42 is the answer</p>") {
            placeholder(0, placeholderText = "")
          }
        }
      }
    }.findTask("lesson1", "task1")

    doTest(task, """
    |type: edu
    |files:
    |- name: task.txt
    |  visible: true
    |  placeholders:
    |  - offset: 0
    |    length: 16
    |    placeholder_text: ""
    |    initial_state:
    |      length: 16
    |      offset: 0
    |    selected: false
    |    status: Unchecked
    |  text: 42 is the answer
    |  learner_created: false
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

  fun `test learner created`() {
    val task = courseWithFiles {
      lesson {
        eduTask {
          taskFile("task.txt", "text")
        }
      }
    }.findTask("lesson1", "task1")
    task.taskFiles.values.first().isLearnerCreated = true

    doTest(task, """
    |type: edu
    |files:
    |- name: task.txt
    |  visible: true
    |  text: text
    |  learner_created: true
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

  fun `test no text for image`() {
    val task = courseWithFiles {
      lesson {
        eduTask {
          taskFile("task.png", "")
        }
      }
    }.findTask("lesson1", "task1")

    doTest(task, """
    |type: edu
    |files:
    |- name: task.png
    |  visible: true
    |  learner_created: false
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

  fun `test no text for invisible task file`() {
    val task = courseWithFiles {
      lesson {
        eduTask {
          taskFile("task.txt", "task text", false)
        }
      }
    }.findTask("lesson1", "task1")

    doTest(task, """
    |type: edu
    |files:
    |- name: task.txt
    |  visible: false
    |  text: task text
    |  learner_created: false
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

  fun `test binary file text is saved in framework lesson`() {
    val base64Text = "eAErKUpNVTA3ZjA0MDAzMVHITczM08suYTh0o+NNPdt26bgThdosKRdPVXHN/wNVUpSamJKbqldSUcKwosqLb/75qC5OmZAJs9O9Di0I/PoCAJ5FH4E="
    val gitObjectFilePath = "test/objects/b6/28add5fd4be3bdd2cdb776dfa035cc69956859"
    val task = courseWithFiles {
      frameworkLesson {
        eduTask {
          taskFile("task.txt", "task text")
          taskFile(gitObjectFilePath, base64Text, false)
        }
      }
    }.findTask("lesson1", "task1")

    doTest(task, """
    |type: edu
    |files:
    |- name: task.txt
    |  visible: true
    |  text: task text
    |  learner_created: false
    |- name: $gitObjectFilePath
    |  visible: false
    |  text: $base64Text
    |  learner_created: false
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

  fun `test huge binary file text is not saved in framework lesson`() {
    var base64Text = "eAErKUpNVTA3ZjA0MDAzMVHITczM08suYTh0o+NNPdt26bgThdosKRdPVXHN/wNVUpSamJKbqldSUcKwosqLb/75qC5OmZAJs9O9Di0I/PoCAJ5FH4E="

    //create huge fileText
    while (!EduUtils.exceedsBase64ContentLimit(base64Text)) {
      base64Text += base64Text
    }

    val gitObjectFilePath = "test/objects/b6/28add5fd4be3bdd2cdb776dfa035cc69956859"
    val task = courseWithFiles {
      frameworkLesson {
        eduTask {
          taskFile("task.txt", "task text")
          taskFile(gitObjectFilePath, base64Text, false)
        }
      }
    }.findTask("lesson1", "task1")

    doTest(task, """
    |type: edu
    |files:
    |- name: task.txt
    |  visible: true
    |  text: task text
    |  learner_created: false
    |- name: $gitObjectFilePath
    |  visible: false
    |  learner_created: false
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

  private fun doTest(item: StudyItem, expected: String) {
    val actual = YamlFormatSynchronizer.STUDENT_MAPPER.writeValueAsString(item)
    assertEquals(expected, actual)
  }
}