package com.jetbrains.edu.learning.stepik

import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.Lists
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCStudyItemDeleteProvider
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduUtils.showNotification
import com.jetbrains.edu.learning.EduUtils.synchronize
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader.loadCourseStructure
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.io.IOException
import java.net.URISyntaxException
import java.util.*

class StepikCourseUpdater(val course: EduCourse, val project: Project) {
  private val LOG = Logger.getInstance(this.javaClass)

  private val oldLessonDirectories = HashMap<Int, VirtualFile>()
  private val oldSectionDirectories = HashMap<Int, VirtualFile>()

  fun updateCourse() {
    oldLessonDirectories.clear()
    oldSectionDirectories.clear()

    val courseFromServer = courseFromServer(course)
    if (courseFromServer == null) {
      LOG.warn("Course ${course.id} not found on Stepik")
      return
    }

    doUpdate(courseFromServer)

    runInEdt {
      synchronize()
      ProjectView.getInstance(project).refresh()
      showNotification(project, "Course updated", null)
      course.configurator?.courseBuilder?.refreshProject(project)
      YamlFormatSynchronizer.saveAll(project)
    }
  }

  @VisibleForTesting
  fun doUpdate(courseFromServer: EduCourse) {
    courseFromServer.items.withIndex().forEach { (index, item) -> item.index = index + 1 }

    setCourseInfo(courseFromServer)
    updateSections(courseFromServer)
    updateLessons(courseFromServer)
    updateAdditionalMaterialsFiles(courseFromServer)

    course.items = Lists.newArrayList(courseFromServer.items)
    course.setUpdated(courseFromServer)
  }

  private fun updateLessons(courseFromServer: Course) {
    processNewLessons(courseFromServer)
    processDeletedLessons(courseFromServer)
    processModifiedLessons(courseFromServer.lessons.filter { course.getLesson(it.id) != null }, course)
  }

  private fun processNewLessons(courseFromServer: Course) {
    val newLessons = courseFromServer.lessons.filter { course.getLesson(it.id) == null }
    if (newLessons.isNotEmpty()) {
      createNewLessons(newLessons, project.courseDir)
    }
  }

  private fun processDeletedLessons(courseFromServer: Course) {
    val lessonsFromServerIds = mutableListOf<Int>()
    courseFromServer.visitLessons { lessonsFromServerIds.add(it.id) }
    val courseLessons = mutableListOf<Lesson>()
    course.visitLessons { courseLessons.add(it) }

    val lessonsToDelete = courseLessons.filter { it.id !in lessonsFromServerIds }
    if (lessonsToDelete.isNotEmpty()) {
      runInEdt {
        runWriteAction {
          for (lesson in lessonsToDelete) {
            val virtualFile = lesson.getDir(project)
            CommandProcessor.getInstance().executeCommand(project, {
              virtualFile?.delete(CCStudyItemDeleteProvider::class.java)
            }, "", this.javaClass)
          }
        }
      }
    }
  }

  @Throws(URISyntaxException::class, IOException::class)
  private fun processModifiedLessons(lessonsFromServer: List<Lesson>, parent: LessonContainer): Int {
    var lessonsUpdated = 0
    for (lessonFromServer in lessonsFromServer) {

      lessonFromServer.taskList.withIndex().forEach { (index, task) -> task.index = index + 1 }
      val currentLesson = parent.getLesson(lessonFromServer.id)

      val taskIdsToUpdate = taskIdsToUpdate(lessonFromServer, currentLesson!!)
      val tasksToDelete = tasksToDelete(lessonFromServer, currentLesson)

      if (tasksToDelete.isNotEmpty()) {
        runInEdt {
          runWriteAction {
            tasksToDelete.forEach {
              val virtualFile = it.getTaskDir(project)
              CommandProcessor.getInstance().executeCommand(project, {
                virtualFile?.delete(CCStudyItemDeleteProvider::class.java)
              }, "", this.javaClass)
            }
          }
        }
      }

      if (lessonContentChanged(taskIdsToUpdate)) {
        lessonsUpdated++
        val lessonDir = constructDir(lessonFromServer, currentLesson)
        updateTasks(taskIdsToUpdate, lessonFromServer, currentLesson, lessonDir)
      }

      if (!lessonContentChanged(taskIdsToUpdate) && renamed(lessonFromServer, currentLesson)) {
        constructDir(lessonFromServer, currentLesson)
      }

      lessonFromServer.init(course, lessonFromServer.section, false)
    }
    return lessonsUpdated
  }

  private fun updateSections(courseFromServer: EduCourse) {
    val sectionIds = course.sections.map { it.id }

    processNewSections(courseFromServer, sectionIds)
    processDeletedSections(courseFromServer)
    processModifiedSections(courseFromServer, sectionIds)
  }

  private fun processNewSections(courseFromServer: Course,
                                 sectionIds: List<Int>): List<Section> {
    val newSections = courseFromServer.sections.filter { section -> section.id !in sectionIds }

    if (newSections.isNotEmpty()) {
      createNewSections(project, newSections)
    }
    return newSections
  }

  private fun processDeletedSections(courseFromServer: EduCourse) {
    val sectionsFromServerIds = courseFromServer.sections.map { it.id }
    val sectionsToDelete = course.sections.filter { it.id !in sectionsFromServerIds }
    if (sectionsToDelete.isNotEmpty()) {
      runInEdt {
        runWriteAction {
          for (section in sectionsToDelete) {
            val virtualFile = section.getDir(project)
            CommandProcessor.getInstance().executeCommand(project, {
              virtualFile?.delete(CCStudyItemDeleteProvider::class.java)
            }, "", this.javaClass)
          }
        }
      }
    }
  }

  @Throws(URISyntaxException::class, IOException::class)
  private fun processModifiedSections(courseFromServer: EduCourse, sectionIds: List<Int>) {
    val sectionsFromServer = courseFromServer.sections.filter { section -> section.id in sectionIds }
    val sectionsById = course.sections.associateBy { it.id }
    for (sectionFromServer in sectionsFromServer) {
      sectionFromServer.lessons.withIndex().forEach { (index, lesson) -> lesson.index = index + 1 }

      if (course.lessons.isNotEmpty()) {
        val isTopLevelLessonsSection = sectionFromServer.id == course.sectionIds[0]
        if (isTopLevelLessonsSection) {
          return
        }
      }

      val currentSection = sectionsById[sectionFromServer.id]
      val currentLessons = currentSection!!.lessons.map { it.id }

      val newLessons = sectionFromServer.lessons.filter { it.id !in currentLessons }
      val sectionContentChanged = newLessons.isNotEmpty()
      if (sectionContentChanged) {
        val currentSectionDir = constructDir(sectionFromServer, currentSection)
        createNewLessons(newLessons, currentSectionDir)
      }
      if (!sectionContentChanged && renamed(currentSection, sectionFromServer)) {
        constructDir(sectionFromServer, currentSection)
      }

      val lessonsToUpdate = sectionFromServer.lessons.filter { it.id in currentLessons }
      processModifiedLessons(lessonsToUpdate, currentSection)
      sectionFromServer.init(course, course, false)
    }
  }

  private fun updateAdditionalMaterialsFiles(courseFromServer: Course) {
    val filesToCreate = courseFromServer.additionalFiles
    val baseDir = project.courseDir
    for (file in filesToCreate) {
      GeneratorUtils.createChildFile(baseDir, file.name, file.text)
    }
  }

  private fun setCourseInfo(courseFromServer: Course) {
    course.name = courseFromServer.name
    course.description = courseFromServer.description
  }

  private fun lessonContentChanged(taskIdsToUpdate: List<Int>) = taskIdsToUpdate.isNotEmpty()

  private fun updateTasks(taskIdsToUpdate: List<Int>,
                          lessonFromServer: Lesson,
                          currentLesson: Lesson,
                          lessonDir: VirtualFile?) {
    val serverTasksById = lessonFromServer.taskList.associateBy({ it.id }, { it })
    val tasksById = currentLesson.taskList.associateBy { it.id }
    for (taskId in taskIdsToUpdate) {
      val taskFromServer = serverTasksById[taskId]
      val taskIndex = taskFromServer!!.index
      if (tasksById.containsKey(taskId)) {
        val currentTask = tasksById[taskId]
        if ((isSolved(currentTask!!) && !isTheory(currentTask)) && course.isStudy) {
          currentTask.index = taskIndex
          currentTask.descriptionText = taskFromServer.descriptionText
          continue
        }
        removeExistingDir(currentTask, lessonDir)
      }

      taskFromServer.init(course, currentLesson, false)

      createTaskDirectories(lessonDir!!, taskFromServer)
    }
  }

  private fun isTheory(currentTask: Task) = currentTask.itemType == TheoryTask().itemType

  @Throws(IOException::class)
  private fun removeExistingDir(studentTask: Task,
                                lessonDir: VirtualFile?) {
    val taskDir = getTaskDir(studentTask.name, lessonDir)
    invokeAndWaitIfNeeded { runWriteAction { taskDir?.delete(studentTask) } }
  }

  @Throws(IOException::class)
  private fun createTaskDirectories(lessonDir: VirtualFile, task: Task) {
    if (!task.lesson.course.isStudy) {
      CCUtils.initializeTaskPlaceholders(task, project)
    }
    GeneratorUtils.createTask(task, lessonDir)
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
    val taskIds = lessonFromServer.taskList.map { task -> task.id.toString() }.toTypedArray()
    val tasksById = currentLesson.taskList.associateBy({ it.id }, { it })

    return lessonFromServer.taskList
      .zip(taskIds)
      .filter { (newTask, taskId) ->
        val task = tasksById[Integer.parseInt(taskId)]
        task == null || task.updateDate.before(newTask.updateDate)
      }
      .map { (_, taskId) -> Integer.parseInt(taskId) }
  }

  private fun tasksToDelete(lessonFromServer: Lesson, currentLesson: Lesson): List<Task> {
    val tasksFromServerIds = lessonFromServer.taskList.map { task -> task.id }
    return currentLesson.taskList.filter { it.id !in tasksFromServerIds }
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
    val baseDir = project.courseDir
    for (section in newSections) {
      val sectionDir = baseDir.findChild(section.name)
      if (directoryAlreadyExists(sectionDir)) {
        saveExistingDirectory(sectionDir!!, section)
      }

      section.init(course, course, false)
      GeneratorUtils.createSection(section, baseDir)
    }
  }

  private fun rename(dirToRename: VirtualFile, s: String) {
    invokeAndWaitIfNeeded {
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
      project.courseDir.findChild(item.name)
    }
    else {
      (item as Lesson).getLessonDir(project)
    }
  }

  private fun saveExistingDirectory(itemDir: VirtualFile, item: StudyItem) {
    val directoryMap = if (item is Lesson) oldLessonDirectories else oldSectionDirectories

    val id = (item as? Lesson)?.id ?: (item as? Section)?.id
    invokeAndWaitIfNeeded {
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

  private fun courseFromServer(currentCourse: EduCourse): EduCourse? {
    val remoteCourse = StepikConnector.getInstance().getCourseInfo(currentCourse.id, true)
    if (remoteCourse != null) {
      loadCourseStructure(remoteCourse)
      addTopLevelLessons(remoteCourse)
      return remoteCourse
    }
    return null
  }

  // On Stepik top-level lessons section is named after a course
  // In case it was renamed on stepik, its lessons  won't be parsed as top-level
  // so we need to copy them manually
  private fun addTopLevelLessons(courseFromServer: Course?) {
    if (courseFromServer!!.sections.isNotEmpty() && course.sectionIds.isNotEmpty()) {
      if (courseFromServer.sections[0].id == course.sectionIds[0]) {
        courseFromServer.addLessons(courseFromServer.sections[0].lessons)
      }
    }
  }
}
