package com.jetbrains.edu.coursecreator.handlers.rename;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFileSystemItem;
import com.jetbrains.edu.coursecreator.actions.StudyItemType;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Section;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer;
import org.jetbrains.annotations.NotNull;

public class CCSectionRenameHandler extends CCStudyItemRenameHandler {
  public CCSectionRenameHandler() {
    super(StudyItemType.SECTION);
  }

  @Override
  protected boolean isAvailable(@NotNull Project project, @NotNull VirtualFile file) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return false;
    }
    return file.isDirectory() && course.getSection(file.getName()) != null;
  }

  @Override
  protected void rename(@NotNull Project project, @NotNull Course course, @NotNull PsiFileSystemItem item) {
    if (!(item instanceof PsiDirectory)) return;
    PsiDirectory directory = (PsiDirectory)item;
    Section section = course.getSection(directory.getName());
    if (section != null) {
      processRename(section, EduNames.SECTION, course, project, directory.getVirtualFile());
      YamlFormatSynchronizer.saveItem(course);
    }
  }

  @Override
  public String getActionTitle() {
    return "Rename section";
  }
}
