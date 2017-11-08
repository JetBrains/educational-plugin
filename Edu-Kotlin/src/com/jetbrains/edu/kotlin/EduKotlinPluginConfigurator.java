package com.jetbrains.edu.kotlin;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.StudyFillPlaceholdersAction;
import com.jetbrains.edu.learning.checker.StudyTaskChecker;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.core.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.PyCharmTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.intellij.EduIntellijUtils;
import com.jetbrains.edu.learning.intellij.EduPluginConfiguratorBase;
import com.jetbrains.edu.learning.intellij.JdkProjectSettings;
import com.jetbrains.edu.learning.intellij.generation.EduGradleModuleGenerator;
import com.jetbrains.edu.learning.newproject.EduCourseProjectGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinIcons;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class EduKotlinPluginConfigurator extends EduPluginConfiguratorBase {

  static final String LEGACY_TESTS_KT = "tests.kt";
  static final String TESTS_KT = "Tests.kt";
  static final String KT_EXTENSION = ".kt";
  private static final String TASK_KT = "Task.kt";
  private final Collection<String> NAMES_TO_EXCLUDE = ContainerUtil.newHashSet(
    "gradlew", "gradlew.bat", "local.properties", "gradle.properties", "build.gradle"
    , "settings.gradle", "gradle-wrapper.jar", "gradle-wrapper.properties");

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

  @Override
  public boolean excludeFromArchive(@NotNull String path) {
    boolean excluded = super.excludeFromArchive(path);
    if (!EduUtils.isAndroidStudio() || excluded) {
      return excluded;
    }
    return path.contains("build") || NAMES_TO_EXCLUDE.contains(PathUtil.getFileName(path));

  }

  @NotNull
  @Override
  public StudyTaskChecker<PyCharmTask> getPyCharmTaskChecker(@NotNull PyCharmTask pyCharmTask, @NotNull Project project) {
    return EduUtils.isAndroidStudio() ? new EduKotlinAndroidPyCharmTaskChecker(pyCharmTask, project) : new EduKotlinPyCharmTaskChecker(pyCharmTask, project);
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
    if (EduUtils.isAndroidStudio()) {
      initTask(task);
      ApplicationManager.getApplication().runWriteAction(() -> {
        try {
          EduGradleModuleGenerator.createTaskModule(parentDirectory, task);
        } catch (IOException e) {
          LOG.error("Failed to create task");
        }
      });

      ExternalSystemUtil.refreshProjects(project, new ProjectSystemId("GRADLE"), true, ProgressExecutionMode.MODAL_SYNC);
      return parentDirectory.findChild(EduNames.TASK + task.getIndex());
    }
    return EduIntellijUtils.createTask(project, task, parentDirectory, TASK_KT, TESTS_KT);
  }

  @Override
  public void createTestsForNewSubtask(@NotNull Project project, @NotNull TaskWithSubtasks task) {
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      return;
    }
    int prevSubtaskIndex = task.getLastSubtaskIndex();
    PsiDirectory taskPsiDir = PsiManager.getInstance(project).findDirectory(taskDir);
    if (taskPsiDir == null) {
      return;
    }
    int nextSubtaskIndex = prevSubtaskIndex + 1;
    String nextSubtaskFileName = getSubtaskFileName(TESTS_KT, nextSubtaskIndex);

    ApplicationManager.getApplication().runWriteAction(() -> {
      try {
        FileTemplate testsTemplate = FileTemplateManager.getInstance(project).getInternalTemplate(TESTS_KT);
        if (testsTemplate == null) {
          return;
        }
        Properties properties = new Properties();
        properties.setProperty("TEST_CLASS_NAME", "Test" + EduNames.SUBTASK_MARKER + nextSubtaskIndex);
        FileTemplateUtil.createFromTemplate(testsTemplate, nextSubtaskFileName, properties, taskPsiDir);
      }
      catch (Exception e) {
        LOG.error(e);
      }
    });
  }

  @NotNull
  public static String getSubtaskFileName(@NotNull String original, int subTaskIndex) {
    return FileUtil.getNameWithoutExtension(original) + EduNames.SUBTASK_MARKER + subTaskIndex + EduKotlinPluginConfigurator.KT_EXTENSION;
  }

  @Nullable
  @Override
  public Properties getFileTemplateProperties() {
    Properties properties = new Properties();
    properties.setProperty("TEST_CLASS_NAME", "Test");
    return properties;
  }

  @Override
  public List<String> getBundledCoursePaths() {
    File bundledCourseRoot = StudyUtils.getBundledCourseRoot(EduKotlinKoansModuleBuilder.DEFAULT_COURSE_NAME, EduKotlinKoansModuleBuilder.class);
    return Collections.singletonList(FileUtil.join(bundledCourseRoot.getAbsolutePath(), EduKotlinKoansModuleBuilder.DEFAULT_COURSE_NAME));
  }

  @Override
  public EduCourseProjectGenerator<JdkProjectSettings> getEduCourseProjectGenerator(@NotNull Course course) {
    return EduUtils.isAndroidStudio() ? new KoansAndroidProjectGenerator(course)
            : new EduKotlinCourseProjectGenerator(course);
  }

  @Nullable
  @Override
  public Icon getLogo() {
    return KotlinIcons.SMALL_LOGO;
  }

  static void initTask(@NotNull Task task) {
    TaskFile taskFile = new TaskFile();
    taskFile.setTask(task);
    taskFile.name  = TASK_KT;
    taskFile.text = EduUtils.getTextFromInternalTemplate(TASK_KT);
    task.addTaskFile(taskFile);
    task.getTestsText().put(TESTS_KT, EduUtils.getTextFromInternalTemplate(TESTS_KT));
  }
}
