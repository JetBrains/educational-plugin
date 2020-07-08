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
import com.jetbrains.edu.coursecreator.TaskType
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
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
        } else {
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
        } else {
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
            } else {
                cargoProjectMap[rootDir] = cargoProject
            }
        }

        course.visitLessons {
            for (task in it.taskList) {
                val taskDir = task.getTaskDir(project) ?: continue
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
            templates +=TemplateFileInfo(MAIN_RS, "src/$MAIN_RS", true)
            templates +=TemplateFileInfo(CargoConstants.MANIFEST_FILE, CargoConstants.MANIFEST_FILE, true)
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
      if (itemType == TaskType) RsPackageNameValidator.validate(name.toPackageName(), true) else null

    override fun createLessonContent(project: Project, lesson: Lesson, parentDirectory: VirtualFile): VirtualFile? {
        val lessonFile = super.createLessonContent(project, lesson, parentDirectory) ?: return null

        WriteCommandAction.runWriteCommandAction(project) {
            updateCargoToml(project, lesson)
        }

        return lessonFile
    }

    private fun updateCargoToml(project: Project, lesson: Lesson) {
        val membersArray = project.workspaceManifest?.membersArray ?: return

        val lessonRelativePath = lesson.courseRelativePath(project) ?: return
        // Similar item already exists
        if (membersArray.findStringLiteralElement { it.startsWith(lessonRelativePath) } != null) return

        val factory = TomlPsiFactory(project)
        var anchor = findAnchor(project, membersArray, lesson) ?: return

        val nextSibling = anchor.nextSibling
        if (anchor is TomlValue) {
            anchor = if (nextSibling?.isTomlComma == true) nextSibling else membersArray.addAfter(factory.createComma(), anchor)
        }

        val createdElement = membersArray.addAfter(factory.createLiteral("\"$lessonRelativePath/*/\""), anchor)
        membersArray.addBefore(createNewline(project), createdElement)
        // If anchor element is in the middle of array, we need add comma after inserted element.
        // Note, trailing comma is allowed in toml (see https://toml.io/en/v1.0.0-rc.1#array)
        membersArray.addAfter(factory.createComma(), createdElement)
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
        val lessonDir = getLessonDir(project) ?: return null
        return VfsUtil.getRelativePath(lessonDir, project.courseDir)
    }

    private fun findAnchor(project: Project, membersArray: TomlArray, lesson: Lesson): PsiElement? {
        val prevLessonPath = run {
            val prevLesson = NavigationUtils.previousLesson(lesson) ?: return@run null
            val prevLessonDir = prevLesson.getLessonDir(project) ?: return null
            VfsUtil.getRelativePath(prevLessonDir, project.courseDir)
        }

        var anchor: PsiElement? = null
        if (prevLessonPath != null) {
            anchor = membersArray.findStringLiteralElement { it.startsWith(prevLessonPath) }
        }
        return anchor ?: membersArray.elements.lastOrNull()
               ?: membersArray.childrenWithLeaves.find { it.elementType == TomlElementTypes.L_BRACKET }
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

    companion object {
        private const val LIB_RS = RsConstants.LIB_RS_FILE
        private const val MAIN_RS = RsConstants.MAIN_RS_FILE
        private const val TESTS_RS = "tests.rs"
    }
}
