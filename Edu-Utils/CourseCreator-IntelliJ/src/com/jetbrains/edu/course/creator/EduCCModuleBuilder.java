package com.jetbrains.edu.course.creator;

import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.coursecreator.CCProjectService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class EduCCModuleBuilder extends JavaModuleBuilder {
    @Nullable
    @Override
    public Module commitModule(@NotNull Project project, @Nullable ModifiableModuleModel model) {
        Module module = super.commitModule(project, model);
        if (module == null) {
            return null;
        }
        final CCProjectService service = CCProjectService.getInstance(project);
        final Course course = new Course();
        course.setName("Custom Course");
        course.setAuthors(new String[]{""});
        course.setDescription("empty description");
        course.setLanguage("JAVA");
        service.setCourse(course);
        return module;
    }
}
