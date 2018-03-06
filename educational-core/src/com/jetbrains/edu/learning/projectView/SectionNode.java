package com.jetbrains.edu.learning.projectView;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.SimpleTextAttributes;
import com.jetbrains.edu.learning.courseFormat.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static com.jetbrains.edu.learning.projectView.CourseViewPane.HIDE_SOLVED_LESSONS;


public class SectionNode extends ProjectViewNode<Section> implements EduNode {
  @NotNull protected final Project myProject;
  protected final ViewSettings myViewSettings;
  protected final Section mySection;
  private final Course myCourse;
  private final PsiDirectory myCourseDir;

  public SectionNode(@NotNull Project project,
                     ViewSettings viewSettings,
                     @NotNull Section section, Course course, PsiDirectory courseDir) {
    super(project, section, viewSettings);
    myProject = project;
    myViewSettings = viewSettings;
    mySection = section;
    myCourse = course;
    myCourseDir = courseDir;
  }

  @Nullable
  public AbstractTreeNode modifyChildNode(AbstractTreeNode childNode) {
    final Object value = childNode.getValue();
    if (value instanceof PsiDirectory) {
      Lesson lesson = myCourse.getLesson(((PsiDirectory)value).getName());
      if (lesson != null) {
        final CheckStatus status = lesson.getStatus();
        if (status.equals(CheckStatus.Solved) && PropertiesComponent.getInstance().getBoolean(HIDE_SOLVED_LESSONS, false)) {
          return null;
        }
      }
      if (lesson == null || !mySection.lessonIds.contains(lesson.getId())) return null;
      return CourseNode.modifyLessonNode(myCourse, myProject, myViewSettings, childNode);
    }
    return null;
  }

  @Override
  public PsiDirectoryNode createChildDirectoryNode(StudyItem item, PsiDirectory directory) {
    return CourseNode.createLessonNode(myCourse, myProject, myViewSettings, (Lesson)item, directory);
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    if (file.isDirectory()) {
      Lesson lesson = myCourse.getLesson(file.getName());
      if (lesson != null) {
        final CheckStatus status = lesson.getStatus();
        if (status.equals(CheckStatus.Solved) && PropertiesComponent.getInstance().getBoolean(HIDE_SOLVED_LESSONS, false)) {
          return false;
        }
      }
      if (lesson == null || !mySection.lessonIds.contains(lesson.getId())) return false;
    }
    return true;
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode> getChildren() {
    return ProjectViewDirectoryHelper.getInstance(myProject).getDirectoryChildren(myCourseDir, getSettings(), true, null);
  }

  @Override
  protected void update(PresentationData data) {
    data.addText(mySection.getTitle(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    data.setIcon(AllIcons.Ide.Error);
  }
}
