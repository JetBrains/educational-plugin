package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Section;
import com.jetbrains.edu.learning.stepik.StepikNames;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EduTreeStructureProvider implements TreeStructureProvider, DumbAware {
  @NotNull
  @Override
  public Collection<AbstractTreeNode> modify(@NotNull AbstractTreeNode parent,
                                             @NotNull Collection<AbstractTreeNode> children,
                                             ViewSettings settings) {
    Project project = parent.getProject();
    if (project == null || !shouldModify(project)) {
      return children;
    }
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return children;
    }
    Collection<AbstractTreeNode> modifiedNodes = new ArrayList<>();
    for (AbstractTreeNode node : children) {
      if (parent instanceof ProjectViewProjectNode && node instanceof PsiDirectoryNode) {
        modifiedNodes.add(createCourseNode(project, node, settings, course));
        continue;
      }
      if (parent instanceof EduNode) {
        AbstractTreeNode modifiedNode = ((EduNode)parent).modifyChildNode(node);
        if (modifiedNode != null) {
          modifiedNodes.add(modifiedNode);
        }
      }
    }
    if (parent instanceof CourseNode) {
      final List<Section> sections = course.getSections();
      if (hasVisibleSections(course)) {
        for (Section section : sections) {
          modifiedNodes.add(new SectionNode(project, settings, section, course, ((CourseNode)parent).getValue()));
        }
      }
    }

    return modifiedNodes;
  }

  public static boolean hasVisibleSections(Course course) {
    final List<Section> sections = course.getSections();
    if (sections.isEmpty()) {
      return false;
    }
    final String firstSectionTitle = sections.get(0).getTitle();
    if (firstSectionTitle.equals(course.getName())) {
      if (sections.size() == 1) {
        return false;
      }
      final String secondSectionTitle = sections.get(1).getTitle();
      if (sections.size() == 2 && (secondSectionTitle.equals(EduNames.ADDITIONAL_MATERIALS) ||
                                   secondSectionTitle.equals(StepikNames.PYCHARM_ADDITIONAL))) {
        return false;
      }
    }
    return true;
  }

  @NotNull
  protected CourseNode createCourseNode(Project project, AbstractTreeNode node, ViewSettings settings, Course course) {
    return new CourseNode(project, ((PsiDirectory)node.getValue()), settings, course);
  }

  protected boolean shouldModify(@NotNull final Project project) {
    return EduUtils.isStudentProject(project);
  }
}
