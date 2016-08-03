package com.jetbrains.edu.kotlin;

import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.edu.learning.StudyState;
import com.jetbrains.edu.utils.EduCheckAction;
import com.jetbrains.edu.utils.EduIntelliJNames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtClass;

import java.util.Collection;

class EduKotlinCheckAction extends EduCheckAction {

    @Nullable
    @Override
    protected VirtualFile getTestsFile(@NotNull StudyState studyState) {
        VirtualFile taskFileVF = studyState.getVirtualFile();
        VirtualFile testsFile = taskFileVF.getParent().findChild("tests.kt");
        if (testsFile != null) {
            return testsFile;
        }
        return taskFileVF.getParent().findChild("Tests.kt");
    }


    protected void setProcessParameters(Project project, ApplicationConfiguration configuration,
                                      VirtualFile taskFileVF, @NotNull VirtualFile testsFile) {
        configuration.setMainClassName(EduIntelliJNames.TEST_RUNNER_CLASS);
        Module module = ModuleUtilCore.findModuleForFile(taskFileVF, project);
        configuration.setModule(module);
        PsiFile psiFile = PsiManager.getInstance(project).findFile(testsFile);
        Collection<KtClass> ktClasses = PsiTreeUtil.findChildrenOfType(psiFile, KtClass.class);
        for (KtClass ktClass : ktClasses) {
            String name = ktClass.getName();
            configuration.setProgramParameters(name);
        }
    }

    @NotNull
    @Override
    public String getActionId() {
        return "EduKotlinCheckAction";
    }
}
