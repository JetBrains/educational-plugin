package com.jetbrains.edu.learning.courseFormat

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.UserInfo
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.compatibility.CourseCompatibility.Companion.forCourse
import com.jetbrains.edu.learning.courseFormat.ext.technologyName
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
  var description = ""
  var environment = EduNames.DEFAULT_ENVIRONMENT
  var courseMode = EduNames.STUDY //this field is used to distinguish study and course creator modes
  var solutionsHidden = false

  @Transient
  var visibility: CourseVisibility = CourseVisibility.LocalVisibility

  @Transient
  var additionalFiles = listOf<TaskFile>()
    set(value) {
      field = value.toList()
    }

  @Transient
  var pluginDependencies = emptyList<PluginInfo>()

  open var languageCode = "en"

  // Marketplace:
  var isMarketplace = false
  var vendor: Vendor? = null
  var marketplaceCourseVersion = 0
  var organization: String? = null
  var isMarketplacePrivate = false
  var createDate = Date(0)
  var feedbackLink: String? = null
  var license: String? = null

  /**
   * This method is needed to serialize language and its version as one property
   * Consider using [.getLanguageID] and [.getLanguageVersion] methods instead
   */
  open var language = EduNames.PYTHON // language and optional version in form "Language Version" (as "Python 3.7")

  val languageById: Language?
    get() = Language.findLanguageByID(languageID)

  val languageID: String
    get() = language.split(" ")[0]

  open val languageVersion: String?
    get() {
      if (!language.contains(" ")) {
        return null
      }
      val languageVersionStartIndex = language.indexOf(" ")
      return if (languageVersionStartIndex == language.length - 1) {
        null
      }
      else language.substring(languageVersionStartIndex + 1)
    }

  @Transient
  private val nonEditableFiles = mutableSetOf<String>()

  @Transient
  var authors = listOf<UserInfo>()
    set(value) {
      field = value.toList()
    }

  override fun init(course: Course?, parentItem: StudyItem?, isRestarted: Boolean) {
    for ((i, item) in items.withIndex()) {
      item.index = i + 1
      item.init(this, this, isRestarted)
    }
  }

  fun getLesson(sectionName: String?, lessonName: String): Lesson? {
    if (sectionName == null) return lessons.firstOrNull { lessonName == it.name }

    val section = getSection(sectionName)
    if (section != null) {
      return section.getLesson(lessonName)
    }
    return lessons.firstOrNull { lessonName == it.name }
  }

  val sections: List<Section>
    get() = items.filterIsInstance(Section::class.java)

  fun addSection(section: Section) {
    items.add(section)
  }

  fun removeSection(toRemove: Section) {
    items.remove(toRemove)
  }

  fun getSection(name: String): Section? {
    return getSection { name == it.name }
  }

  fun getSection(isSection: (Section) -> Boolean): Section? {
    return items.filterIsInstance(Section::class.java).firstOrNull { isSection(it) }
  }

  override fun getId(): Int {
    return 0
  }

  override fun getDir(baseDir: VirtualFile): VirtualFile {
    return baseDir
  }

  override fun getCourse(): Course {
    return this
  }

  override fun getParent(): StudyItem {
    return this
  }

  override fun getItemType(): String {
    return EduNames.PYCHARM //"PyCharm" is used here for historical reasons
  }

  open val checkAction: CheckAction
    get() = CheckAction()

  fun copy(): Course {
    return copyAs(javaClass)
  }

  val isStudy: Boolean
    get() = EduNames.STUDY == courseMode

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

  open val tags: List<Tag>
    get() {
      val tags = mutableListOf<Tag>()
      technologyName?.let { tags.add(ProgrammingLanguageTag(it)) }
      tags.add(HumanLanguageTag(humanLanguage))
      return tags
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

  fun addItem(item: StudyItem, index: Int) {
    items.add(index, item)
  }

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
