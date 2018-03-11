package com.jetbrains.edu.learning.projectView;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.ui.SimpleTextAttributes;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Section;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;


public class SectionNode extends ProjectViewNode<Section> {
  protected final Section mySection;
  private final Course myCourse;
  private final PsiDirectory myCourseDir;

  public SectionNode(@NotNull Project project, ViewSettings viewSettings, @NotNull Section section,
                     Course course, PsiDirectory courseDir) {
    super(project, section, viewSettings);
    mySection = section;
    myCourse = course;
    myCourseDir = courseDir;
  }

  @Override
  public boolean contains(@NotNull final VirtualFile file) {
    if (myCourseDir == null || !myCourseDir.isValid()) {
      return false;
    }

    final PsiFile containingFile = myCourseDir.getContainingFile();
    if (containingFile == null) {
      return false;
    }
    final VirtualFile valueFile = containingFile.getVirtualFile();
    return file.equals(valueFile);
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode> getChildren() {
    return CourseNode.getLessonNodes(myProject, myCourse, myCourseDir,
                                     getSettings(), lesson -> mySection.lessonIds.contains(lesson.getId()),
                                     (lesson, lessonDir) -> new LessonNode(myProject, lessonDir, getSettings(), lesson));
  }

  @Override
  protected void update(PresentationData data) {
    data.addText(mySection.getTitle(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    data.setIcon(AllIcons.Ide.Error);
  }
}
