package com.jetbrains.edu.kotlin;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.coursecreator.settings.CCSettings;
import com.jetbrains.edu.learning.EduPluginConfigurator;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.StudyCheckAction;
import com.jetbrains.edu.learning.actions.StudyFillPlaceholdersAction;
import com.jetbrains.edu.learning.actions.StudyShowHintAction;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.utils.EduCCCreationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class EduKotlinPluginConfigurator implements EduPluginConfigurator {

  static final String LEGACY_TESTS_KT = "tests.kt";
  static final String TESTS_KT = "Tests.kt";

  @NotNull
  @Override
  public String getTestFileName() {
    return TESTS_KT;
  }

  @Override
  public PsiDirectory createLesson(@NotNull Project project, @NotNull StudyItem item, @Nullable IdeView view, @NotNull PsiDirectory parentDirectory) {
    return EduCCCreationUtils.createLesson(project, item, parentDirectory);
  }

  @Override
  public PsiDirectory createTask(@NotNull Project project, @NotNull StudyItem item, @Nullable IdeView view, @NotNull PsiDirectory parentDirectory, @NotNull Course course) {
    return EduCCCreationUtils.createTask(project, item, view, parentDirectory, course);
  }

  @Override
  public void createTaskContent(@NotNull Project project, @Nullable IdeView view, @NotNull PsiDirectory taskDirectory) {
    String taskDescriptionFileName = StudyUtils.getTaskDescriptionFileName(CCSettings.getInstance().useHtmlAsDefaultTaskFormat());
    StudyUtils.createFromTemplate(project, taskDirectory, taskDescriptionFileName, view, false);
  }

  @Override
  public boolean excludeFromArchive(File pathname) {
    String name = pathname.getName();
    return "out".equals(name) || ".idea".equals(name);
  }

  @Override
  public boolean isTestFile(VirtualFile file) {
    String name = file.getName();
    return TESTS_KT.equals(name) || LEGACY_TESTS_KT.equals(name) || name.contains(FileUtil.getNameWithoutExtension(TESTS_KT)) && name.contains(EduNames.SUBTASK_MARKER);
  }

  @NotNull
  @Override
  public String getDefaultHighlightingMode() {
    return "text/x-java";
  }

  @NotNull
  @Override
  public String getLanguageScriptUrl() {
    return getClass().getResource("/code_mirror/clike.js").toExternalForm();
  }

  @Override
  public StudyCheckAction getCheckAction() {
    return new EduKotlinCheckAction();
  }

  @NotNull
  @Override
  public DefaultActionGroup getTaskDescriptionActionGroup() {
    DefaultActionGroup taskDescriptionActionGroup = EduPluginConfigurator.super.getTaskDescriptionActionGroup();
    taskDescriptionActionGroup.remove(ActionManager.getInstance().getAction(StudyShowHintAction.ACTION_ID));
    StudyFillPlaceholdersAction fillPlaceholdersAction = new StudyFillPlaceholdersAction();
    fillPlaceholdersAction.getTemplatePresentation().setIcon(EduKotlinIcons.FILL_PLACEHOLDERS_ICON);
    fillPlaceholdersAction.getTemplatePresentation().setText("Fill Answer Placeholders");
    taskDescriptionActionGroup.add(fillPlaceholdersAction);
    return taskDescriptionActionGroup;
  }
}
