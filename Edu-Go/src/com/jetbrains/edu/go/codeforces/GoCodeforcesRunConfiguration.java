package com.jetbrains.edu.go.codeforces;

import com.goide.execution.application.GoApplicationConfiguration;
import com.goide.psi.GoFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfiguration;
import com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfigurationType;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.jetbrains.edu.go.checker.GoCodeforcesConfigurationUtilsKt.getGoInputRedirectOptions;
import static com.jetbrains.edu.learning.OpenApiExtKt.getCourseDir;
import static com.jetbrains.edu.learning.VirtualFileExt.getTaskFile;
import static com.jetbrains.edu.learning.codeforces.run.CodeforcesRunConfigurationType.CONFIGURATION_ID;

public class GoCodeforcesRunConfiguration extends GoApplicationConfiguration implements CodeforcesRunConfiguration {
  public GoCodeforcesRunConfiguration(Project project) {
    super(project, CONFIGURATION_ID, CodeforcesRunConfigurationType.getInstance());
  }

  @Override
  public void setExecutableFile(@NotNull VirtualFile file) {
    setKind(Kind.PACKAGE);
    Project project = getProject();
    GoFile goPsiFile = checkRequired((GoFile)PsiManager.getInstance(project).findFile(file),
                                     "Unable to find psiFile for virtual file " + file.getPath());
    String packageName = checkRequired(goPsiFile.getImportPath(false), "Unable to obtain package name for Go file " + file.getPath());
    setPackage(packageName);

    TaskFile taskFile = checkRequired(getTaskFile(file, project), "Unable to find taskFile for virtual file " + file.getPath());
    Task task = taskFile.getTask();
    VirtualFile taskDir = checkRequired(task.getDir(getCourseDir(project)), "Unable to find taskDir for task " + task.getName());
    setWorkingDirectory(taskDir.getPath());
  }

  private static <T> @NotNull T checkRequired(@Nullable T value, @NotNull String error) {
    if (value == null) throw new IllegalStateException(error);
    return value;
  }

  @Override
  public @NotNull InputRedirectOptions getInputRedirectOptions() {
    return getGoInputRedirectOptions(this);
  }
}
