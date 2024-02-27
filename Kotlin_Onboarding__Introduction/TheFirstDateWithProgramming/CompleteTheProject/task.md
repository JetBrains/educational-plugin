### Task

After the user has answered the mock questions implemented in the previous steps, 
print the text `Now let's have fun!`. 
Then, print the real questions along with the user’s previous answers. 
The real questions are stored in the already **predefined** `firstQuestion`, `secondQuestion`, and `thirdQuestion` variables.

_Predefined_ means that you can access these variables 
because the course creator put them in the project and assigned the necessary values. 
For example, you can write `println(firstQuestion)` to print 
the value from the **predefined** `firstQuestion` variable.
You can find all these variables in the `RealQuestions.kt` file.

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="hint" title="Click me to view an example with the first real question">

To print the first  **predefined** question from the `firstQuestion` variable and the user answer, you
can use the `println` function from the previous steps:

```kotlin
fun main() {
    println("Hello! I will ask you several questions.")
    println("Please answer all of them and be honest with me!")
    println("What is TROTEN?")
    val firstUserAnswer = readlnOrNull()
    println("How did you spend your graduation?")
    val secondUserAnswer = readlnOrNull()
    println("Why does a spider need eight legs?")
    val thirdUserAnswer = readlnOrNull()
    println("Now let's have fun!")
    println(firstQuestion)
    println(firstUserAnswer)
}
```

</div>

<div class="hint" title="Click me to view the expected state of the game after completing this task">

The game should look like this:

![The game's example](../../utils/src/main/resources/images/part1/first.date/game.gif "The game's example")

</div>

<div class="hint" title="Click me to learn how to combine text and string variables together">

String literals may contain template expressions – pieces of code that are
evaluated and whose results are concatenated into the string.
[A template expression](https://kotlinlang.org/docs/strings.html#string-templates) starts with a dollar sign (`$`) and consists of either a name or an expression in curly braces.

To insert something into a string, you can use the following construction:
```kotlin
val a = 5
println("a = $a") // a = 5 will be printed
```
</div>

