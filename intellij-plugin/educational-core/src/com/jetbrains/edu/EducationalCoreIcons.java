package com.jetbrains.edu;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Utility class that provides icons for various educational purposes
 */
public final class EducationalCoreIcons {
  private static @NotNull Icon load(String path) {
    return IconLoader.getIcon(path, EducationalCoreIcons.class);
  }

  /**
   * Utility class that provides icons for various programming languages
   *
   * <p>All icons are 16x16</p>
   */
  public static final class Language {
    public static final Icon Android = load("/icons/com/jetbrains/edu/language/logoAndroid.svg");
    public static final Icon Cpp = load("/icons/com/jetbrains/edu/language/logoCAndC++.svg");
    public static final Icon Go = load("/icons/com/jetbrains/edu/language/logoGo.svg");
    public static final Icon Java = load("/icons/com/jetbrains/edu/language/logoJava.svg");
    public static final Icon Js = load("/icons/com/jetbrains/edu/language/logoJavaScript.svg");
    public static final Icon Kotlin = load("/icons/com/jetbrains/edu/language/logoKotlin.svg");
    public static final Icon Php = load("/icons/com/jetbrains/edu/language/logoPHP.svg");
    public static final Icon Python = load("/icons/com/jetbrains/edu/language/logoPython.svg");
    public static final Icon Rust = load("/icons/com/jetbrains/edu/language/logoRust.svg");
    public static final Icon Scala = load("/icons/com/jetbrains/edu/language/logoScala.svg");
    public static final Icon Shell = load("/icons/com/jetbrains/edu/language/logoShell.svg");
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
      public static final Icon CheckiO = load("/icons/com/jetbrains/edu/platform/tab/logoCheckio@24x24.svg");
      public static final Icon Codeforces = load("/icons/com/jetbrains/edu/platform/tab/logoCodeforces@24x24.svg");
      public static final Icon Coursera = load("/icons/com/jetbrains/edu/platform/tab/logoCoursera@24x24.svg");
      public static final Icon JetBrainsAcademy = load("/icons/com/jetbrains/edu/platform/tab/logoJetBrainsAcademy@24x24.svg");
      public static final Icon Marketplace = load("/icons/com/jetbrains/edu/platform/tab/logoMarketplace@24x24.svg");
    }

    public static final Icon Codeforces = load("/icons/com/jetbrains/edu/platform/logoCodeforces.svg");
    public static final Icon JetBrainsAcademy = load("/icons/com/jetbrains/edu/platform/logoJetBrainsAcademy.svg");
    public static final Icon JSCheckiO = load("/icons/com/jetbrains/edu/platform/logoJSCheckiO.svg");
    public static final Icon PyCheckiO = load("/icons/com/jetbrains/edu/platform/logoPyCheckiO.svg");
  }

  /**
   * Utility class that provides icons for various components in the Course View
   */
  public static final class CourseView {
    public static final Icon CourseTree = load("/icons/com/jetbrains/edu/courseView/eduCourseTree.svg");
    public static final Icon IdeTask = load("/icons/com/jetbrains/edu/courseView/eduTaskIdeDefault.svg");
    public static final Icon IdeTaskSolved = load("/icons/com/jetbrains/edu/courseView/eduTaskIdeDone.svg");
    public static final Icon Lesson = load("/icons/com/jetbrains/edu/courseView/eduLessonDefault.svg");
    public static final Icon LessonSolved = load("/icons/com/jetbrains/edu/courseView/eduLessonDone.svg");
    public static final Icon Section = load("/icons/com/jetbrains/edu/courseView/eduSectionDefault.svg");
    public static final Icon SectionSolved = load("/icons/com/jetbrains/edu/courseView/eduSectionDone.svg");
    public static final Icon SyncFilesModInfo = load("/icons/com/jetbrains/edu/courseView/syncFilesModInfo.svg");
    public static final Icon SyncFilesModWarning = load("/icons/com/jetbrains/edu/courseView/syncFilesModWarning.svg");
    public static final Icon Task = load("/icons/com/jetbrains/edu/courseView/eduTaskDefault.svg");
    public static final Icon TaskFailed = load("/icons/com/jetbrains/edu/courseView/eduTaskFailed.svg");
    public static final Icon TaskSolved = load("/icons/com/jetbrains/edu/courseView/eduTaskDone.svg");
    public static final Icon TheoryTask = load("/icons/com/jetbrains/edu/courseView/eduTaskTheoryDefault.svg");
    public static final Icon TheoryTaskSolved = load("/icons/com/jetbrains/edu/courseView/eduTaskTheoryDone.svg");
    public static final Icon UsersNumber = load("/icons/com/jetbrains/edu/courseView/usersNumber.svg");
  }

  public static final Icon LOGGED_IN_USER = load("/icons/com/jetbrains/edu/learning/loggedInUser.svg"); // 16x16
  public static final Icon TaskSolvedNoFrame = load("/icons/com/jetbrains/edu/eduTaskDoneNoFrame@2x.png"); //11x11
  public static final Icon TaskSolvedNoFrameHighContrast = load("/icons/com/jetbrains/edu/eduTaskDoneNoFrameHighContrast@2x.png"); //11x11
  public static final Icon TaskFailedNoFrame = load("/icons/com/jetbrains/edu/eduTaskFailedNoFrame@2x.png"); // 11x11
  public static final Icon TaskFailedNoFrameHighContrast = load("/icons/com/jetbrains/edu/eduTaskFailedNoFrameHighContrast@2x.png"); // 11x11

  public static final Icon NavigationMapTheoryTask = load("/icons/com/jetbrains/edu/courseView/eduNavigationMapTheoryTask.svg"); // 16x16

  public static final Icon CourseAction = load("/icons/com/jetbrains/edu/eduCourseAction.svg"); // 16x16
  public static final Icon CourseToolWindow = load("/icons/com/jetbrains/edu/eduCourseTask.svg"); // 13x13

  public static final Icon ResultCorrect = load("/icons/com/jetbrains/edu/learning/resultCorrect.svg"); // 16x16
  public static final Icon ResultIncorrect = load("/icons/com/jetbrains/edu/learning/resultIncorrect.svg"); // 16x16
  public static final Icon ResetTask = load("/icons/com/jetbrains/edu/learning/resetTask.svg"); // 16x16
  public static final Icon CommentTask = load("/icons/com/jetbrains/edu/learning/commentTask.svg"); // 16x16

  public static final Icon RateCourse = load("/icons/com/jetbrains/edu/learning/rateCourse.svg"); // 16x16
  public static final Icon Clock = load("/icons/com/jetbrains/edu/learning/clock.svg"); // 16x16

  public static final Icon CheckDetailsIcon = load("/icons/com/jetbrains/edu/learning/checkDetailsToolWindow.svg"); // 13x13

  public static final Icon DOT = load("/icons/com/jetbrains/edu/learning/dot.svg"); // 3x3

  public static final Icon MoveUpMatching = load("/icons/com/jetbrains/edu/learning/moveUp.svg"); // 16x16
  public static final Icon MoveDownMatching = load("/icons/com/jetbrains/edu/learning/moveDown.svg"); // 16x16

  public static final Icon ApplyCode = load("/icons/com/jetbrains/edu/learning/applyCode.svg"); // 16x16

  public static final Icon SyncFiles = load("/icons/com/jetbrains/edu/syncFiles.svg"); // 14x14

  public static final Icon SyncChangesIgnore = load("/icons/com/jetbrains/edu/syncFilesIgnore.svg"); // 16x16

  public static final Icon LessonCardSimpleLesson = load("/icons/com/jetbrains/edu/lessonCardSimpleLesson.svg"); // 24x24
  public static final Icon LessonCardSimpleLessonSelected = load("/icons/com/jetbrains/edu/lessonCardSimpleLessonSelected.svg"); // 24x24
  public static final Icon LessonCardGuidedProject = load("/icons/com/jetbrains/edu/lessonCardGuidedProject.svg"); // 24x24
  public static final Icon LessonCardGuidedProjectSelected = load("/icons/com/jetbrains/edu/lessonCardGuidedProjectSelected.svg"); // 24x24
}
