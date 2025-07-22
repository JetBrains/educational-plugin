package com.jetbrains.edu.go.slow.checker

import com.goide.GoLanguage
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import org.junit.Test

class GoCheckerTest : GoCheckersTestBase() {
  override fun createCourse(): Course {
    return course(language = GoLanguage.INSTANCE) {
      lesson {
        eduTask("Edu") {
          goTaskFile("task.go", """
            package task

            // todo: replace this with an actual task
            func Sum(a, b int) int {
              return a + b
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
        eduTask("EduWithIgnoredTest") {
          goTaskFile("task.go", """
            package task

            // todo: replace this with an actual task
            func Sum(a, b int) int {
              return a + b
            }
          """)
          taskFile("go.mod", """
            module eduwithignoredtest
          """)
          goTaskFile("test/task_test.go", """
            package test

            import (
              task "eduwithignoredtest"
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
                {"ignored", args{1, 2}, 4},
              }
              for _, tt := range tests {
                t.Run(tt.name, func(t *testing.T) {
                  if tt.name == "ignored" {
                    t.Skip()
                  }
                  if got := task.Sum(tt.args.a, tt.args.b); got != tt.want {
                    t.Errorf("Sum() = %v, want %v", got, tt.want)
                  }
                })
              }
            }
          """)
        }
        eduTask("EduWithCustomRunConfiguration") {
          goTaskFile("task.go", """
            package task

            import "os"

            func Hello() string {
              return os.Getenv("EXAMPLE_ENV")
            }
          """)
          taskFile("go.mod", """
            module eduwithcustomrunconfiguration
          """)
          goTaskFile("test/task_test.go", """
            package test

            import (
              task "eduwithcustomrunconfiguration"
              "testing"
            )

            func TestSum(t *testing.T) {
              tests := []struct {
                name string
                want string
              }{
                {"hello", "Hello!"},
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
                <working_directory value="${'$'}TASK_DIR$/test" />
                <envs>
                  <env name="EXAMPLE_ENV" value="Hello!" />
                </envs>
                <root_directory value="${'$'}TASK_DIR$" />
                <kind value="PACKAGE" />
                <package value="eduwithcustomrunconfiguration/test" />
                <directory value="${'$'}PROJECT_DIR$" />
                <filePath value="${'$'}PROJECT_DIR$" />
                <framework value="gotest" />
                <pattern value="^\QTestSum\E$/^\Qhello\E$" />
                <method v="2" />
              </configuration>
            </component>
          """)
        }
        outputTask("Output") {
          goTaskFile("main.go", """
              package main
              import (
              "fmt"
              "os"
              )

              func main() {
                var name string
                fmt.Fscan(os.Stdin, &name)
                fmt.Println(name)
              }
          """)
          taskFile("go.mod", """
            module task2
          """)
          taskFile("output.txt","input text")
          taskFile("input.txt","input text")
        }
      }
    }
  }

  @Test
  fun `test go course`() {
    CheckActionListener.expectedMessage { CheckUtils.CONGRATULATIONS }
    doTest()
  }
}