package com.jetbrains.edu.core;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.platform.ProjectTemplate;
import com.intellij.platform.ProjectTemplatesFactory;
import com.jetbrains.edu.kotlin.EduKotlinProjectTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class EduProjectTemplateFactory extends ProjectTemplatesFactory {
    private static final String GROUP_NAME = "Education";

    @NotNull
    @Override
    public String[] getGroups() {
        return new String[] {GROUP_NAME};
    }

    @NotNull
    @Override
    public ProjectTemplate[] createTemplates(@Nullable String group, WizardContext context) {
        return new ProjectTemplate[] {new EduKotlinProjectTemplate(),
                new EduCustomCourseProjectTemplate()};
    }

    @Override
    public Icon getGroupIcon(String group) {
        return AllIcons.Modules.Types.UserDefined;
    }
}
