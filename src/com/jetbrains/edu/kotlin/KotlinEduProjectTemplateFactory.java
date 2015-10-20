package com.jetbrains.edu.kotlin;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.platform.ProjectTemplate;
import com.intellij.platform.ProjectTemplatesFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KotlinEduProjectTemplateFactory extends ProjectTemplatesFactory {
    public static final String GROUP_NAME = "Kotlin Education";

    @NotNull
    @Override
    public String[] getGroups() {
        return new String[] {GROUP_NAME};
    }

    @NotNull
    @Override
    public ProjectTemplate[] createTemplates(@Nullable String group, WizardContext context) {
        return new ProjectTemplate[] {new KotlinEduProjectTemplate()};
    }
}
