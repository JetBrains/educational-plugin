package com.jetbrains.edu.kotlin;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.util.PlatformUtils;
import com.jetbrains.edu.learning.actions.StudyFillPlaceholdersAction;
import com.jetbrains.edu.learning.actions.StudyShowHintAction;
import com.jetbrains.edu.learning.checker.StudyTaskChecker;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.PyCharmTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.newproject.EduCourseProjectGenerator;
import com.jetbrains.edu.utils.EduIntellijUtils;
import com.jetbrains.edu.utils.EduPluginConfiguratorBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class EduKotlinPluginConfigurator extends EduPluginConfiguratorBase {

  static final String LEGACY_TESTS_KT = "tests.kt";
  static final String TESTS_KT = "Tests.kt";
  private EduKotlinCourseProjectGenerator myProjectGenerator = new EduKotlinCourseProjectGenerator();

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
    taskDescriptionActionGroup.remove(ActionManager.getInstance().getAction(StudyShowHintAction.ACTION_ID));
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
    StartupManager.getInstance(project).runWhenProjectIsInitialized(() -> EduKotlinLibConfigurator.configureLib(project));
  }

  @Override
  public PsiDirectory createTaskContent(@NotNull Project project, @NotNull Task task, @Nullable IdeView view, @NotNull PsiDirectory parentDirectory, @NotNull Course course) {
    return EduIntellijUtils.createTask(project, task, view, parentDirectory, "Task.kt", TESTS_KT);
  }

  @Override
  public List<String> getBundledCoursePaths() {
    if (!PlatformUtils.isJetBrainsProduct()) {
      return Collections.emptyList();
    }
    File bundledCourseRoot = EduIntellijUtils.getBundledCourseRoot(EduKotlinKoansModuleBuilder.DEFAULT_COURSE_NAME, EduKotlinKoansModuleBuilder.class);
    return Collections.singletonList(FileUtil.join(bundledCourseRoot.getAbsolutePath(), EduKotlinKoansModuleBuilder.DEFAULT_COURSE_NAME));
  }

  @Override
  public EduCourseProjectGenerator getEduCourseProjectGenerator() {
    return myProjectGenerator;
  }

}
