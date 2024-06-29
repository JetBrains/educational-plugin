package com.jetbrains.edu;

import com.jetbrains.edu.learning.ui.EduIcon;

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
    public static final EduIcon Android = new EduIcon("/icons/com/jetbrains/edu/language/logoAndroid.svg");
    public static final EduIcon Cpp = new EduIcon("/icons/com/jetbrains/edu/language/logoCAndC++.svg");
    public static final EduIcon Go = new EduIcon("/icons/com/jetbrains/edu/language/logoGo.svg");
    public static final EduIcon Java = new EduIcon("/icons/com/jetbrains/edu/language/logoJava.svg");
    public static final EduIcon Js = new EduIcon("/icons/com/jetbrains/edu/language/logoJavaScript.svg");
    public static final EduIcon Kotlin = new EduIcon("/icons/com/jetbrains/edu/language/logoKotlin.svg");
    public static final EduIcon Php = new EduIcon("/icons/com/jetbrains/edu/language/logoPHP.svg");
    public static final EduIcon Python = new EduIcon("/icons/com/jetbrains/edu/language/logoPython.svg");
    public static final EduIcon Rust = new EduIcon("/icons/com/jetbrains/edu/language/logoRust.svg");
    public static final EduIcon Scala = new EduIcon("/icons/com/jetbrains/edu/language/logoScala.svg");
    public static final EduIcon Shell = new EduIcon("/icons/com/jetbrains/edu/language/logoShell.svg");
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
      public static final EduIcon CheckiO = new EduIcon("/icons/com/jetbrains/edu/platform/tab/logoCheckio@24x24.svg");
      public static final EduIcon Codeforces = new EduIcon("/icons/com/jetbrains/edu/platform/tab/logoCodeforces@24x24.svg");
      public static final EduIcon Coursera = new EduIcon("/icons/com/jetbrains/edu/platform/tab/logoCoursera@24x24.svg");
      public static final EduIcon JetBrainsAcademy = new EduIcon("/icons/com/jetbrains/edu/platform/tab/logoJetBrainsAcademy@24x24.svg");
      public static final EduIcon Marketplace = new EduIcon("/icons/com/jetbrains/edu/platform/tab/logoMarketplace@24x24.svg");
    }

    public static final EduIcon Codeforces = new EduIcon("/icons/com/jetbrains/edu/platform/logoCodeforces.svg");
    public static final EduIcon JetBrainsAcademy = new EduIcon("/icons/com/jetbrains/edu/platform/logoJetBrainsAcademy.svg");
    public static final EduIcon JSCheckiO = new EduIcon("/icons/com/jetbrains/edu/platform/logoJSCheckiO.svg");
    public static final EduIcon PyCheckiO = new EduIcon("/icons/com/jetbrains/edu/platform/logoPyCheckiO.svg");
  }

  /**
   * Utility class that provides icons for various components in the Course View
   */
  public static final class CourseView {
    public static final EduIcon CourseTree = new EduIcon("/icons/com/jetbrains/edu/courseView/eduCourseTree.svg");
    public static final EduIcon IdeTask = new EduIcon("/icons/com/jetbrains/edu/courseView/eduTaskIdeDefault.svg");
    public static final EduIcon IdeTaskSolved = new EduIcon("/icons/com/jetbrains/edu/courseView/eduTaskIdeDone.svg");
    public static final EduIcon Lesson = new EduIcon("/icons/com/jetbrains/edu/courseView/eduLessonDefault.svg");
    public static final EduIcon LessonSolved = new EduIcon("/icons/com/jetbrains/edu/courseView/eduLessonDone.svg");
    public static final EduIcon Section = new EduIcon("/icons/com/jetbrains/edu/courseView/eduSectionDefault.svg");
    public static final EduIcon SectionSolved = new EduIcon("/icons/com/jetbrains/edu/courseView/eduSectionDone.svg");
    public static final EduIcon SyncFilesModInfo = new EduIcon("/icons/com/jetbrains/edu/courseView/syncFilesModInfo.svg");
    public static final EduIcon SyncFilesModWarning = new EduIcon("/icons/com/jetbrains/edu/courseView/syncFilesModWarning.svg");
    public static final EduIcon Task = new EduIcon("/icons/com/jetbrains/edu/courseView/eduTaskDefault.svg");
    public static final EduIcon TaskFailed = new EduIcon("/icons/com/jetbrains/edu/courseView/eduTaskFailed.svg");
    public static final EduIcon TaskSolved = new EduIcon("/icons/com/jetbrains/edu/courseView/eduTaskDone.svg");
    public static final EduIcon TheoryTask = new EduIcon("/icons/com/jetbrains/edu/courseView/eduTaskTheoryDefault.svg");
    public static final EduIcon TheoryTaskSolved = new EduIcon("/icons/com/jetbrains/edu/courseView/eduTaskTheoryDone.svg");
    public static final EduIcon UsersNumber = new EduIcon("/icons/com/jetbrains/edu/courseView/usersNumber.svg");
  }

  /**
   * Utility class that provides icons for the task tool window.
   */
  public static final class TaskToolWindow {
    public static final EduIcon Clock = new EduIcon("/icons/com/jetbrains/edu/taskToolWindow/clock.svg");
    public static final EduIcon CourseToolWindow = new EduIcon("/icons/com/jetbrains/edu/taskToolWindow/courseToolWindow.svg");
    public static final EduIcon ExternalLink = new EduIcon("/icons/com/jetbrains/edu/taskToolWindow/externalLink.svg");
    public static final EduIcon MoveDown = new EduIcon("/icons/com/jetbrains/edu/taskToolWindow/moveDown.svg");
    public static final EduIcon MoveUp = new EduIcon("/icons/com/jetbrains/edu/taskToolWindow/moveUp.svg");
    public static final EduIcon NavigationMapTheoryTask = new EduIcon("/icons/com/jetbrains/edu/taskToolWindow/navigationMapTheoryTask.svg");
  }

  /**
   * Utility class that provides icons for the check panel.
   */
  public static final class CheckPanel {
    public static final EduIcon CheckDetails = new EduIcon("/icons/com/jetbrains/edu/checkPanel/checkDetails.svg");
    public static final EduIcon CommentTask = new EduIcon("/icons/com/jetbrains/edu/checkPanel/commentTask.svg");
    public static final EduIcon ResetTask = new EduIcon("/icons/com/jetbrains/edu/checkPanel/resetTask.svg");
    public static final EduIcon ResultCorrect = new EduIcon("/icons/com/jetbrains/edu/checkPanel/resultCorrect.svg");
    public static final EduIcon ResultIncorrect = new EduIcon("/icons/com/jetbrains/edu/checkPanel/resultIncorrect.svg");
  }

  /**
   * Utility class that provides icons for the course creator.
   */
  public static final class CourseCreator {
    /**
     * Utility class that provides icons for lesson cards
     */
    public static final class LessonCard {
      public static final EduIcon GuidedProject = new EduIcon("/icons/com/jetbrains/edu/courseCreator/lessonCard/guidedProject.svg");
      public static final EduIcon GuidedProjectSelected = new EduIcon("/icons/com/jetbrains/edu/courseCreator/lessonCard/guidedProjectSelected.svg");
      public static final EduIcon SimpleLesson = new EduIcon("/icons/com/jetbrains/edu/courseCreator/lessonCard/simpleLesson.svg");
      public static final EduIcon SimpleLessonSelected = new EduIcon("/icons/com/jetbrains/edu/courseCreator/lessonCard/simpleLessonSelected.svg");
    }

    public static final EduIcon IgnoreSyncFile = new EduIcon("/icons/com/jetbrains/edu/courseCreator/ignoreSyncFile.svg");
    public static final EduIcon SyncChanges = new EduIcon("/icons/com/jetbrains/edu/courseCreator/syncChanges.svg");
  }

  /**
   * Utility class that provides icons for various actions
   *
   * <p>All icons are 16x16</p>
   */
  public static final class Actions {
    public static final EduIcon ApplyCode = new EduIcon("/icons/com/jetbrains/edu/actions/applyCode.svg");
    public static final EduIcon EduCourse = new EduIcon("/icons/com/jetbrains/edu/actions/eduCourse.svg");
    public static final EduIcon LoggedInUser = new EduIcon("/icons/com/jetbrains/edu/actions/loggedInUser.svg");
    public static final EduIcon RateCourse = new EduIcon("/icons/com/jetbrains/edu/actions/rateCourse.svg");
  }

  public static final EduIcon Dot = new EduIcon("/icons/com/jetbrains/edu/learning/dot.svg"); // 3x3
}
