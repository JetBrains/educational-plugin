package com.jetbrains.edu.python.learning;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import com.jetbrains.edu.python.learning.newproject.PyDirectoryProjectGenerator;
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings;
import com.jetbrains.python.newProject.PyNewProjectSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.jetbrains.edu.python.learning.PyConfigurator.TASK_PY;
import static com.jetbrains.edu.python.learning.PyConfigurator.TESTS_PY;

public class PyCourseBuilder implements EduCourseBuilder<PyNewProjectSettings> {

  private static final Logger LOG = Logger.getInstance(PyCourseBuilder.class);

  @Nullable
  @Override
  public String getTaskTemplateName() {
    return TASK_PY;
  }

  @Nullable
  @Override
  public String getTestTemplateName() {
    return TESTS_PY;
  }

  @Override
  public void createTestsForNewSubtask(@NotNull Project project, @NotNull TaskWithSubtasks task) {
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) return;
    VirtualFile testDir = TaskExt.findTestDir(task, taskDir);
    if (testDir == null) return;

    int nextSubtaskIndex = task.getLastSubtaskIndex() + 1;
    String nextSubtaskTestsFileName = getSubtaskTestsFileName(nextSubtaskIndex);
    ApplicationManager.getApplication().runWriteAction(() -> {
      try {
        PsiDirectory testPsiDir = PsiManager.getInstance(project).findDirectory(testDir);
        FileTemplate testsTemplate = FileTemplateManager.getInstance(project).getInternalTemplate(TESTS_PY);
        if (testPsiDir == null || testsTemplate == null) {
          return;
        }
        FileTemplateUtil.createFromTemplate(testsTemplate, nextSubtaskTestsFileName, null, testPsiDir);
      }
      catch (Exception e) {
        LOG.error(e);
      }
    });
  }

  @NotNull
  public static String getSubtaskTestsFileName(int index) {
    return index == 0 ? TESTS_PY : FileUtil.getNameWithoutExtension(TESTS_PY) +
            EduNames.SUBTASK_MARKER +
            index + "." +
            FileUtilRt.getExtension(TESTS_PY);
  }

  @NotNull
  @Override
  public LanguageSettings<PyNewProjectSettings> getLanguageSettings() {
    return new PyLanguageSettings();
  }

  @Nullable
  @Override
  public CourseProjectGenerator<PyNewProjectSettings> getCourseProjectGenerator(@NotNull Course course) {
    return new PyDirectoryProjectGenerator(this, course);
  }
}
