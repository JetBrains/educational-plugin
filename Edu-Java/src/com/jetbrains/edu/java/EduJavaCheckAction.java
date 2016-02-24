package com.jetbrains.edu.java;

import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.StudyState;
import com.jetbrains.edu.utils.EduCheckAction;
import com.jetbrains.edu.utils.EduIntelliJNames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class EduJavaCheckAction extends EduCheckAction {
    @Nullable
    @Override
    protected VirtualFile getTestsFile(@NotNull StudyState studyState) {
        VirtualFile taskFileVF = studyState.getVirtualFile();
        String testFileName = taskFileVF.getNameWithoutExtension() + "Test" + ".java";
        return taskFileVF.getParent().findChild(testFileName);
    }


    protected void setProcessParameters(Project project, ApplicationConfiguration configuration,
                                      VirtualFile taskFileVF, @NotNull VirtualFile testsFile) {
        configuration.setMainClassName(EduIntelliJNames.TEST_RUNNER_CLASS);
        Module module = ModuleUtilCore.findModuleForFile(taskFileVF, project);
        configuration.setModule(module);
        configuration.setProgramParameters(testsFile.getNameWithoutExtension());
    }
}
