In this task, you will respond to the user and then ask the next question!

### Task

Firstly, print `Nice to meet you, <some user's answer>! My name is Kotlin Bot! I am a young programming language created in 2010. How old are you?`,
where you replace `<some user's answer>` with the user's answer from the previous task. For instance, if the user answered `John`,
the text `Nice to meet you, John! My name is Kotlin Bot! I am a young programming language created in 2010. How old are you?` will be printed.

After that, read the second user's answer into a variable (you can use any variable name you wish).

**Note**: to avoid typos, just copy the text from here and paste it into your code.

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="hint" title="Click me to learn which functions can be helpful to solve this task">

To print the answer, you can use the `println` function from the previous steps.
Then, to read the user's input into a variable, you can use the `readlnOrNull` function.

</div>

<div class="hint" title="Click me to learn how to combine text and string variables together">

String literals may contain template expressions – pieces of code that are
evaluated and whose results are concatenated into the string.
[A template expression](https://kotlinlang.org/docs/strings.html#string-templates) starts with a dollar sign (`$`) and consists of either a name or an expression in curly braces.

To insert something into a string, you can use the following construction:
```kotlin
val name = readlnOrNull()
println("Nice to meet you, $name!")
```
</div>
