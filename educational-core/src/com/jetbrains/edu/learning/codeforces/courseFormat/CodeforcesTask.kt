package com.jetbrains.edu.learning.codeforces.courseFormat

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.EduNames.GO
import com.jetbrains.edu.learning.EduNames.JAVA
import com.jetbrains.edu.learning.EduNames.JAVASCRIPT
import com.jetbrains.edu.learning.EduNames.KOTLIN
import com.jetbrains.edu.learning.EduNames.PYTHON
import com.jetbrains.edu.learning.EduNames.PYTHON_2_VERSION
import com.jetbrains.edu.learning.EduNames.PYTHON_3_VERSION
import com.jetbrains.edu.learning.EduNames.RUST
import com.jetbrains.edu.learning.EduNames.SCALA
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_SUBMIT
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTaskBase

open class CodeforcesTask : OutputTaskBase() {

  override val itemType: String = CODEFORCES_TASK_TYPE
  var problemIndex: String = ""

  override val course: CodeforcesCourse
    get() = super.course as CodeforcesCourse

  companion object {
    private val LOG: Logger = Logger.getInstance(CodeforcesTask::class.java)

    fun codeforcesSubmitLink(task: CodeforcesTask): String {
      val course = task.course
      return "${course.getContestUrl()}/${CODEFORCES_SUBMIT}?locale=${course.languageCode}" +
             "&programTypeId=${course.programTypeId ?: codeforcesDefaultProgramTypeId(course)}" +
             "&submittedProblemIndex=${task.problemIndex}"
    }

    fun codeforcesTaskLink(task: CodeforcesTask): String {
      val course = task.course
      return "${course.getContestUrl()}/problem/${task.problemIndex}?locale=${course.languageCode}"
    }

    @Deprecated("Only for backwards compatibility. Use CodeforcesCourse.programTypeId")
    fun codeforcesDefaultProgramTypeId(course: CodeforcesCourse): String? {
      val languageId = course.languageId
      val languageVersion = course.languageVersion
      return when {
        GO == languageId -> GO_TYPE_ID
        JAVA == languageId && "8" == languageVersion-> JAVA_8_TYPE_ID
        JAVA == languageId && "11" == languageVersion -> JAVA_11_TYPE_ID
        JAVASCRIPT == languageId -> JAVASCRIPT_TYPE_ID
        KOTLIN == languageId -> KOTLIN_TYPE_ID
        PYTHON == languageId && PYTHON_2_VERSION == languageVersion -> PYTHON_2_TYPE_ID
        PYTHON == languageId && PYTHON_3_VERSION == languageVersion -> PYTHON_3_TYPE_ID
        RUST == languageId -> RUST_TYPE_ID
        SCALA == languageId -> SCALA_TYPE_ID
        //only for tests
        "TEXT" == languageId -> TEXT_TYPE_ID
        else -> {
          LOG.warn("Programming language was not detected: $languageId $languageVersion")
          null
        }
      }?.toString()
    }

    // For backwards compatibility. Don't use or update it
    private const val GO_TYPE_ID = 32
    private const val JAVA_8_TYPE_ID = 36
    private const val JAVA_11_TYPE_ID = 60
    private const val JAVASCRIPT_TYPE_ID = 34
    private const val KOTLIN_TYPE_ID = 48
    private const val PYTHON_2_TYPE_ID = 7
    private const val PYTHON_3_TYPE_ID = 31
    private const val RUST_TYPE_ID = 75
    private const val SCALA_TYPE_ID = 20
    //only for tests
    private const val TEXT_TYPE_ID = 0
  }
}