package jetbrains.course.tutorial

import org.jetbrains.academy.cognifire.dsl.*

fun calculateDogAgeInDogYears() {
  prompt("""
        Get the user input and save the result to `humanYearsInput`.
        Call the function `verifyHumanYearsInput` with `humanYearsInput`. 
        If the function returns true, set `humanYears` equal to `humanYearsInput` interpreted as an integer. 
        Then, initialize `dogYears` to `humanYears` multiplied by 7 and output the message "Your dog's age in dog years is: `dogYears`".
        If the function returns false, output the message "Oops! That doesn't look like a valid age.".
    """)
  code {
    val humanYearsInput = readln()
    if (verifyHumanYearsInput(humanYearsInput)) {
      val humanYears = humanYearsInput.toInt()
      val dogYears = humanYears * 7
      println("Your dog's age in dog years is: $dogYears")
    } else {
      println("Oops! That doesn't look like a valid age.")
    }
  }
}

fun verifyHumanYearsInput(humanYearsInput: String): Boolean {
  TODO("Not implemented yet")
}

fun main() {
  calculateDogAgeInDogYears()
}
