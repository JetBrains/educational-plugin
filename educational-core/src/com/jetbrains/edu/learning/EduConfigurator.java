package com.jetbrains.edu.learning;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.actions.*;
import org.jetbrains.annotations.NotNull;

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
   * @return true for all the test files including tests for subtasks
   */
  default boolean isTestFile(VirtualFile file) {
    return false;
  }

  @NotNull
  default DefaultActionGroup getTaskDescriptionActionGroup() {
    final DefaultActionGroup group = new DefaultActionGroup();
    String[] ids = new String[]{
      CheckAction.ACTION_ID,
      PreviousTaskAction.ACTION_ID,
      NextTaskAction.ACTION_ID,
      RefreshTaskFileAction.ACTION_ID,
      ShowHintAction.ACTION_ID,
      CompareWithAnswerAction.ACTION_ID
    };
    ActionManager actionManager = ActionManager.getInstance();
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
}
