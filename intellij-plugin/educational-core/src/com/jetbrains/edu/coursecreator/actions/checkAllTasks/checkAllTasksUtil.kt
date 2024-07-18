package com.jetbrains.edu.coursecreator.actions.checkAllTasks

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import javax.swing.event.HyperlinkEvent

/**
 * Checks all tasks that are in [studyItems] and returns a list of failed tasks
 */
/*
 * Important note: we deliberately start with checking sections, then check lessons, and then individual tasks.
 * Why? Because a person can choose out a separate task as a task located in the selected lesson / section.
 * We don't want to check such tasks twice, and that’s why we have such order of checking.
 */
fun checkAllStudyItems(
  project: Project,
  course: Course,
  studyItems: List<StudyItem>,
  indicator: ProgressIndicator
): List<Task>? {
  val failedTasks = mutableListOf<Task>()

  val (tasks, lessons, sections) = splitStudyItems(studyItems)

  val selectedCourse = studyItems.find { it is Course }
  if (selectedCourse != null) {
    return checkAllTasksInItemContainer(project, course, selectedCourse as Course, indicator)
  }

  for (section in sections) {
    failedTasks += checkAllTasksInItemContainer(project, course, section, indicator) ?: return null
  }

  for (lesson in lessons) {
    if (lesson.section in sections) continue
    failedTasks += checkAllTasksInItemContainer(project, course, lesson, indicator) ?: return null
  }

  for (task in tasks) {
    if (task.lesson in lessons || task.lesson.section in sections) continue
    indicator.isIndeterminate = true
    indicator.text = EduCoreBundle.message("progress.text.checking.task", task.name)
    if (!checkTask(project, course, task, indicator)) {
      failedTasks.add(task)
    }
    indicator.isIndeterminate = false
    if (indicator.isCanceled) {
      return null
    }
  }
  return failedTasks
}

/**
 * Split studyItems into (Tasks, Lessons, Sections)
 */
fun splitStudyItems(studyItems: List<StudyItem>): Triple<Set<Task>, Set<Lesson>, Set<Section>> {
  val tasks = mutableSetOf<Task>()
  val lessons = mutableSetOf<Lesson>()
  val sections = mutableSetOf<Section>()
  for (item in studyItems) {
    when (item) {
      is Task -> tasks += item
      is Lesson -> lessons += item
      is Section -> sections += item
    }
  }
  return Triple(tasks, lessons, sections)
}

/**
 * Checks all tasks in lesson container and returns list of failed tasks
 *
 * @return List of failed tasks, null if the indicator was cancelled
 */
fun checkAllTasksInItemContainer(
  project: Project,
  course: Course,
  itemContainer: ItemContainer,
  indicator: ProgressIndicator
): List<Task>? {
  val failedTasks = mutableListOf<Task>()
  var curTask = 0
  val tasksNum = getNumberOfTasks(itemContainer)
  val itemContainerVisitFunc = getVisitItemContainerFunc(itemContainer)
  indicator.text = EduCoreBundle.message("progress.text.checking.tasks.in.container", itemContainer.name)
  itemContainerVisitFunc.invoke {
    if (indicator.isCanceled) {
      return@invoke
    }
    curTask++
    indicator.fraction = curTask * 1.0 / tasksNum
    if (!checkTask(project, course, it, indicator)) {
      failedTasks.add(it)
    }
  }
  if (indicator.isCanceled) {
    return null
  }
  return failedTasks
}

fun checkTask(
  project: Project,
  course: Course,
  task: Task,
  indicator: ProgressIndicator
): Boolean {
  if (task is TheoryTask || (task is ChoiceTask && task.selectedVariants.isEmpty())) {
    return true
  }

  val checker = course.configurator?.taskCheckerProvider?.getTaskChecker(task, project)!!
  if (checker is EduTaskCheckerBase) {
    checker.activateRunToolWindow = false
  }
  val checkResult = checker.check(indicator)
  checker.clearState()
  return checkResult.status == CheckStatus.Solved
}

/**
 * @Returns Number of tasks in [itemContainer]
 */
fun getNumberOfTasks(itemContainer: ItemContainer): Int {
  var ans = 0
  getVisitItemContainerFunc(itemContainer).invoke { ans++ }
  return ans
}

/**
 * @Returns summary number of tasks in study items
 */
fun getNumberOfTasks(studyItems: List<StudyItem>): Int {
  var sum = 0

  val selectedCourse = studyItems.find { it is Course }
  if (selectedCourse != null) {
    return getNumberOfTasks(selectedCourse as Course)
  }

  val (tasks, lessons, sections) = splitStudyItems(studyItems)

  sum += sections.sumOf { getNumberOfTasks(it) }

  sum += lessons.sumOf {
    if (it.section !in sections) getNumberOfTasks(it)
    else 0
  }

  sum += tasks.filter { it.lesson !in lessons && it.lesson.section !in sections }.size

  return sum
}

fun getVisitItemContainerFunc(itemContainer: ItemContainer): (((Task) -> Unit) -> Unit) {
  return when (itemContainer) {
    is LessonContainer -> itemContainer::visitTasks
    is Lesson -> itemContainer::visitTasks
    else -> error("Unable to get the number of tasks of ItemContainer that are not a Lesson or LessonContainer")
  }
}

fun showFailedTasksNotification(project: Project, failedTasks: List<Task>, tasksNum: Int) {
  EduNotificationManager
    .create(WARNING, EduCoreBundle.message("notification.title.check"), notificationContent(failedTasks))
    .apply {
      subtitle = EduCoreBundle.message("notification.subtitle.some.tasks.failed", failedTasks.size, tasksNum)
      setListener(object : NotificationListener.Adapter() {
        override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
          notification.hideBalloon()
          NavigationUtils.navigateToTask(project, failedTasks[Integer.valueOf(e.description)])
          EduCounterUsageCollector.taskNavigation(EduCounterUsageCollector.TaskNavigationPlace.CHECK_ALL_NOTIFICATION)
        }
      })
      if (failedTasks.size > 1) {
        addAction(object : AnAction(EduCoreBundle.lazyMessage("action.open.first.failed.task.text")) {
          override fun actionPerformed(e: AnActionEvent) {
            this@apply.hideBalloon()
            NavigationUtils.navigateToTask(project, failedTasks.first())
            EduCounterUsageCollector.taskNavigation(EduCounterUsageCollector.TaskNavigationPlace.CHECK_ALL_NOTIFICATION)
          }
        })
      }
    }.notify(project)
}

@Suppress("UnstableApiUsage")
@NlsSafe
private fun notificationContent(failedTasks: List<Task>): String =
  failedTasks.withIndex().joinToString("<br>") {
    "<a href=\"${it.index}\">${it.value.fullName}</a>"
  }

private val Task.fullName: String
  get() = listOfNotNull(lesson.section, lesson, this).joinToString("/") { it.presentableName }