In this task we will call already implemented functions inside the main function.

### Task

Call the `playGame` function in the `main` function.
Don't forget to use the `generateSecret` function to get a secret and pass as an argument to the `playGame` function.

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="Hint" title="Click me to learn how to pass a generated secret inside the playGame function">

You can call the `generateSecret` and write the output to a variable, or you can pass the result 
without creating an extra variable:
```kotlin
fun main() {
    val wordLength = 4
    val maxAttemptsCount = 3
    val secretExample = "ACEB"
    println(getGameRules(wordLength, maxAttemptsCount, secretExample))
    
    val secret = generateSecret()
    playGame(secret, wordLength, maxAttemptsCount)
}
```
or
```kotlin
fun main() {
    val wordLength = 4
    val maxAttemptsCount = 3
    val secretExample = "ACEB"
    println(getGameRules(wordLength, maxAttemptsCount, secretExample))
    
    playGame(generateSecret(), wordLength, maxAttemptsCount)
}
```
</div>
