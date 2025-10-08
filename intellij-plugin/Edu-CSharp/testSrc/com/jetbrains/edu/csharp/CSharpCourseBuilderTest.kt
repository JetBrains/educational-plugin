package com.jetbrains.edu.csharp

import com.jetbrains.edu.learning.CourseBuilder
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.rider.languages.fileTypes.csharp.CSharpLanguage
import org.junit.Test


class CSharpCourseBuilderTest : CSharpCourseGenerationTestBase() {

  @Test
  fun `test new educator course`() {
    val course = csharpCourse("My Test Course", CourseMode.EDUCATOR)
    createCourseStructure(course)
    val expectedFileTree = fileTree {
      file("${course.name}.sln")
      dir("lesson1") {
        dir("task1") {
          file("Lesson1.Task1.csproj")
          file("task.md")
          dir("src") {
            file("Task.cs", """
              // ReSharper disable all CheckNamespace
              
              class Task
              {
                  public static void Main(string[] args)
                  {
                      // your code here
                      Console.WriteLine("Hello world");
                  }
              }
            """)
          }
          dir("test") {
            file("Test.cs", """
              // ReSharper disable all CheckNamespace
              // Please ensure all the tests are contained within the same namespace
              
              [TestFixture]
              internal class Test
              {
                  [Test]
                  public void Test1()
                  {
                      Assert.Fail("Tests are not implemented");
                  }
              }
              """)
          }
        }
      }
    }
    expectedFileTree.assertExists(rootDir)

    assertListOfAdditionalFiles(course)
  }

  @Test
  fun `test student course`() {
    val course = csharpCourse("My Test Course", CourseMode.STUDENT) {
      additionalFile("My Test Course.sln", solutionFile)
      lesson("Lesson 1") {
        eduTask("Task 1") {
          csTaskTestFiles("Lesson1.Task1")
        }
        eduTask("Task 2") {
          csTaskTestFiles("Lesson1.Task2")
        }
        theoryTask("Task 3") {
          csTaskFiles("Lesson1.Task3")
        }
      }
      lesson("Another Lesson") {
        eduTask("First task") {
          csTaskTestFiles("AnotherLesson.FirstTask")
        }
      }
    }

    createCourseStructure(course)
    val expectedFileTree = fileTree {
      file("${course.name}.sln")
      dir("Lesson 1") {
        dir("Task 1") {
          file("Lesson1.Task1.csproj")
          file("task.md")
          dir("src") {
            file("Task.cs", """
              // ReSharper disable all CheckNamespace
              
              class Task
              {
                  public static void Main(string[] args)
                  {
                      // your code here
                      Console.WriteLine("Hello world");
                  }
              }
            """)
          }
          dir("test") {
            file("Test.cs", """
              // ReSharper disable all CheckNamespace
              // Please ensure all the tests are contained within the same namespace
              
              [TestFixture]
              internal class Test
              {
                  [Test]
                  public void Test1()
                  {
                      Assert.Fail("Tests are not implemented");
                  }
              }
              """)
          }
        }
        dir("Task 2") {
          file("Lesson1.Task2.csproj")
          file("task.md")
          dir("src") {
            file("Task.cs", """
              // ReSharper disable all CheckNamespace
              
              class Task
              {
                  public static void Main(string[] args)
                  {
                      // your code here
                      Console.WriteLine("Hello world");
                  }
              }
            """)
          }
          dir("test") {
            file("Test.cs", """
              // ReSharper disable all CheckNamespace
              // Please ensure all the tests are contained within the same namespace
              
              [TestFixture]
              internal class Test
              {
                  [Test]
                  public void Test1()
                  {
                      Assert.Fail("Tests are not implemented");
                  }
              }
              """)
          }
        }
        dir("Task 3") {
          file("Lesson1.Task3.csproj")
          file("task.md")
          dir("src") {
            file("Task.cs", """
              // ReSharper disable all CheckNamespace
              
              class Task
              {
                  public static void Main(string[] args)
                  {
                      // your code here
                      Console.WriteLine("Hello world");
                  }
              }
            """)
          }
        }
      }
      dir("Another Lesson") {
        dir("First task") {
          file("AnotherLesson.FirstTask.csproj")
          file("task.md")
          dir("src") {
            file("Task.cs", """
              // ReSharper disable all CheckNamespace
              
              class Task
              {
                  public static void Main(string[] args)
                  {
                      // your code here
                      Console.WriteLine("Hello world");
                  }
              }
            """)
          }
          dir("test") {
            file("Test.cs", """
              // ReSharper disable all CheckNamespace
              // Please ensure all the tests are contained within the same namespace
              
              [TestFixture]
              internal class Test
              {
                  [Test]
                  public void Test1()
                  {
                      Assert.Fail("Tests are not implemented");
                  }
              }
              """)
          }
        }
      }
    }
    expectedFileTree.assertExists(rootDir)
  }

  private fun csharpCourse(courseName: String, courseMode: CourseMode, buildCourse: CourseBuilder.() -> Unit = {}): Course = course(
    name = courseName,
    language = CSharpLanguage,
    courseMode = courseMode,
    buildCourse = buildCourse
  )
}