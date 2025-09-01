package com.jetbrains.edu.rust

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParserFacade
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.StudyItemType.TASK_TYPE
import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.DefaultSettingsUtils.findPath
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.pathInCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.rust.messages.EduRustBundle
import org.rust.cargo.CargoConstants
import org.rust.cargo.project.model.CargoProject
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.workspace.PackageOrigin
import org.rust.cargo.toolchain.RsLocalToolchain
import org.rust.ide.newProject.RsPackageNameValidator
import org.rust.lang.RsConstants
import org.rust.lang.core.psi.ext.childrenWithLeaves
import org.rust.openapiext.pathAsPath
import org.rust.openapiext.toPsiFile
import org.toml.lang.psi.*
import org.toml.lang.psi.ext.TomlLiteralKind
import org.toml.lang.psi.ext.elementType
import org.toml.lang.psi.ext.kind
import org.toml.lang.psi.ext.name
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

class RsCourseBuilder : EduCourseBuilder<RsProjectSettings> {

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<RsProjectSettings> =
    RsCourseProjectGenerator(this, course)

  override fun getLanguageSettings(): LanguageSettings<RsProjectSettings> = RsLanguageSettings()

  override fun getDefaultSettings(): Result<RsProjectSettings, String> {
    return findPath(DEFAULT_TOOLCHAIN_PROPERTY, "Rust toolchain").flatMap { toolchainPath ->
      val toolchain = RsLocalToolchain(Paths.get(toolchainPath))
      if (!toolchain.looksLikeValidToolchain()) {
        return@flatMap Err("`$toolchainPath` doesn't look like a valid Rust toolchain")
      }
      Ok(RsProjectSettings(toolchain))
    }
  }

  override fun refreshProject(project: Project, cause: RefreshCause) {
    // Don't try to call project model reloading in light tests.
    // It doesn't make sense and may even fail
    @Suppress("TestOnlyProblems")
    if (isUnitTestMode && project.isLight) return
    val course = StudyTaskManager.getInstance(project).course ?: return
    if (!RsCourseProjectRefreshService.getInstance(project).isRefreshEnabled) return
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

  override fun extractInitializationParams(info: NewStudyItemInfo): Map<String, String> {
    return mapOf("PACKAGE_NAME" to info.name.toPackageName())
  }

  override fun validateItemName(project: Project, name: String, itemType: StudyItemType): String? {
    if (itemType != TASK_TYPE) return null
    val packageName = name.toPackageName()
    val nameValidationMessage = RsPackageNameValidator.validate(packageName, true)
    if (nameValidationMessage != null) return nameValidationMessage
    if (!project.isSingleWorkspaceProject) return null

    val isNameAlreadyUsed = project.cargoProjects.allProjects
      .flatMap { it.workspace?.packages.orEmpty() }
      .any { it.origin == PackageOrigin.WORKSPACE && it.name == packageName }

    return if (isNameAlreadyUsed) EduRustBundle.message("error.name.already.used", RsNames.CARGO) else null
  }

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
          membersArray.removeLessonMembers(project, listOf(lesson))
        }
      }
      is Lesson -> membersArray.removeLessonMembers(project, listOf(item))
      is Section -> {
        membersArray.removeLessonMembers(project, item.lessons)
      }
    }
  }

  private fun TomlArray.removeLessonMembers(project: Project, lessons: List<Lesson>) {
    val smartPointerManager = SmartPointerManager.getInstance(project)
    val itemPointers = lessons.mapNotNull { lesson ->
      val path = lesson.courseRelativePath(project) ?: return@mapNotNull null
      val item = findStringLiteralElement { it.startsWith(path) } ?: return@mapNotNull null
      // PSI may be changed between here and next `invokeLater`.
      // To prevent using invalid PSI and storing strong reference to it, we use smart pointer here
      smartPointerManager.createSmartPsiElementPointer(item)
    }

    if (itemPointers.isEmpty()) return

    RsCourseProjectRefreshService.getInstance(project).disableProjectRefresh()

    project.invokeLater {
      WriteCommandAction.runWriteCommandAction(project) {
        try {
          for (pointer in itemPointers) {
            val item = pointer.element ?: return@runWriteCommandAction

            var lastElementToDelete: PsiElement? = null
            for (element in item.rightSiblings) {
              if (element is TomlValue || element.elementType == TomlElementTypes.R_BRACKET) break
              lastElementToDelete = element
              if (element.isTomlComma) break
            }

            if (lastElementToDelete != null) {
              deleteChildRange(item, lastElementToDelete)
            }
            else {
              item.delete()
            }
          }

          saveDocument()
        }
        finally {
          RsCourseProjectRefreshService.getInstance(project).enableProjectRefresh()
        }

        refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
      }
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

    membersArray.saveDocument()
  }

  private val Project.workspaceManifest: TomlFile?
    get() = courseDir.findChild(CargoConstants.MANIFEST_FILE)?.toPsiFile(this) as? TomlFile

  private val TomlFile.membersArray: TomlArray?
    get() {
      val workspaceTable = PsiTreeUtil.getChildrenOfTypeAsList(this, TomlTable::class.java)
                             .find { it.header.key?.name == "workspace" } ?: return null
      return workspaceTable.entries.find { it.key.name == "members" }?.value as? TomlArray
    }

  private fun Lesson.courseRelativePath(project: Project): String? {
    val lessonDir = getDir(project.courseDir) ?: return null
    return lessonDir.pathInCourse(project)
  }

  private fun findAnchor(project: Project, membersArray: TomlArray, lesson: Lesson): PsiElement? {
    val nextLessonPath = run {
      val nextLesson = NavigationUtils.nextLesson(lesson) ?: return@run null
      val nextLessonDir = nextLesson.getDir(project.courseDir) ?: return@run null
      nextLessonDir.pathInCourse(project)
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
    PsiParserFacade.getInstance(project).createWhiteSpaceFromText("\n")

  private fun PsiElement.saveDocument() {
    val document = PsiDocumentManager.getInstance(project).getDocument(containingFile) ?: error("Failed to find document for $containingFile")
    FileDocumentManager.getInstance().saveDocument(document)
  }

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

    private const val DEFAULT_TOOLCHAIN_PROPERTY = "project.rust.toolchain"
  }
}
