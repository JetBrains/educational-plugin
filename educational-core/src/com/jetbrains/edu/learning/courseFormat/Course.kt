package com.jetbrains.edu.learning.courseFormat

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduLanguage
import com.jetbrains.edu.learning.EduLanguage.Companion.get
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.UserInfo
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.compatibility.CourseCompatibility.Companion.forCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.plugins.PluginInfo
import java.util.*
import javax.swing.Icon

/**
 * To introduce new course it's required to:
 * - Extend Course class
 * - Update CourseBuilder#build() in [com.jetbrains.edu.learning.yaml.format.CourseYamlUtil] to handle course loading from YAML
 * - Override [Course.getItemType], that's how we find appropriate [com.jetbrains.edu.learning.configuration.EduConfigurator]
 */
abstract class Course : LessonContainer() {
  var description: String = ""
  var environment: String = DEFAULT_ENVIRONMENT
  var courseMode: CourseMode = CourseMode.STUDENT //this field is used to distinguish study and course creator modes
  var solutionsHidden: Boolean = false

  @Transient
  var visibility: CourseVisibility = CourseVisibility.LocalVisibility

  @Transient
  var additionalFiles: List<TaskFile> = emptyList()

  @Transient
  var pluginDependencies: List<PluginInfo> = emptyList()

  @Transient
  private val nonEditableFiles: MutableSet<String> = mutableSetOf()

  @Transient
  var authors: List<UserInfo> = emptyList()

  open var languageCode: String = "en"

  // Marketplace:
  var isMarketplace: Boolean = false
  var vendor: Vendor? = null
  var marketplaceCourseVersion: Int = 0
  var organization: String? = null
  var isMarketplacePrivate: Boolean = false
  var createDate: Date = Date(0)
  var feedbackLink: String? = null
  var license: String? = null

  /**
   * This method is needed to serialize language and its version as one property
   * Consider using [languageID] and [languageVersion] properties instead
   */
  open var programmingLanguage: String = "" // language and optional version in form "Language Version" (as "Python 3.7")

  fun init(isRestarted: Boolean) {
    init(this, isRestarted)
  }

  override fun init(parentItem: ItemContainer, isRestarted: Boolean) {
    require(parentItem is Course)
    super.init(parentItem, isRestarted)
  }

  val languageById: Language?
    get() = getEduLanguage().language

  val languageID: String
    get() = getEduLanguage().id

  open val languageVersion: String?
    get() = getEduLanguage().version.ifEmpty { null }

  private fun getEduLanguage(): EduLanguage {
    return get(programmingLanguage)
  }

  fun getLesson(sectionName: String?, lessonName: String): Lesson? {
    if (sectionName != null) {
      val section = getSection(sectionName)
      if (section != null) {
        return section.getLesson(lessonName)
      }
    }
    return lessons.firstOrNull { lessonName == it.name }
  }

  val sections: List<Section>
    get() = items.filterIsInstance<Section>()

  fun addSection(section: Section) {
    addItem(section)
  }

  fun removeSection(toRemove: Section) {
    removeItem(toRemove)
  }

  fun getSection(name: String): Section? {
    return getSection { name == it.name }
  }

  fun getSection(predicate: (Section) -> Boolean): Section? {
    return sections.firstOrNull { predicate(it) }
  }

  override fun getDir(baseDir: VirtualFile): VirtualFile? {
    return baseDir
  }

  override val course: Course
    get() = this

  override val itemType: String = EduNames.PYCHARM //"PyCharm" is used here for historical reasons

  open val checkAction: CheckAction
    get() = CheckAction()

  val isStudy: Boolean
    get() = CourseMode.STUDENT == courseMode

  override fun sortItems() {
    super.sortItems()
    sections.forEach { it.sortItems() }
  }

  override fun toString(): String {
    return name
  }

  val authorFullNames: List<String>
    get() {
      return organization?.let { listOf(it) } ?: authors.map { it.getFullName() }
    }

  open val humanLanguage: String
    get() = Locale(languageCode).displayName

  open val compatibility: CourseCompatibility
    get() = forCourse(this)

  open val icon: Icon
    get() = EducationalCoreIcons.CourseTree

  open val isViewAsEducatorEnabled: Boolean
    get() = ApplicationManager.getApplication().isInternal

  open val isStepikRemote: Boolean
    get() = false

  fun incrementMarketplaceCourseVersion(remoteCourseVersion: Int) {
    marketplaceCourseVersion = remoteCourseVersion + 1
  }

  fun isEditableFile(path: String): Boolean {
    return !nonEditableFiles.contains(path)
  }

  fun addNonEditableFile(path: String?) {
    if (path != null && isStudy) {
      nonEditableFiles.add(path)
    }
  }
}
