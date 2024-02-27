It's time to complete the project!

### Task

Print `<some user's answer> is great! I hope you successfully complete this course! Anyone can learn programming at any age!`,
where instead of `<some user's answer>`, you need to print the user's answer from the previous task. For instance, if the user answered `20`,
the text `20 is great! I hope you successfully complete this course! Anyone can learn programming at any age!` will be printed.

**Note**: to avoid typos, just copy the text from here and paste it into your code.

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="hint" title="Click me to view the expected state of the game after completing this task">

The game should look like this:

![Chat example](../../utils/src/main/resources/images/part1/chat/game.gif "Chat example")

</div>

<div class="hint" title="Click me to learn which functions can be helpful to solve this task">

To print the answer, you can use the `println` function from the previous steps.

</div>

<div class="hint" title="Click me to learn how to combine text and string variables together">

String literals may contain template expressions – pieces of code that are
evaluated and whose results are concatenated into the string.
[A template expression](https://kotlinlang.org/docs/strings.html#string-templates) starts with a dollar sign (`$`) and consists of either a name or an expression in curly braces.

To insert something into a string, you can use the following construction:
```kotlin
val age = readlnOrNull()
println("$age is great!")
```
</div>

