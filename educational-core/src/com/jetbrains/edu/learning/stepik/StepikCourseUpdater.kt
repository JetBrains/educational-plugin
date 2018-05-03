package com.jetbrains.edu.learning.stepik

import com.intellij.ide.projectView.ProjectView
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.invokeAndWaitIfNeed
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils.synchronize
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.stepik.StepikConnector.getCourse
import com.jetbrains.edu.learning.stepik.StepikConnector.getCourseFromStepik
import java.io.IOException
import java.net.URISyntaxException
import java.util.*
import kotlin.collections.ArrayList

class StepikCourseUpdater(val course: RemoteCourse, val project: Project) {
  private val LOG = Logger.getInstance(this.javaClass)

  private val oldLessonDirectories = HashMap<Int, VirtualFile>()
  private val oldSectionDirectories = HashMap<Int, VirtualFile>()

  fun updateCourse() {
    oldLessonDirectories.clear()
    oldSectionDirectories.clear()
    val courseFromServer = courseFromServer(project, course)
    val (updatedLessons, newLessons) = doUpdate(courseFromServer)
    runInEdt {
      synchronize()
      ProjectView.getInstance(project).refresh()
      showNotification(newLessons, updatedLessons)
      course.configurator?.courseBuilder?.refreshProject(project)
    }
  }

  fun doUpdate(courseFromServer: Course?): Pair<Int, Int> {
    if (courseFromServer == null) {
      LOG.warn("Course ${course.id} not found on Stepik")
      return Pair(0, 0)
    }

    val newSections = courseFromServer.sections.filter { section -> section.id !in course.sectionIds }
    if (!newSections.isEmpty()) {
      createNewSections(project, newSections)
    }
    val sectionsToUpdate = courseFromServer.sections.filter { section -> section.id in course.sectionIds }
    updateSections(sectionsToUpdate)

    courseFromServer.items.withIndex().forEach({ (index, lesson) -> lesson.index = index + 1 })

    //update top level lessons
    val newLessons = courseFromServer.lessons.filter { course.getLesson(it.id) == null }
    if (!newLessons.isEmpty()) {
      createNewLessons(newLessons, project.baseDir)
    }
    val lessonsUpdated = updateLessons(courseFromServer.lessons.filter { course.getLesson(it.id) != null }, course)
    course.items = courseFromServer.items
    setCourseInfo(courseFromServer)

    return Pair(lessonsUpdated, newLessons.size)
  }

  private fun setCourseInfo(courseFromServer: Course) {
    course.name = courseFromServer.name
    course.description = courseFromServer.description
  }

  private fun showNotification(newLessons: Int,
                               updateLessonsNumber: Int) {
    val message = buildString {
      if (newLessons > 0) {
        append(if (newLessons > 1) "Loaded ${newLessons} new lessons" else "Loaded one new lesson")
        append("\n")
      }

      if (updateLessonsNumber > 0) {
        append(if (updateLessonsNumber == 1) "Updated one lesson" else "Updated $updateLessonsNumber lessons")
        append("\n")
      }
    }

    val updateNotification = Notification("Update.course", "Course updated", message, NotificationType.INFORMATION)
    updateNotification.notify(project)
  }

  @Throws(URISyntaxException::class, IOException::class)
  private fun updateLessons(lessonsFromServer: List<Lesson>, parent: ItemContainer): Int {
    var lessonsUpdated = 0
    for (lessonFromServer in lessonsFromServer) {

      lessonFromServer.taskList.withIndex().forEach { (index, task) -> task.index = index + 1 }
      val currentLesson = parent.getLesson(lessonFromServer.id)

      val taskIdsToUpdate = taskIdsToUpdate(lessonFromServer, currentLesson!!)

      val updatedTasks = ArrayList(upToDateTasks(currentLesson, taskIdsToUpdate))

      if (lessonContentChanged(taskIdsToUpdate)) {
        lessonsUpdated++
        val lessonDir = constructDir(lessonFromServer, currentLesson)
        updateTasks(taskIdsToUpdate, lessonFromServer, currentLesson, updatedTasks, lessonDir)
      }

      if (!lessonContentChanged(taskIdsToUpdate) && renamed(lessonFromServer, currentLesson)) {
        constructDir(lessonFromServer, currentLesson)
      }

      updatedTasks.sortBy { task -> task.index }
      lessonFromServer.taskList = updatedTasks
      lessonFromServer.init(course, lessonFromServer.section, false)
    }
    return lessonsUpdated
  }

  private fun lessonContentChanged(taskIdsToUpdate: List<Int>) = !taskIdsToUpdate.isEmpty()

  @Throws(URISyntaxException::class, IOException::class)
  private fun updateSections(sectionsFromServer: List<Section>) {
    val sectionsById = course.sections.associateBy{ it.id }
    for (sectionFromServer in sectionsFromServer) {
      sectionFromServer.lessons.withIndex().forEach { (index, lesson) -> lesson.index = index + 1 }

      val currentSection = sectionsById[sectionFromServer.id]

      val currentLessons = currentSection!!.lessons.map { it.id }

      val newLessons = sectionFromServer.lessons.filter { it.id !in currentLessons }
      val sectionContentChanged = !newLessons.isEmpty()
      if (sectionContentChanged) {
        val currentSectionDir = constructDir(sectionFromServer, currentSection)
        createNewLessons(newLessons, currentSectionDir)
      }
      if (!sectionContentChanged && renamed(currentSection, sectionFromServer)) {
        constructDir(sectionFromServer, currentSection)
      }

      val lessonsToUpdate = sectionFromServer.lessons.filter { it.id in currentLessons }
      updateLessons(lessonsToUpdate, currentSection)
      sectionFromServer.init(course, course, false)
    }
  }

  private fun updateTasks(taskIdsToUpdate: List<Int>,
                          lessonFromServer: Lesson,
                          currentLesson: Lesson,
                          updatedTasks: ArrayList<Task>,
                          lessonDir: VirtualFile?) {
    val serverTasksById = lessonFromServer.taskList.associateBy({ it.stepId }, { it })
    val tasksById = currentLesson.taskList.associateBy { it.stepId }
    for (taskId in taskIdsToUpdate) {
      val taskFromServer = serverTasksById[taskId]
      val taskIndex = taskFromServer!!.index
      if (tasksById.containsKey(taskId)) {
        val currentTask = tasksById[taskId]
        if ((isSolved(currentTask!!) && !isTheory(currentTask)) && course.isStudy) {
          updatedTasks.add(currentTask)
          currentTask.index = taskIndex
          currentTask.descriptionText = taskFromServer.descriptionText
          continue
        }
        removeExistingDir(currentTask, lessonDir)
      }

      taskFromServer.init(course, currentLesson, false)


      createTaskDirectories(lessonDir!!, taskFromServer)
      updatedTasks.add(taskFromServer)
    }
  }

  private fun isTheory(currentTask: Task) = currentTask.taskType == TheoryTask().taskType

  private fun upToDateTasks(currentLesson: Lesson,
                            taskIdsToUpdate: List<Int>) =
    currentLesson.taskList.filter { task -> !taskIdsToUpdate.contains(task.stepId) }

  @Throws(IOException::class)
  private fun removeExistingDir(studentTask: Task,
                                lessonDir: VirtualFile?) {
    val taskDir = getTaskDir(studentTask.name, lessonDir)
    invokeAndWaitIfNeed { runWriteAction { taskDir?.delete(studentTask) } }
  }

  @Throws(IOException::class)
  private fun createTaskDirectories(lessonDir: VirtualFile,
                                    task: Task) {
    GeneratorUtils.createTask(task, lessonDir)
    if (!task.lesson.course.isStudy) {
      CCUtils.initializeTaskPlaceholders(task, project)
    }
  }

  private fun getTaskDir(taskName: String, lessonDir: VirtualFile?): VirtualFile? {
    return lessonDir?.findChild(taskName)
  }

  private fun isSolved(studentTask: Task): Boolean {
    return CheckStatus.Solved == studentTask.status
  }

  @Throws(URISyntaxException::class, IOException::class)
  private fun taskIdsToUpdate(lessonFromServer: Lesson,
                              currentLesson: Lesson): List<Int> {
    val taskIds = lessonFromServer.getTaskList().map { task -> task.stepId.toString() }.toTypedArray()
    val tasksById = currentLesson.taskList.associateBy({ it.stepId }, { it })

    return lessonFromServer.taskList
      .zip(taskIds)
      .filter { (newTask, taskId) ->
        val task = tasksById[Integer.parseInt(taskId)]
        task == null || task.updateDate.before(newTask.updateDate)
      }
      .map { (_, taskId) -> Integer.parseInt(taskId) }
  }


  private fun createNewLessons(newLessons: List<Lesson>, parentDir: VirtualFile) {
    for (lesson in newLessons) {
      val lessonDir = lesson.getLessonDir(project)
      if (directoryAlreadyExists(lessonDir)) {
        saveExistingDirectory(lessonDir!!, lesson)
      }

      lesson.init(course, lesson.section, false)
      GeneratorUtils.createLesson(lesson, parentDir)
    }
  }

  private fun createNewSections(project: Project, newSections: List<Section>) {
    for (section in newSections) {
      val baseDir = project.baseDir
      val sectionDir = baseDir.findChild(section.name)
      if (directoryAlreadyExists(sectionDir)) {
        saveExistingDirectory(sectionDir!!, section)
      }

      section.init(course, course, false)
      GeneratorUtils.createSection(section, project.baseDir)
    }
  }

  private fun rename(dirToRename: VirtualFile, s: String) {
    invokeAndWaitIfNeed {
      runWriteAction {
        try {
          dirToRename.rename(this, s)
        }
        catch (e: IOException) {
          LOG.warn(e)
        }
      }
    }
  }

  private fun renamed(currentItem: StudyItem, newItem: StudyItem) = currentItem.name != newItem.name

  private fun directoryAlreadyExists(directory: VirtualFile?): Boolean {
    return directory != null
  }

  private fun constructDir(newItem: StudyItem, currentItem: StudyItem): VirtualFile {
    if (!renamed(currentItem, newItem)) {
      return itemDir(currentItem)!!
    }

    if (directoryAlreadyExists(itemDir(newItem))) {
      saveExistingDirectory(itemDir(newItem)!!, newItem)
    }

    val currentItemDir = getCurrentItemDir(currentItem)
    rename(currentItemDir, newItem.name)

    return currentItemDir
  }

  private fun getCurrentItemDir(item: StudyItem): VirtualFile {
    val dirsMap = if (item is Section) oldSectionDirectories else oldLessonDirectories
    val id = (item as? Section)?.id ?: (item as Lesson).id

    return if (dirsMap.containsKey(id)) {
      val savedDir = dirsMap[id]
      dirsMap.remove(id)
      savedDir!!
    }
    else {
      val oldItemDirectory = itemDir(item)
      oldItemDirectory!!
    }
  }

  private fun itemDir(item: StudyItem): VirtualFile? {
    return if (item is Section) {
      project.baseDir.findChild(item.name)
    }
    else {
      (item as Lesson).getLessonDir(project)
    }
  }

  private fun saveExistingDirectory(itemDir: VirtualFile, item: StudyItem) {
    val directoryMap = if (item is Lesson) oldLessonDirectories else oldSectionDirectories

    val id = (item as? Lesson)?.id ?: (item as? Section)?.id
    invokeAndWaitIfNeed {
      runWriteAction {
        try {
          itemDir.rename(item, "old_${itemDir.name}")
          directoryMap[id!!] = itemDir
        }
        catch (e: IOException) {
          LOG.warn(e.message)
        }
      }
    }
  }

  private fun courseFromServer(project: Project, currentCourse: RemoteCourse): Course? {
    var course: Course? = null
    try {
      val remoteCourse = getCourseFromStepik(EduSettings.getInstance().user, currentCourse.id, true)
      if (remoteCourse != null) {
        course = getCourse(project, remoteCourse)
      }
    }
    catch (e: IOException) {
      LOG.warn(e.message)
    }

    return course
  }

}
