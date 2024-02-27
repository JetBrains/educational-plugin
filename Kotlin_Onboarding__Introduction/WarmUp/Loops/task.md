It's time to start implementing the `playGame` function to be able to read user guesses.

### Task

Define a new function `playGame`, which accepts `secret`, `wordLength`, and `maxAttemptsCount` 
and imitates the game process.

<div class="hint" title="Click me to see the signature of the playGame function">

The signature of the function is:
```kotlin
fun playGame(secret: String, wordLength: Int, maxAttemptsCount: Int): Unit
```
</div>

This function should have a loop while game is not complete 
(we can use the `isComplete` function from the previous steps here).
Inside the loop you need to ask the user to input a guess and write their answer into the `guess` variable.
After asking about input you need to recheck if the game was compiled to avoid having an infinite loop.

It is better to ask user what do you expect to get, so print this text before read line user input:

```text
Please input your guess. It should be of length <wordLength>.
```
where instead `<wordLength>` you need to print the value from the `wordLength` function argument, e.g. if the value is `4`,
the text `Please input your guess. It should be of length 4` will be printed.

**Note**: to avoid typos just copy the text from here and paste into your code.

**Note**: Instead of the `readlnOrNull` function, please use the `safeReadLine` function here. 
It is a custom function from the course authors that makes the interaction with the user input easier:

```kotlin
val guess = safeReadLine()
```

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="Hint" title="Click me to get help with `playGame` function">

Use `do-while` loop for imitating the game process.
</div>

<div class="Hint" title="Click me to learn which type for the condition variable for the loop is better to use">

The best type for the `complete` variable for the loop condition is `Boolean`, since it indicates only two game states.
</div>

<div class="Hint" title="Click me to get a code style hint">

If you use the `do-while` loop with a `Boolean` variable as the condition, 
you can omit the initialization of the variable before the loop. For example, consider the following code:
```
var myBool = false
do {
    myBool = getNewValue()
} while (!myBool)
```
It can be replaced with:
```
var myBool: Boolean
do {
    myBool = getNewValue()
} while (!myBool)
```
You can do it only if the value of the variable is <b>changed</b> inside the loop.
</div>

<div class="Hint" title="Click me to see the correct solution of this task">

One of the possible ways to solve this task:
```kotlin
fun playGame(secret: String, wordLength: Int, maxAttemptsCount: Int) {
    var complete: Boolean
    do {
        println("Please input your guess. It should be of length $wordLength.")
        val guess = safeReadLine()
        complete = isComplete(secret, guess)
    } while (!complete)
}
```
</div>
