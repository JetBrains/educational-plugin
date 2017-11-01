package com.jetbrains.edu.kotlin;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.StudyFillPlaceholdersAction;
import com.jetbrains.edu.learning.checker.StudyTaskChecker;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.core.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.PyCharmTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.intellij.EduIntellijUtils;
import com.jetbrains.edu.learning.intellij.EduPluginConfiguratorBase;
import com.jetbrains.edu.learning.intellij.JdkProjectSettings;
import com.jetbrains.edu.learning.newproject.EduCourseProjectGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinIcons;

import javax.swing.*;
import java.io.File;
import java.util.Collections;
import java.util.List;

public class EduKotlinPluginConfigurator extends EduPluginConfiguratorBase {

  private static final Logger LOG = Logger.getInstance(EduKotlinPluginConfigurator.class);

  static final String LEGACY_TESTS_KT = "tests.kt";
  public static final String TESTS_KT = "Tests.kt";
  public static final String TASK_KT = "Task.kt";

  @NotNull
  @Override
  public String getTestFileName() {
    return TESTS_KT;
  }

  @Override
  public boolean isTestFile(VirtualFile file) {
    String name = file.getName();
    return TESTS_KT.equals(name) || LEGACY_TESTS_KT.equals(name) || name.contains(FileUtil.getNameWithoutExtension(TESTS_KT)) && name.contains(EduNames.SUBTASK_MARKER);
  }

  @NotNull
  @Override
  public StudyTaskChecker<PyCharmTask> getPyCharmTaskChecker(@NotNull PyCharmTask pyCharmTask, @NotNull Project project) {
    return new EduKotlinPyCharmTaskChecker(pyCharmTask, project);
  }

  @NotNull
  @Override
  public DefaultActionGroup getTaskDescriptionActionGroup() {
    DefaultActionGroup taskDescriptionActionGroup = super.getTaskDescriptionActionGroup();
    StudyFillPlaceholdersAction fillPlaceholdersAction = new StudyFillPlaceholdersAction();
    fillPlaceholdersAction.getTemplatePresentation().setIcon(EduKotlinIcons.FILL_PLACEHOLDERS_ICON);
    fillPlaceholdersAction.getTemplatePresentation().setText("Fill Answer Placeholders");
    taskDescriptionActionGroup.add(fillPlaceholdersAction);
    return taskDescriptionActionGroup;
  }

  @Override
  public void configureModule(@NotNull Module module) {
    super.configureModule(module);
    Project project = module.getProject();
    EduKotlinLibConfigurator.configureLib(project);
  }

  @Override
  public VirtualFile createTaskContent(@NotNull Project project, @NotNull Task task,
                                       @NotNull VirtualFile parentDirectory, @NotNull Course course) {
    return EduIntellijUtils.createTask(project, task, parentDirectory, TASK_KT, TESTS_KT);
  }

  @Override
  public List<String> getBundledCoursePaths() {
    File bundledCourseRoot = StudyUtils.getBundledCourseRoot(EduKotlinKoansModuleBuilder.DEFAULT_COURSE_NAME, EduKotlinKoansModuleBuilder.class);
    return Collections.singletonList(FileUtil.join(bundledCourseRoot.getAbsolutePath(), EduKotlinKoansModuleBuilder.DEFAULT_COURSE_NAME));
  }

  @Override
  public EduCourseProjectGenerator<JdkProjectSettings> getEduCourseProjectGenerator(@NotNull Course course) {
    return new EduKotlinCourseProjectGenerator(course);
  }

  @Nullable
  @Override
  public Icon getLogo() {
    return KotlinIcons.SMALL_LOGO;
  }

  @Override
  public boolean isEnabled() {
    return !EduUtils.isAndroidStudio();
  }
}
