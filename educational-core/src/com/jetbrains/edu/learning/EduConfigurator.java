package com.jetbrains.edu.learning;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.actions.*;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.checker.TheoryTaskChecker;
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.SystemIndependent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The main interface provides courses support for some language.
 *
 * To get configurator instance for some language use {@link EduConfiguratorManager}
 * instead of {@link com.intellij.lang.LanguageExtension} because configurator shouldn't be used in some environments
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
  boolean excludeFromArchive(@NotNull String name);

  /**
   * @return true for all the test files
   */
  default boolean isTestFile(VirtualFile file) {
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
   * Provides directory path where test files should be placed in task folder.
   * Can be empty.
   *
   * See {@link EduConfigurator#getSourceDir()} javadoc for example.
   *
   * @return test files directory path
   */
  @SystemIndependent
  @NotNull
  default String getTestDir() {
    return "";
  }

  /**
   * @return class that provide checkers for all types of tasks
   */
  @NotNull TaskCheckerProvider getTaskCheckerProvider();

  @NotNull
  default DefaultActionGroup getTaskDescriptionActionGroup() {
    ActionManager actionManager = ActionManager.getInstance();
    final DefaultActionGroup group = new DefaultActionGroup();

    group.add(actionManager.getAction(CheckAction.ACTION_ID));
    group.addSeparator();

    String[] ids = new String[]{
      PreviousTaskAction.ACTION_ID,
      NextTaskAction.ACTION_ID,
      RefreshTaskFileAction.ACTION_ID,
      ShowHintAction.ACTION_ID,
      CompareWithAnswerAction.ACTION_ID
    };

    Arrays.stream(ids)
      .map(actionManager::getAction)
      .filter(Objects::nonNull)
      .forEach(group::add);

    group.add(new EditInputAction());
    return group;
  }

  default List<String> getBundledCoursePaths() {
    return Collections.emptyList();
  }

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
}
