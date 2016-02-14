package com.jetbrains.edu.kotlin;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.learning.projectView.StudyDirectoryNode;
import org.jetbrains.annotations.NotNull;

public class KotlinDirectoryNode extends StudyDirectoryNode {
    public KotlinDirectoryNode(@NotNull Project project, PsiDirectory value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    @Override
    public void navigate(boolean requestFocus) {
        //TODO: implement
        //super.navigate(requestFocus);
    }
}
