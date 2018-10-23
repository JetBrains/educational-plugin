package com.jetbrains.edu.learning.configuration;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.EmptyIcon;
import com.jetbrains.edu.coursecreator.ui.CCNewCoursePanel;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.checker.TheoryTaskChecker;
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.SystemIndependent;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * The main interface provides courses support for some language and course type.
 *
 * To get configurator instance for some language use {@link EduConfiguratorManager}
 * and {@link EduConfiguratorManager} supports the corresponding filtering.
 *
 * @param <Settings> container type holds course project settings state
 *
 * @see EduConfiguratorManager
 */
public interface EduConfigurator<Settings> {
  String EP_NAME = "Educational.configurator";

  Logger LOG = Logger.getInstance(EduConfigurator.class);

  @NotNull
  EduCourseBuilder<Settings> getCourseBuilder();

  @NotNull
  String getTestFileName();

  /**
   * Used in educator plugin to filter files to be packed into course archive
   */
  default boolean excludeFromArchive(@NotNull Project project, @NotNull String path) {
    return false;
  }

  /**
   * @return true for all the test files
   */
  default boolean isTestFile(@NotNull Project project, @NotNull VirtualFile file) {
    return false;
  }

  /**
   * Provides directory path where task files should be placed in task folder.
   * Can be empty.
   *
   * For example, task folder is `courseName/lesson1/task1` and `getSourceDir` returns `src`
   * then any task files should be placed in `courseName/lesson1/task1/src` folder
   *
   * @return task files directory path
   */
  @SystemIndependent
  @NotNull
  default String getSourceDir() {
    return "";
  }

  /**
   * Provides list of directories where test files should be placed in task folder.
   * Can be empty.
   *
   * See {@link EduConfigurator#getSourceDir()} javadoc for example.
   *
   * @return list of test files directories paths
   */
  @SystemIndependent
  @NotNull
  default List<String> getTestDirs() {
    return Collections.emptyList();
  }

  /**
   * @return class that provide checkers for all types of tasks
   */
  @NotNull TaskCheckerProvider getTaskCheckerProvider();

  /**
   * Allows to determine if configurator can be used in current environment or not.
   *
   * @return true if configurator can be used, false otherwise
   *
   * @see EduConfiguratorManager
   */
  default boolean isEnabled() {
    return true;
  }

  /**
   * Allows to customize file template used as playground in theory and choice tasks
   * Template should work along with the according {@link TheoryTaskChecker}
   *
   * @see StepikTaskBuilder
   *
   */
  default String getMockTemplate() {
    return "";
  }

  /**
   * Provide IDE plugin ids which are required for correct work of courses for the corresponding language.
   *
   * @return list of plugin ids
   */
  default List<String> pluginRequirements() {
    return Collections.emptyList();
  }

  /**
   * Returns icon for decorator language.
   * This icon is used in places where course is associated with language.
   * For example, 'Browse Courses' and 'Create New Course' dialogs.
   *
   * @return 16x16 icon
   *
   * @see CoursesPanel
   * @see CCNewCoursePanel
   */
  @NotNull
  default Icon getLogo() {
    return EmptyIcon.ICON_16;
  }
}
