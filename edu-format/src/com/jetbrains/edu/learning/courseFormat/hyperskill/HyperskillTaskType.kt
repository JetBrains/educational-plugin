package com.jetbrains.edu.learning.courseFormat.hyperskill

import com.jetbrains.edu.learning.courseFormat.RemoteEduTask.Companion.REMOTE_EDU_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask.Companion.CODE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask.Companion.DATA_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask.Companion.PYCHARM_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.NumberTask.Companion.NUMBER_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.StringTask.Companion.STRING_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask.Companion.CHOICE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask.Companion.MATCHING_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask.Companion.SORTING_TASK_TYPE

// lexicographical order
@Suppress("unused")
enum class HyperskillTaskType(val type: String, val value: String) {
  ADMIN("admin", "Linux"),
  CHOICE(CHOICE_TASK_TYPE, "Quiz"),
  CODE(CODE_TASK_TYPE, "Programming"),
  DATASET(DATA_TASK_TYPE, "Data"),
  FREE_ANSWER("free-answer", "Free Response"),
  MANUAL_SCORE("manual-score", "Manual Score"),
  MATCHING(MATCHING_TASK_TYPE, "Matching"),
  MATH("math", "Math"),
  NUMBER(NUMBER_TASK_TYPE, "Number"),
  PYCHARM(PYCHARM_TASK_TYPE, "Programming"),
  REMOTE_EDU(REMOTE_EDU_TASK_TYPE, "Programming"),
  SORTING(SORTING_TASK_TYPE, "Sorting"),
  STRING(STRING_TASK_TYPE, "Text"),
  TABLE("table", "Table"),
  TEXT("text", "Theory"),
  VIDEO("video", "Video")
}