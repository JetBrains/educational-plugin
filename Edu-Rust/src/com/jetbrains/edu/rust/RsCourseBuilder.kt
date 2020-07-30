package com.jetbrains.edu.rust

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParserFacade
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.io.exists
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.StudyItemType.TASK_TYPE
import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import org.rust.cargo.CargoConstants
import org.rust.cargo.project.model.CargoProject
import org.rust.cargo.project.model.cargoProjects
import org.rust.ide.newProject.RsPackageNameValidator
import org.rust.lang.RsConstants
import org.rust.lang.core.psi.ext.childrenWithLeaves
import org.rust.openapiext.pathAsPath
import org.rust.openapiext.toPsiFile
import org.toml.lang.psi.*
import org.toml.lang.psi.ext.TomlLiteralKind
import org.toml.lang.psi.ext.elementType
import org.toml.lang.psi.ext.kind
import java.nio.file.Path

class RsCourseBuilder : EduCourseBuilder<RsProjectSettings> {

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<RsProjectSettings>? =
    RsCourseProjectGenerator(this, course)

  override fun getLanguageSettings(): LanguageSettings<RsProjectSettings> = RsLanguageSettings()

  override fun refreshProject(project: Project, cause: RefreshCause) {
    val course = StudyTaskManager.getInstance(project).course ?: return
    if (project.isSingleWorkspaceProject) {
      refreshWorkspace(project)
    }
    else {
      refreshTaskPackages(project, course)
    }
  }

  // If there is `Cargo.toml` in root of project, we assume that all task packages are in single workspace.
  // In this case it's enough just to refresh all current cargo projects (actually, single one)
  // or delegate project discovering to IntelliJ Rust plugin when there isn't any cargo project yet
  private fun refreshWorkspace(project: Project) {
    val cargoProjects = project.cargoProjects
    if (!cargoProjects.hasAtLeastOneValidProject) {
      cargoProjects.discoverAndRefresh()
    }
    else {
      cargoProjects.refreshAllProjects()
    }
  }

  private fun refreshTaskPackages(project: Project, course: Course) {
    val cargoProjects = project.cargoProjects
    val cargoProjectMap = HashMap<VirtualFile, CargoProject>()
    val toAttach = mutableListOf<Path>()
    val toDetach = mutableListOf<CargoProject>()
    for (cargoProject in cargoProjects.allProjects) {
      val rootDir = cargoProject.rootDir
      // we should check existence of manifest file because after study item rename
      // manifest path will be outdated
      if (rootDir == null || !cargoProject.manifest.exists()) {
        toDetach += cargoProject
      }
      else {
        cargoProjectMap[rootDir] = cargoProject
      }
    }

    course.visitLessons {
      for (task in it.taskList) {
        val taskDir = task.getDir(project.courseDir) ?: continue
        val cargoProject = cargoProjectMap[taskDir]
        if (cargoProject == null) {
          val manifestFile = taskDir.findChild(CargoConstants.MANIFEST_FILE) ?: continue
          toAttach.add(manifestFile.pathAsPath)
        }
      }
    }

    toDetach.forEach { cargoProjects.detachCargoProject(it) }
    // TODO: find out way not to refresh all projects on each `CargoProjectsService.attachCargoProject` call.
    //  Now it leads to O(n^2) cargo invocations
    toAttach.forEach { cargoProjects.attachCargoProject(it) }
    cargoProjects.refreshAllProjects()
  }

  override fun getTestTaskTemplates(course: Course, info: NewStudyItemInfo, withSources: Boolean): List<TemplateFileInfo> {
    val templates = mutableListOf<TemplateFileInfo>()
    if (withSources) {
      templates += TemplateFileInfo(LIB_RS, "src/$LIB_RS", true)
      templates += TemplateFileInfo(MAIN_RS, "src/$MAIN_RS", true)
      templates += TemplateFileInfo(CargoConstants.MANIFEST_FILE, CargoConstants.MANIFEST_FILE, true)
    }
    templates += TemplateFileInfo(TESTS_RS, "tests/$TESTS_RS", false)
    return templates
  }

  override fun getExecutableTaskTemplates(course: Course, info: NewStudyItemInfo, withSources: Boolean): List<TemplateFileInfo> {
    if (!withSources) return emptyList()
    return listOf(
      TemplateFileInfo(MAIN_RS, "src/$MAIN_RS", true),
      TemplateFileInfo(CargoConstants.MANIFEST_FILE, CargoConstants.MANIFEST_FILE, true)
    )
  }

  override fun extractInitializationParams(project: Project, info: NewStudyItemInfo): Map<String, String> {
    return mapOf("PACKAGE_NAME" to info.name.toPackageName())
  }

  override fun validateItemName(name: String, itemType: StudyItemType): String? =
    if (itemType == TASK_TYPE) RsPackageNameValidator.validate(name.toPackageName(), true) else null

  override fun onStudyItemCreation(project: Project, item: StudyItem) {
    if (item is Task) {
      val lesson = item.lesson
      // Info about new lesson should be inserted into Cargo.toml only after first task creation.
      // Otherwise, Cargo fails to retrieve project metadata because of invalid manifest
      if (lesson.items.size == 1) {
        WriteCommandAction.runWriteCommandAction(project) {
          updateCargoToml(project, lesson)
        }
      }
    }
  }

  override fun beforeStudyItemDeletion(project: Project, item: StudyItem) {
    val membersArray = project.workspaceManifest?.membersArray ?: return

    when (item) {
      is Task -> {
        val lesson = item.lesson
        if (lesson.items.size == 1) {
          removeLesson(project, lesson, membersArray)
        }
      }
      is Lesson -> removeLesson(project, item, membersArray)
      is Section -> {
        for (lesson in item.lessons) {
          removeLesson(project, lesson, membersArray)
        }
      }
    }
  }

  private fun removeLesson(project: Project, lesson: Lesson, membersArray: TomlArray) {
    val path = lesson.courseRelativePath(project) ?: return
    val item = membersArray.findStringLiteralElement { it.startsWith(path) } ?: return

    var lastElementToDelete: PsiElement? = null
    for (element in item.rightSiblings) {
      if (element is TomlValue || element.elementType == TomlElementTypes.R_BRACKET) break
      lastElementToDelete = element
      if (element.isTomlComma) break
    }

    if (lastElementToDelete != null) {
      membersArray.deleteChildRange(item, lastElementToDelete)
    }
    else {
      item.delete()
    }
  }

  private fun updateCargoToml(project: Project, lesson: Lesson) {
    val membersArray = project.workspaceManifest?.membersArray ?: return

    val lessonRelativePath = lesson.courseRelativePath(project) ?: return
    // Similar item already exists
    if (membersArray.findStringLiteralElement { it.startsWith(lessonRelativePath) } != null) return

    val factory = TomlPsiFactory(project)
    val anchor = findAnchor(project, membersArray, lesson) ?: return

    var hasComma = false
    var tomlValue: TomlValue? = null
    for (element in anchor.leftSiblings) {
      if (element is TomlValue) {
        tomlValue = element
        break
      }
      if (element.isTomlComma) {
        hasComma = true
      }
    }

    if (tomlValue != null && !hasComma) {
      membersArray.addAfter(factory.createComma(), tomlValue)
    }

    val createdElement = membersArray.addBefore(factory.createLiteral("\"$lessonRelativePath/*/\""), anchor)
    // If anchor element is in the middle of array, we need add comma after inserted element.
    // Note, trailing comma is allowed in toml (see https://toml.io/en/v1.0.0-rc.1#array)
    val createdComma = membersArray.addAfter(factory.createComma(), createdElement)
    membersArray.addAfter(createNewline(project), createdComma)
  }

  private val Project.workspaceManifest: TomlFile?
    get() = courseDir.findChild(CargoConstants.MANIFEST_FILE)?.toPsiFile(this) as? TomlFile

  private val TomlFile.membersArray: TomlArray?
    get() {
      val workspaceTable = PsiTreeUtil.getChildrenOfTypeAsList(this, TomlTable::class.java)
                             .find { it.header.names.singleOrNull()?.text == "workspace" } ?: return null
      return workspaceTable.entries.find { it.key.text == "members" }?.value as? TomlArray
    }

  private fun Lesson.courseRelativePath(project: Project): String? {
    val lessonDir = getDir(project.courseDir) ?: return null
    return VfsUtil.getRelativePath(lessonDir, project.courseDir)
  }

  private fun findAnchor(project: Project, membersArray: TomlArray, lesson: Lesson): PsiElement? {
    val nextLessonPath = run {
      val nextLesson = NavigationUtils.nextLesson(lesson) ?: return@run null
      val nextLessonDir = nextLesson.getDir(project.courseDir) ?: return@run null
      VfsUtil.getRelativePath(nextLessonDir, project.courseDir)
    }

    var anchor: PsiElement? = null
    if (nextLessonPath != null) {
      anchor = membersArray.findStringLiteralElement { it.startsWith(nextLessonPath) }
    }
    return anchor ?: membersArray.childrenWithLeaves.find { it.elementType == TomlElementTypes.R_BRACKET }
  }

  private fun TomlArray.findStringLiteralElement(filter: (String) -> Boolean): TomlValue? {
    return elements.find {
      val kind = (it as? TomlLiteral)?.kind as? TomlLiteralKind.String ?: return@find false
      kind.value?.let(filter) == true
    }
  }

  private fun createNewline(project: Project): PsiElement =
    PsiParserFacade.SERVICE.getInstance(project).createWhiteSpaceFromText("\n")

  private fun TomlPsiFactory.createComma(): PsiElement {
    return createArray("1, 2").childrenWithLeaves.first { it.isTomlComma }
  }

  private val PsiElement.isTomlComma: Boolean get() = elementType == TomlElementTypes.COMMA

  private val PsiElement.leftSiblings: Sequence<PsiElement>
    get() = generateSequence(prevSibling) { it.prevSibling }

  private val PsiElement.rightSiblings: Sequence<PsiElement>
    get() = generateSequence(nextSibling) { it.nextSibling }

  companion object {
    private const val LIB_RS = RsConstants.LIB_RS_FILE
    private const val MAIN_RS = RsConstants.MAIN_RS_FILE
    private const val TESTS_RS = "tests.rs"
  }
}
