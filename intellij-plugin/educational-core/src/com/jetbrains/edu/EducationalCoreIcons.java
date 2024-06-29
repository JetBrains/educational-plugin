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
    public static final Icon AndroidLogo = load("/icons/com/jetbrains/edu/language/logoAndroid.svg");
    public static final Icon CppLogo = load("/icons/com/jetbrains/edu/language/logoCAndC++.svg");
    public static final Icon GoLogo = load("/icons/com/jetbrains/edu/language/logoGo.svg");
    public static final Icon JavaLogo = load("/icons/com/jetbrains/edu/language/logoJava.svg");
    public static final Icon JsLogo = load("/icons/com/jetbrains/edu/language/logoJavaScript.svg");
    public static final Icon KotlinLogo = load("/icons/com/jetbrains/edu/language/logoKotlin.svg");
    public static final Icon PhpLogo = load("/icons/com/jetbrains/edu/language/logoPHP.svg");
    public static final Icon PythonLogo = load("/icons/com/jetbrains/edu/language/logoPython.svg");
    public static final Icon RustLogo = load("/icons/com/jetbrains/edu/language/logoRust.svg");
    public static final Icon ScalaLogo = load("/icons/com/jetbrains/edu/language/logoScala.svg");
    public static final Icon ShellLogo = load("/icons/com/jetbrains/edu/language/logoShell.svg");
  }

  // Platforms
  public static final class Platform {
    public static final Icon CheckiO = load("/icons/com/jetbrains/edu/platform/checkio.svg");
    public static final Icon Codeforces = load("/icons/com/jetbrains/edu/platform/codeforces.svg"); // 24x24
    public static final Icon CODEFORCES_SMALL = load("/icons/com/jetbrains/edu/platform/codeforcesSmall.svg"); // 16x16
    public static final Icon Coursera = load("/icons/com/jetbrains/edu/platform/coursera.svg"); // 24x24
    public static final Icon JB_ACADEMY = load("/icons/com/jetbrains/edu/platform/JB_academy.svg");
    public static final Icon JB_ACADEMY_TAB = load("/icons/com/jetbrains/edu/platform/JB_academy_course_tab.svg"); // 24x24
    public static final Icon JSCheckiO = load("/icons/com/jetbrains/edu/platform/logoCheckiOJS.svg");
    public static final Icon MARKETPLACE_TAB = load("/icons/com/jetbrains/edu/platform/marketplace_courses_tab.svg"); // 24x24
    public static final Icon PyCheckiO = load("/icons/com/jetbrains/edu/platform/logoCheckiOPy.svg");
  }

  public static final Icon LOGGED_IN_USER = load("/icons/com/jetbrains/edu/learning/loggedInUser.svg"); // 16x16
  public static final Icon Task = load("/icons/com/jetbrains/edu/eduTaskDefault.svg"); // 16x16
  public static final Icon TaskSolved = load("/icons/com/jetbrains/edu/eduTaskDone.svg"); // 16x16
  public static final Icon TaskSolvedNoFrame = load("/icons/com/jetbrains/edu/eduTaskDoneNoFrame@2x.png"); //11x11
  public static final Icon TaskSolvedNoFrameHighContrast = load("/icons/com/jetbrains/edu/eduTaskDoneNoFrameHighContrast@2x.png"); //11x11
  public static final Icon TaskFailed = load("/icons/com/jetbrains/edu/eduTaskFailed.svg"); // 16x16
  public static final Icon TaskFailedNoFrame = load("/icons/com/jetbrains/edu/eduTaskFailedNoFrame@2x.png"); // 11x11
  public static final Icon TaskFailedNoFrameHighContrast = load("/icons/com/jetbrains/edu/eduTaskFailedNoFrameHighContrast@2x.png");
  // 11x11
  public static final Icon IdeTask = load("/icons/com/jetbrains/edu/eduTaskIdeDefault.svg"); // 16x16
  public static final Icon IdeTaskSolved = load("/icons/com/jetbrains/edu/eduTaskIdeDone.svg"); // 16x16

  public static final Icon TheoryTask = load("/icons/com/jetbrains/edu/eduTaskTheoryDefault.svg"); // 16x16

  public static final Icon TheoryTaskSolved = load("/icons/com/jetbrains/edu/eduTaskTheoryDone.svg"); // 16x16

  public static final Icon NavigationMapTheoryTask = load("/icons/com/jetbrains/edu/eduNavigationMapTheoryTask.svg"); // 16x16

  public static final Icon Lesson = load("/icons/com/jetbrains/edu/eduLessonDefault.svg"); // 16x16
  public static final Icon LessonSolved = load("/icons/com/jetbrains/edu/eduLessonDone.svg"); // 16x16
  public static final Icon Section = load("/icons/com/jetbrains/edu/eduSectionDefault.svg"); // 16x16
  public static final Icon SectionSolved = load("/icons/com/jetbrains/edu/eduSectionDone.svg"); // 16x16

  public static final Icon CourseAction = load("/icons/com/jetbrains/edu/eduCourseAction.svg"); // 16x16
  public static final Icon CourseTree = load("/icons/com/jetbrains/edu/eduCourseTree.svg"); // 16x16
  public static final Icon CourseToolWindow = load("/icons/com/jetbrains/edu/eduCourseTask.svg"); // 13x13

  public static final Icon ResultCorrect = load("/icons/com/jetbrains/edu/learning/resultCorrect.svg"); // 16x16
  public static final Icon ResultIncorrect = load("/icons/com/jetbrains/edu/learning/resultIncorrect.svg"); // 16x16
  public static final Icon ResetTask = load("/icons/com/jetbrains/edu/learning/resetTask.svg"); // 16x16
  public static final Icon CommentTask = load("/icons/com/jetbrains/edu/learning/commentTask.svg"); // 16x16

  public static final Icon RateCourse = load("/icons/com/jetbrains/edu/learning/rateCourse.svg"); // 16x16
  public static final Icon Clock = load("/icons/com/jetbrains/edu/learning/clock.svg"); // 16x16

  public static final Icon User = load("/icons/com/jetbrains/edu/usersNumber.svg"); // 12x12

  public static final Icon CheckDetailsIcon = load("/icons/com/jetbrains/edu/learning/checkDetailsToolWindow.svg"); // 13x13

  public static final Icon DOT = load("/icons/com/jetbrains/edu/learning/dot.svg"); // 3x3

  public static final Icon MoveUpMatching = load("/icons/com/jetbrains/edu/learning/moveUp.svg"); // 16x16
  public static final Icon MoveDownMatching = load("/icons/com/jetbrains/edu/learning/moveDown.svg"); // 16x16

  public static final Icon ApplyCode = load("/icons/com/jetbrains/edu/learning/applyCode.svg"); // 16x16

  public static final Icon SyncFiles = load("/icons/com/jetbrains/edu/syncFiles.svg"); // 14x14
  public static final Icon SyncFilesModInfo = load("/icons/com/jetbrains/edu/syncFilesModInfo.svg");
  public static final Icon SyncFilesModWarning = load("/icons/com/jetbrains/edu/syncFilesModWarning.svg");

  public static final Icon SyncChangesIgnore = load("/icons/com/jetbrains/edu/syncFilesIgnore.svg"); // 16x16

  public static final Icon LessonCardSimpleLesson = load("/icons/com/jetbrains/edu/lessonCardSimpleLesson.svg"); // 24x24
  public static final Icon LessonCardSimpleLessonSelected = load("/icons/com/jetbrains/edu/lessonCardSimpleLessonSelected.svg"); // 24x24
  public static final Icon LessonCardGuidedProject = load("/icons/com/jetbrains/edu/lessonCardGuidedProject.svg"); // 24x24
  public static final Icon LessonCardGuidedProjectSelected = load("/icons/com/jetbrains/edu/lessonCardGuidedProjectSelected.svg"); // 24x24
}
