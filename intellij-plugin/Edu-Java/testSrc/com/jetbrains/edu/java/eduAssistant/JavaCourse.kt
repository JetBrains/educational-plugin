package com.jetbrains.edu.java.eduAssistant

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.learning.course

val javaCourse = course(language = JavaLanguage.INSTANCE) {
  lesson {
    eduTask {
      javaTaskFile(
        "src/main/kotlin/Main.java", """
          public class Main {
              public static String invokeSayHello(int howManyTimes) {
                  Collection<String> list = Collections.nCopies(howManyTimes, "Hello");
                  return String.join(System.lineSeparator(), list);
              }

              public static void main(String[] args) {
                  System.out.println("How many times should I print Hello?");
                  Scanner scanner = new Scanner(System.in);
                  int howManyTimes = scanner.nextInt();
                  System.out.println(invokeSayHello(howManyTimes));
              }
          }
        """
      )
    }
  }
}
