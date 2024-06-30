package com.jetbrains.edu;

import com.jetbrains.edu.learning.ui.EduIcon;
import com.jetbrains.edu.learning.ui.EduIcon.Companion.CustomExpUIMapping;
import com.jetbrains.edu.learning.ui.EduIcon.Companion.NoDarkTheme;
import com.jetbrains.edu.learning.ui.EduIcon.Companion.NoLegacyVersion;

import static com.jetbrains.edu.learning.ui.EduIcon.Companion.IconTarget.BOTH;
import static com.jetbrains.edu.learning.ui.EduIcon.Companion.IconTarget.LEGACY;

/**
 * Utility class that provides icons for various educational purposes
 */
public final class EducationalCoreIcons {
  /**
   * Utility class that provides icons for various programming languages
   *
   * <p>All icons are 16x16</p>
   */
  public static final class Language {
    @NoDarkTheme(LEGACY)
    public static final EduIcon Android = EduIcon.get("/icons/com/jetbrains/edu/language/logoAndroid.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon Cpp = EduIcon.get("/icons/com/jetbrains/edu/language/logoCAndC++.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon Go = EduIcon.get("/icons/com/jetbrains/edu/language/logoGo.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon Java = EduIcon.get("/icons/com/jetbrains/edu/language/logoJava.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon Js = EduIcon.get("/icons/com/jetbrains/edu/language/logoJavaScript.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon Kotlin = EduIcon.get("/icons/com/jetbrains/edu/language/logoKotlin.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon Php = EduIcon.get("/icons/com/jetbrains/edu/language/logoPHP.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon Python = EduIcon.get("/icons/com/jetbrains/edu/language/logoPython.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon Rust = EduIcon.get("/icons/com/jetbrains/edu/language/logoRust.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon Scala = EduIcon.get("/icons/com/jetbrains/edu/language/logoScala.svg");
    public static final EduIcon Shell = EduIcon.get("/icons/com/jetbrains/edu/language/logoShell.svg");
  }

  /**
   * A utility class that provides icons for various educational platforms
   *
   * <p>All child icons are 16x16</p>
   */
  public static final class Platform {
    /**
     * Class that provides tab icons for different platforms
     *
     * <p>Tab icons are 24x24 and are supposed to be used only in implementations of
     * {@code com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory}</p>
     */
    public static final class Tab {
      @NoDarkTheme(BOTH)
      public static final EduIcon CheckiO = EduIcon.get("/icons/com/jetbrains/edu/platform/tab/logoCheckio@24x24.svg");
      @NoDarkTheme(LEGACY)
      public static final EduIcon Codeforces = EduIcon.get("/icons/com/jetbrains/edu/platform/tab/logoCodeforces@24x24.svg");
      public static final EduIcon Coursera = EduIcon.get("/icons/com/jetbrains/edu/platform/tab/logoCoursera@24x24.svg");
      @NoDarkTheme(BOTH)
      public static final EduIcon JetBrainsAcademy = EduIcon.get("/icons/com/jetbrains/edu/platform/tab/logoJetBrainsAcademy@24x24.svg");
      public static final EduIcon Marketplace = EduIcon.get("/icons/com/jetbrains/edu/platform/tab/logoMarketplace@24x24.svg");
    }

    @NoDarkTheme
    public static final EduIcon Codeforces = EduIcon.get("/icons/com/jetbrains/edu/platform/logoCodeforces.svg");
    public static final EduIcon JetBrainsAcademy = EduIcon.get("/icons/com/jetbrains/edu/platform/logoJetBrainsAcademy.svg");
    public static final EduIcon JSCheckiO = EduIcon.get("/icons/com/jetbrains/edu/platform/logoJSCheckiO.svg");
    public static final EduIcon PyCheckiO = EduIcon.get("/icons/com/jetbrains/edu/platform/logoPyCheckiO.svg");
  }

  /**
   * Utility class that provides icons for various components in the Course View
   */
  public static final class CourseView {
    @NoDarkTheme(LEGACY)
    public static final EduIcon CourseTree = EduIcon.get("/icons/com/jetbrains/edu/courseView/eduCourseTree.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon IdeTask = EduIcon.get("/icons/com/jetbrains/edu/courseView/eduTaskIdeDefault.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon IdeTaskSolved = EduIcon.get("/icons/com/jetbrains/edu/courseView/eduTaskIdeDone.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon Lesson = EduIcon.get("/icons/com/jetbrains/edu/courseView/eduLessonDefault.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon LessonSolved = EduIcon.get("/icons/com/jetbrains/edu/courseView/eduLessonDone.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon Section = EduIcon.get("/icons/com/jetbrains/edu/courseView/eduSectionDefault.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon SectionSolved = EduIcon.get("/icons/com/jetbrains/edu/courseView/eduSectionDone.svg");
    @NoLegacyVersion
    public static final EduIcon SyncFilesModInfo = EduIcon.get("/icons/com/jetbrains/edu/courseView/syncFilesModInfo.svg");
    @NoLegacyVersion
    public static final EduIcon SyncFilesModWarning = EduIcon.get("/icons/com/jetbrains/edu/courseView/syncFilesModWarning.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon Task = EduIcon.get("/icons/com/jetbrains/edu/courseView/eduTaskDefault.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon TaskFailed = EduIcon.get("/icons/com/jetbrains/edu/courseView/eduTaskFailed.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon TaskSolved = EduIcon.get("/icons/com/jetbrains/edu/courseView/eduTaskDone.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon TheoryTask = EduIcon.get("/icons/com/jetbrains/edu/courseView/eduTaskTheoryDefault.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon TheoryTaskSolved = EduIcon.get("/icons/com/jetbrains/edu/courseView/eduTaskTheoryDone.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon UsersNumber = EduIcon.get("/icons/com/jetbrains/edu/courseView/usersNumber.svg");
  }

  /**
   * Utility class that provides icons for the task tool window.
   */
  public static final class TaskToolWindow {
    public static final EduIcon Clock = EduIcon.get("/icons/com/jetbrains/edu/taskToolWindow/clock.svg");
    public static final EduIcon CourseToolWindow = EduIcon.get("/icons/com/jetbrains/edu/taskToolWindow/courseToolWindow.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon ExternalLink = EduIcon.get("/icons/com/jetbrains/edu/taskToolWindow/externalLink.svg");
    public static final EduIcon MoveDown = EduIcon.get("/icons/com/jetbrains/edu/taskToolWindow/moveDown.svg");
    public static final EduIcon MoveUp = EduIcon.get("/icons/com/jetbrains/edu/taskToolWindow/moveUp.svg");
    @CustomExpUIMapping(values = "/icons/com/jetbrains/edu/expui/courseView/eduTaskTheoryDefault.svg")
    public static final EduIcon NavigationMapTheoryTask = EduIcon.get("/icons/com/jetbrains/edu/taskToolWindow/navigationMapTheoryTask.svg");
  }

  /**
   * Utility class that provides icons for the check panel.
   */
  public static final class CheckPanel {
    public static final EduIcon CheckDetails = EduIcon.get("/icons/com/jetbrains/edu/checkPanel/checkDetails.svg");
    public static final EduIcon CommentTask = EduIcon.get("/icons/com/jetbrains/edu/checkPanel/commentTask.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon ResetTask = EduIcon.get("/icons/com/jetbrains/edu/checkPanel/resetTask.svg");
    public static final EduIcon ResultCorrect = EduIcon.get("/icons/com/jetbrains/edu/checkPanel/resultCorrect.svg");
    public static final EduIcon ResultIncorrect = EduIcon.get("/icons/com/jetbrains/edu/checkPanel/resultIncorrect.svg");
  }

  /**
   * Utility class that provides icons for the course creator.
   */
  public static final class CourseCreator {
    /**
     * Utility class that provides icons for lesson cards
     */
    public static final class LessonCard {
      @NoLegacyVersion
      public static final EduIcon GuidedProject = EduIcon.get("/icons/com/jetbrains/edu/courseCreator/lessonCard/guidedProject.svg");
      @NoLegacyVersion
      public static final EduIcon GuidedProjectSelected =
        EduIcon.get("/icons/com/jetbrains/edu/courseCreator/lessonCard/guidedProjectSelected.svg");
      @NoLegacyVersion
      public static final EduIcon SimpleLesson = EduIcon.get("/icons/com/jetbrains/edu/courseCreator/lessonCard/simpleLesson.svg");
      @NoLegacyVersion
      public static final EduIcon SimpleLessonSelected =
        EduIcon.get("/icons/com/jetbrains/edu/courseCreator/lessonCard/simpleLessonSelected.svg");
    }

    @NoLegacyVersion
    public static final EduIcon IgnoreSyncFile = EduIcon.get("/icons/com/jetbrains/edu/courseCreator/ignoreSyncFile.svg");
    @NoLegacyVersion
    public static final EduIcon SyncChanges = EduIcon.get("/icons/com/jetbrains/edu/courseCreator/syncChanges.svg");
  }

  /**
   * Utility class that provides icons for various actions
   *
   * <p>All icons are 16x16</p>
   */
  public static final class Actions {
    public static final EduIcon ApplyCode = EduIcon.get("/icons/com/jetbrains/edu/actions/applyCode.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon EduCourse = EduIcon.get("/icons/com/jetbrains/edu/actions/eduCourse.svg");
    @NoDarkTheme(LEGACY)
    public static final EduIcon LoggedInUser = EduIcon.get("/icons/com/jetbrains/edu/actions/loggedInUser.svg");
    public static final EduIcon RateCourse = EduIcon.get("/icons/com/jetbrains/edu/actions/rateCourse.svg");
  }

  @NoLegacyVersion
  @NoDarkTheme
  public static final EduIcon Dot = EduIcon.get("/icons/com/jetbrains/edu/learning/dot.svg"); // 3x3
}
