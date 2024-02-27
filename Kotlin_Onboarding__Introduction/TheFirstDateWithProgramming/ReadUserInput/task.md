In this task, you will ask the user three questions and save their answers.

### Task

Ask the user three questions and add the answers 
to the _firstUserAnswer_, _secondUserAnswer_, and _thirdUserAnswer_ variables respectively.
The questions are:

```text
What is TROTEN?

How did you spend your graduation?

Why does a spider need eight legs?
```

You must ask questions and record answers sequentially, 
i.e. first ask the first question (_print_ it to console), 
then record the answer in the _firstUserAnswer_ variable. 
Then do the same with the second question and the _secondUserAnswer_ variable, 
and finally with the third question and the _thirdUserAnswer_ variable.

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="hint" title="Click me to view an example with the first question">

To print a question, you can use the `println` function from the previous steps.
Then, to read the user input, you can use the `readlnOrNull` function:

```kotlin
fun main() {
    println("Hello! I will ask you several questions.")
    println("Please answer all of them and be honest with me!")
    println("What is TROTEN?")
    val firstUserAnswer = readlnOrNull()
    // You need to ask two others questions bellow
    val secondUserAnswer = ""
    val thirdUserAnswer = ""
}
```

</div>

<div class="hint" title="Click me to view the expected state of the application after completing this task">

As a result, the user's interaction with the game will look like this:

![User interaction example](../../utils/src/main/resources/images/part1/first.date/user_input.gif "User interaction example")

</div>
