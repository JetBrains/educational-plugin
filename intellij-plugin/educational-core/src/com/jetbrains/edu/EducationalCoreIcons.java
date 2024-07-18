package com.jetbrains.edu;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class EducationalCoreIcons {
  /**
   * Loads an icon using the default method.
   * This is currently the default way to load icons based on their path in the `resources` directory.
   * Note that there is also a method available for loading icons as rasterized images
   * using the {@link #loadRasterized(String)} method.
   * <p/>
   * However, the {@link #loadRasterized(String)} method has some limitations because it uses platform API that is not fully adjusted
   * for our needs and may not perfectly fit our use case, but it remains a necessary option for now.
   *
   * @param path the path to the icon in the `resources` directory.
   * @return the loaded {@link Icon} object.
   */
  private static @NotNull Icon load(@NotNull String path) {
    return IconLoader.getIcon(path, EducationalCoreIcons.class);
  }

  /**
   * Loads an icon in a modern and efficient manner as a rasterized image.
   * Currently, this method is used exclusively for loading tool window icons.
   * <p>
   * Note: The last two arguments passed to {@link IconManager#loadRasterizedIcon} are `cacheKey` and `flags`.
   * Both are set to 0, which is acceptable; however, we should consider using a different API in the future
   * that does not apply caching, which is currently unavailable
   *
   * @param path the path to the icon in the resources directory, without a leading slash.
   *             The path must be relative to the classpath root.
   * @return the loaded {@link Icon} object.
   * @throws IllegalArgumentException if the provided path starts with a leading slash.
   */
  private static @NotNull Icon loadRasterized(@NotNull String path) {
    if (path.startsWith("/")) {
      throw new IllegalArgumentException("Path must be specified without a leading slash");
    }

    return IconManager.getInstance().loadRasterizedIcon(path, EducationalCoreIcons.class.getClassLoader(), 0, 0);
  }

  /**
   * Utility class that provides icons for various programming languages
   *
   * <p>All icons are 16x16</p>
   */
  public static final class Language {
    public static final Icon Android = load("/icons/com/jetbrains/edu/learning/AndroidLogo.svg");
    public static final Icon Cpp = load("/icons/com/jetbrains/edu/learning/CAndC++Logo.svg");
    public static final Icon CSharp = load("/icons/com/jetbrains/edu/learning/CSharpLogo.svg");
    public static final Icon Go = load("/icons/com/jetbrains/edu/learning/GoLogo.svg");
    public static final Icon Java = load("/icons/com/jetbrains/edu/learning/JavaLogo.svg");
    public static final Icon JavaScript = load("/icons/com/jetbrains/edu/learning/JavaScriptLogo.svg");
    public static final Icon Kotlin = load("/icons/com/jetbrains/edu/learning/KotlinLogo.svg");
    public static final Icon Php = load("/icons/com/jetbrains/edu/learning/PhpLogo.svg");
    public static final Icon Python = load("/icons/com/jetbrains/edu/learning/PythonLogo.svg");
    public static final Icon Rust = load("/icons/com/jetbrains/edu/learning/RustLogo.svg");
    public static final Icon Scala = load("/icons/com/jetbrains/edu/learning/ScalaLogo.svg");
    public static final Icon Shell = load("/icons/com/jetbrains/edu/learning/ShellLogo.svg");
  }

  /**
   * Utility class that provides icons for various educational platforms
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
      public static final Icon CheckiOTab = load("/icons/com/jetbrains/edu/learning/checkio.svg");
      public static final Icon CourseraTab = load("/icons/com/jetbrains/edu/learning/coursera.svg");
      public static final Icon JetBrainsAcademyTab = load("/icons/com/jetbrains/edu/learning/JB_academy_course_tab.svg");
      public static final Icon MarketplaceTab = load("/icons/com/jetbrains/edu/learning/marketplace_courses_tab.svg");
    }

    public static final Icon Codeforces = load("/icons/com/jetbrains/edu/learning/codeforcesSmall.svg");
    public static final Icon JetBrainsAcademy = load("/icons/com/jetbrains/edu/learning/JB_academy.svg");
    public static final Icon JSCheckiO = load("/icons/com/jetbrains/edu/learning/JSCheckiO.svg");
    public static final Icon PyCheckiO = load("/icons/com/jetbrains/edu/learning/PyCheckiO.svg");
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
  public static final Icon CourseToolWindow = loadRasterized("icons/com/jetbrains/edu/eduCourseTask.svg"); // 13x13

  public static final Icon ResultCorrect = load("/icons/com/jetbrains/edu/learning/resultCorrect.svg"); // 16x16
  public static final Icon ResultIncorrect = load("/icons/com/jetbrains/edu/learning/resultIncorrect.svg"); // 16x16
  public static final Icon ResetTask = load("/icons/com/jetbrains/edu/learning/resetTask.svg"); // 16x16
  public static final Icon CommentTask = load("/icons/com/jetbrains/edu/learning/commentTask.svg"); // 16x16

  public static final Icon RateCourse = load("/icons/com/jetbrains/edu/learning/rateCourse.svg"); // 16x16
  public static final Icon Clock = load("/icons/com/jetbrains/edu/learning/clock.svg"); // 16x16

  public static final Icon User = load("/icons/com/jetbrains/edu/usersNumber.svg"); // 12x12

  public static final Icon CheckDetailsIcon = loadRasterized("icons/com/jetbrains/edu/learning/checkDetailsToolWindow.svg"); // 13x13

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
