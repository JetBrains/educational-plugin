package com.jetbrains.edu.learning.taskToolWindow

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.ui.EduIcon
import com.jetbrains.edu.learning.ui.EduIcon.Companion.CustomExpUIMapping
import com.jetbrains.edu.learning.ui.EduIcon.Companion.IconTarget
import com.jetbrains.edu.learning.ui.EduIcon.Companion.NoDarkTheme
import com.jetbrains.edu.learning.ui.EduIcon.Companion.NoLegacyVersion
import com.jetbrains.edu.learning.ui.EduIcon.Companion.toDarkPath
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.lang.reflect.Field
import java.lang.reflect.Modifier

@Suppress("Junit4RunWithInspection")
@RunWith(Parameterized::class)
class EducationalCoreIconsTest(
  private val aClass: Class<*>,
  private val eduIcon: EduIconTestData,
  @Suppress("unused") private val iconName: String
) : EduTestCase() {
  @Test
  fun `test icons existence `() = eduIcon.doTest(aClass)

  private fun EduIconTestData.doTest(aClass: Class<*>) {
    val paths = buildList {
      add(icon.path)
      if (hasLegacyVersion) add(expuiPath)
      when {
        noDarkTheme == IconTarget.LEGACY && hasLegacyVersion -> add(expuiDarkPath)
        noDarkTheme == IconTarget.NEW_UI -> add(icon.darkPath)
        noDarkTheme == IconTarget.BOTH -> { Unit }
      }
    }
    paths.forEach { path ->
      println("Checking $path")
      val absPath = aClass.getResource(path)
      println("Result: $absPath")
      Assert.assertNotNull("$path doesn't exist in resources folder", absPath)
    }
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{2}")
    fun icons(): Collection<Array<Any>> =
      getIconsFromClasses(
        EducationalCoreIcons::class.java,
        EducationalCoreIcons.Language::class.java,
        EducationalCoreIcons.Platform::class.java,
        EducationalCoreIcons.Platform.Tab::class.java,
        EducationalCoreIcons.CourseView::class.java,
        EducationalCoreIcons.TaskToolWindow::class.java,
        EducationalCoreIcons.CheckPanel::class.java,
        EducationalCoreIcons.CourseCreator::class.java,
        EducationalCoreIcons.CourseCreator.LessonCard::class.java,
        EducationalCoreIcons.Actions::class.java
      ).map { arrayOf(it.first, it.second, it.second.name) }

    private fun getIconsFromClasses(vararg aClasses: Class<*>): Collection<Pair<Class<*>, EduIconTestData>> =
      aClasses.flatMap { aClass ->
        getFields(aClass).map { field ->
          // expecting all fields are EduIcon's
          val icon = field.get(null) as EduIcon
          val noDarkThemeAnnotation = field.getAnnotation(NoDarkTheme::class.java)?.value
          val noLegacyAnnotation = field.getAnnotation(NoLegacyVersion::class.java)
          val customExpUIMapping = field.getAnnotation(CustomExpUIMapping::class.java)?.values
          val testData = EduIconTestData(field.name, icon, noDarkThemeAnnotation, noLegacyAnnotation == null, customExpUIMapping)
          Pair(aClass, testData)
        }
      }

    private fun getFields(aClass: Class<*>): List<Field> =
      aClass.declaredFields
        .filter { field -> Modifier.isStatic(field.modifiers) && Modifier.isFinal(field.modifiers) }
        .map { field -> field.apply { isAccessible = true } }

    data class EduIconTestData(
      val name: String,
      val icon: EduIcon,
      val noDarkTheme: IconTarget? = null,
      val hasLegacyVersion: Boolean = true,
      private val customExpUIMapping: String? = null
    ) {
      val expuiPath: String = customExpUIMapping ?: icon.expuiPath
      val expuiDarkPath: String = customExpUIMapping?.toDarkPath() ?: icon.expuiDarkPath
    }
  }
}