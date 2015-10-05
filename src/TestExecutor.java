//TODO: remove this class


import com.intellij.execution.RunContentExecutor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.run.StudyExecutor;
import com.jetbrains.edu.learning.run.StudyTestRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestExecutor implements StudyExecutor{
    @Nullable
    @Override
    public Sdk findSdk(@NotNull Project project) {
        return null;
    }

    @Override
    public StudyTestRunner getTestRunner(@NotNull Task task, @NotNull VirtualFile taskDir) {
        return null;
    }

    @Override
    public RunContentExecutor getExecutor(@NotNull Project project, @NotNull ProcessHandler handler) {
        return null;
    }

    @Override
    public void setCommandLineParameters(@NotNull GeneralCommandLine cmd, @NotNull Project project, @NotNull String filePath, @NotNull String sdkPath, @NotNull Task currentTask) {

    }

    @Override
    public void showNoSdkNotification(@NotNull Project project) {

    }
}
