package com.jetbrains.edu.java;

import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.StudyState;
import com.jetbrains.edu.utils.EduCheckAction;
import com.jetbrains.edu.utils.EduIntelliJNames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

class EduJavaCheckAction extends EduCheckAction {
    @Nullable
    @Override
    protected VirtualFile getTestsFile(@NotNull StudyState studyState) {
        //typically people invoke checker from task files with placeholders
        VirtualFile taskFileVF = studyState.getVirtualFile();
        VirtualFile taskDir = studyState.getTaskDir();
        String testFileName = taskFileVF.getNameWithoutExtension() + "Test" + ".java";
        VirtualFile virtualFile =  taskDir.findChild(testFileName);
        if (virtualFile != null) {
            return virtualFile;
        }
        Set<String> fileNames = studyState.getTask().getTaskFiles().keySet();
        for (String name : fileNames) {
            String testName = FileUtil.getNameWithoutExtension(name) + "Test" + ".java";
            VirtualFile child = taskDir.findChild(testName);
            if (child != null) {
                return child;
            }
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
