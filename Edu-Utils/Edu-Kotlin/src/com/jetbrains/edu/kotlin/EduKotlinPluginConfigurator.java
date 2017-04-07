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
import com.jetbrains.edu.learning.actions.StudyCheckAction;
import com.jetbrains.edu.learning.actions.StudyFillPlaceholdersAction;
import com.jetbrains.edu.learning.actions.StudyShowHintAction;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.utils.EduIntellijUtils;
import com.jetbrains.edu.utils.EduPluginConfiguratorBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EduKotlinPluginConfigurator extends EduPluginConfiguratorBase {

  static final String LEGACY_TESTS_KT = "tests.kt";
  static final String TESTS_KT = "Tests.kt";

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
  public StudyCheckAction getCheckAction() {
    return new EduKotlinCheckAction();
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
}
