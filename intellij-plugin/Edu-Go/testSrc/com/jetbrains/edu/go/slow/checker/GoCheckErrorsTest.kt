package com.jetbrains.edu.go.slow.checker

import com.goide.GoLanguage
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckResultDiffMatcher
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.nullValue
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class GoCheckErrorsTest : GoCheckersTestBase() {
  override fun createCourse(): Course {
    return course(language = GoLanguage.INSTANCE) {
      lesson {
        eduTask("EduTestFailed") {
          goTaskFile("task.go", """
            package task

            // todo: replace this with an actual task
            func Sum(a, b int) int {
              return a + b + 1
            }
          """)
          taskFile("go.mod", """
            module task1
          """)
          goTaskFile("test/task_test.go", """
            package test
            
            import (
            	task "task1"
            	"testing"
            )
            
            //todo: replace this with an actual test
            func TestSum(t *testing.T) {
            	type args struct {
            		a int
            		b int
            	}
            	tests := []struct {
            		name string
            		args args
            		want int
            	}{
            		{"1", args{1, 1}, 2},
            		{"2", args{1, 2}, 3},
            	}
            	for _, tt := range tests {
            		t.Run(tt.name, func(t *testing.T) {
            			if got := task.Sum(tt.args.a, tt.args.b); got != tt.want {
            				t.Errorf("Sum() = %v, want %v", got, tt.want)
            			}
            		})
            	}
            }

          """)
        }
        eduTask("EduCompilationFailed") {
          goTaskFile("task.go", """
            package task

            // todo: replace this with an actual task
            func Sum(a, b int) int {
              return a + b +
            }
          """)
          taskFile("go.mod", """
            module task1
          """)
          goTaskFile("test/task_test.go", """
            package test
            
            import (
            	task "task1"
            	"testing"
            )
            
            //todo: replace this with an actual test
            func TestSum(t *testing.T) {
            	type args struct {
            		a int
            		b int
            	}
            	tests := []struct {
            		name string
            		args args
            		want int
            	}{
            		{"1", args{1, 1}, 2},
            		{"2", args{1, 2}, 3},
            	}
            	for _, tt := range tests {
            		t.Run(tt.name, func(t *testing.T) {
            			if got := task.Sum(tt.args.a, tt.args.b); got != tt.want {
            				t.Errorf("Sum() = %v, want %v", got, tt.want)
            			}
            		})
            	}
            }

          """)
        }
        eduTask("EduWithCustomRunConfigurationTestFailed") {
          goTaskFile("task.go", """
            package task
            
            import "os"
            
            func Hello() string {
              return os.Getenv("EXAMPLE_ENV")
            }
          """)
          taskFile("go.mod", """
            module eduwithcustomrunconfigurationtestfailed
          """)
          goTaskFile("test/task_test.go", """
            package test
            
            import (
              task "eduwithcustomrunconfigurationtestfailed"
              "testing"
            )
            
            func TestSum(t *testing.T) {
              tests := []struct {
                name string
                want string
              }{
                {"hello", "Hello"},
                {"fail", "Hello"},
              }
              for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                  if tt.name == "fail" {
                    t.FailNow()
                  }
                  if got := task.Hello(); got != tt.want {
                    t.Errorf("Hello() = %v, want %v", got, tt.want)
                  }
                })
              }
            }
          """)
          xmlTaskFile("runConfigurations/CustomCheck.run.xml", """
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="CustomCheck" type="GoTestRunConfiguration" factoryName="Go Test">
                <module name="Go Course3" />
                <working_directory value="${'$'}TASK_DIR${'$'}/test" />
                <envs>
                  <env name="EXAMPLE_ENV" value="Hello!" />
                </envs>
                <root_directory value="${'$'}TASK_DIR${'$'}" />
                <kind value="PACKAGE" />
                <package value="eduwithcustomrunconfigurationtestfailed/test" />
                <directory value="${'$'}PROJECT_DIR${'$'}" />
                <filePath value="${'$'}PROJECT_DIR${'$'}" />
                <framework value="gotest" />
                <pattern value="^\QTestSum\E${'$'}/^\Qhello\E${'$'}" />
                <method v="2" />
              </configuration>
            </component>        
          """)
        }
        outputTask("OutputTestFailed") {
          goTaskFile("main.go", """
              package main
              import "fmt"

              func main() {
	              fmt.Print("No")
              }
          """)
          taskFile("go.mod", """
            module task2
          """)
          taskFile("output.txt", "Yes")
        }
        outputTask("OutputMultilineTestFailed") {
          goTaskFile("main.go", """
              package main
              import "fmt"

              func main() {
	              fmt.Println("1\n2")
              }
          """)
          taskFile("go.mod", """
            module task3
          """)
          taskFile("output.txt") {
            withText("1\n\n2\n\n")
          }
        }
      }
    }
  }

  @Test
  fun `test go errors`() {
    val incorrect = EduCoreBundle.message("check.incorrect")
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      assertEquals(CheckStatus.Failed, checkResult.status)
      val (messageMatcher, diffMatcher) = when (task.name) {
        "EduTestFailed" -> equalTo(incorrect) to nullValue()
        "EduCompilationFailed" -> equalTo(EduCoreBundle.message("error.execution.failed")) to nullValue()
        "EduWithCustomRunConfigurationTestFailed" -> equalTo(incorrect) to nullValue()
        "OutputTestFailed" -> equalTo(incorrect) to
          CheckResultDiffMatcher.diff(CheckResultDiff(expected = "Yes", actual = "No"))
        "OutputMultilineTestFailed" -> equalTo(incorrect) to
          CheckResultDiffMatcher.diff(CheckResultDiff(expected = "1\n\n2\n\n", actual = "1\n2\n"))
        else -> error("Unexpected task name: ${task.name}")
      }
      assertThat("Checker output for ${task.name} doesn't match", checkResult.message, messageMatcher)
      assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, diffMatcher)
    }
    doTest()
  }
}
