package com.jetbrains.edu.java;

import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.StudyState;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.tasks.*;
import com.jetbrains.edu.utils.EduCheckAction;
import com.jetbrains.edu.utils.EduIntelliJNames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class EduJavaCheckAction extends EduCheckAction {
    @Nullable
    @Override
    protected VirtualFile getTestsFile(@NotNull StudyState studyState) {
        String testFileName = EduJavaPluginConfigurator.TEST_JAVA;
        Task task = studyState.getTask();
        if (task instanceof TaskWithSubtasks) {
            int activeSubtaskIndex = ((TaskWithSubtasks) task).getActiveSubtaskIndex();
            testFileName = FileUtil.getNameWithoutExtension(testFileName) + EduNames.SUBTASK_MARKER + activeSubtaskIndex + "." + FileUtilRt.getExtension(EduJavaPluginConfigurator.TEST_JAVA);
        }
        VirtualFile taskDir = studyState.getTaskDir();
        VirtualFile srcDir = taskDir.findChild(EduNames.SRC);
        if (srcDir != null) {
            taskDir = srcDir;
        }
        VirtualFile virtualFile = taskDir.findChild(testFileName);
        if (virtualFile != null) {
            return virtualFile;
        }
        return null;
    }


    protected void setProcessParameters(Project project, ApplicationConfiguration configuration,
                                        VirtualFile taskFileVF, @NotNull VirtualFile testsFile) {
        configuration.setMainClassName(EduIntelliJNames.TEST_RUNNER_CLASS);
        Module module = ModuleUtilCore.findModuleForFile(taskFileVF, project);
        configuration.setModule(module);
        configuration.setProgramParameters(testsFile.getNameWithoutExtension());
    }

    @NotNull
    @Override
    public String getActionId() {
        return "EduJavaCheckAction";
    }
}
