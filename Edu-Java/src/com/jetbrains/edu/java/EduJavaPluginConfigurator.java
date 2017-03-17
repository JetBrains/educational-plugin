package com.jetbrains.edu.java;

import com.intellij.ide.IdeView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.jetbrains.edu.coursecreator.settings.CCSettings;
import com.jetbrains.edu.learning.EduPluginConfigurator;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.StudyCheckAction;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.learning.courseFormat.Task;
import com.jetbrains.edu.utils.EduCCCreationUtils;
import com.jetbrains.edu.utils.EduIntelliJNames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class EduJavaPluginConfigurator implements EduPluginConfigurator {
  static final String TEST_JAVA = "Test.java";

  @NotNull
  @Override
  public String getTestFileName() {
    return TEST_JAVA;
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
  public void createTaskContent(@NotNull Project project,
                                @Nullable IdeView view,
                                @NotNull PsiDirectory taskDirectory) {
    StudyUtils.createFromTemplate(project, taskDirectory, "Task.java", view, false);
    StudyUtils.createFromTemplate(project, taskDirectory, TEST_JAVA, view, false);
    String taskDescriptionFileName = StudyUtils.getTaskDescriptionFileName(CCSettings.getInstance().useHtmlAsDefaultTaskFormat());
    StudyUtils.createFromTemplate(project, taskDirectory, taskDescriptionFileName, view, false);
  }

  @Override
  public boolean excludeFromArchive(File pathname) {
    String name = pathname.getName();
    return "out".equals(name) || ".idea".equals(name) || "iml".equals(FileUtilRt.getExtension(name)) || EduIntelliJNames.TEST_RUNNER_FILE.equals(name);
  }

  @Override
  public StudyCheckAction getCheckAction() {
    return new EduJavaCheckAction();
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
  public boolean isTestFile(VirtualFile file) {
    String name = file.getName();
    return TEST_JAVA.equals(name) || name.contains(FileUtil.getNameWithoutExtension(TEST_JAVA)) && name.contains(EduNames.SUBTASK_MARKER);
  }

  @NotNull
  private static String getSubtaskTestsClassName(int index) {
    return index == 0 ? TEST_JAVA : FileUtil.getNameWithoutExtension(TEST_JAVA) + EduNames.SUBTASK_MARKER + index;
  }

  @Override
  public void createTestsForNewSubtask(@NotNull Project project, @NotNull Task task) {
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
    String nextSubtaskTestsClassName = getSubtaskTestsClassName(nextSubtaskIndex);
    JavaDirectoryService.getInstance().createClass(taskPsiDir, nextSubtaskTestsClassName);
  }
}
