package com.jetbrains.edu.learning

import com.jetbrains.edu.coursecreator.AdditionalFilesUtils


class CourseIgnoreTest : EduTestCase() {

  fun `test first level ignored`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def f():
          |  <p>print(1)</p>
        """.trimMargin("|"))
        }
      }
      additionalFiles {
        eduFile(".courseignore", "ignored.txt")
        eduFile("ignored.txt")
        eduFile("not-ignored.txt")
      }
    }
    val additionalFiles = AdditionalFilesUtils.collectAdditionalFiles(course, project)
    assertSameElements(additionalFiles.map { it.name }, listOf("not-ignored.txt"))
  }

  fun `test second level ignored`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def f():
          |  <p>print(1)</p>
        """.trimMargin("|"))
          taskFile("NotIgnored.txt")
        }
      }
      additionalFiles {
        eduFile(".courseignore",
                """
                  |tmp/ignored.txt
                  |lesson1/task1/NotIgnored.txt
                """.trimMargin())
        eduFile("not-ignored.txt")
        eduFile("tmp/ignored.txt")
        eduFile("tmp/not-ignored.txt")
      }
    }
    val additionalFiles = AdditionalFilesUtils.collectAdditionalFiles(course, project)
    assertSameElements(additionalFiles.map { it.name }, listOf("not-ignored.txt", "tmp/not-ignored.txt"))

    val task = course.findTask("lesson1", "task1")
    assertNotNull(task)
    assertSameElements(task.taskFiles.keys, listOf("NotIgnored.txt", "Task.kt"))
  }

  fun `test courseignore works as gitignore`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task.kt")
          taskFile("NotIgnored.txt")
          dir("dir1") {
            taskFile("a.txt")
            taskFile("b.txt")
            // taskFile("c.txt") exists as an additional file, it is not included independently of courseignore
          }
        }
      }
      additionalFiles {
        eduFile("dir/a.txt")
        eduFile("dir/b.txt")
        eduFile("dir/c.txt")
        eduFile("dir/x.txt")
        eduFile("dir/y.txt")
        eduFile("dir/z.txt")

        eduFile("folder/p.txt")
        eduFile("folder/q.txt")
        eduFile("folder/r.txt")

        eduFile("subfolder/x.txt")

        eduFile("file2/a.txt")

        eduFile("a.txt")
        eduFile("b.txt")
        eduFile("c.txt")
        eduFile("x.txt")
        eduFile("xyz.txt")
        eduFile("dir.txt") // not excluded, although there is a dir*/ entry

        eduFile("1-2.txt")
        eduFile("3+4.txt")
        eduFile("i-j.txt")

        eduFile("file1.txt")

        eduFile("lesson1/task1/dir1/c.txt")

        eduFile(
          ".courseignore",
          """
            |# exclude directories (not files) starting with dir
            |dir*/
            |
            |# exclude all files and directories starting with file
            |file*
            |
            |# exclude all a.txt
            |a.txt
            |
            |# exclude b.txt, c.txt
            |[bc].txt
            |# we don't exclude c.txt, but it is inside the task, and it should not anyway go inside the archive
            |!lesson1/task1/dir1/c.txt
            |
            |# exclude only root x.txt
            |/x.txt
            |
            |# test negative patterns
            |folder/*
            |!folder/p.txt
            |!folder/q.txt
            |# but then again, ignore q.txt
            |q.txt
            |
            |# regular expressions should also work
            |syntax: regexp
            |
            |\d(-|\+)\d\.txt
          """.trimMargin()
        )
      }
    }

    val additionalFiles = AdditionalFilesUtils.collectAdditionalFiles(course, project)
    assertSameElements(additionalFiles.map { it.name }, listOf("folder/p.txt", "dir.txt", "i-j.txt", "xyz.txt", "subfolder/x.txt"))

    val task = course.findTask("lesson1", "task1")
    assertNotNull(task)
    assertSameElements(task.taskFiles.keys, listOf("NotIgnored.txt", "Task.kt", "dir1/a.txt", "dir1/b.txt"))
  }
}