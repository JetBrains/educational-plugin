Let's make our game more fun!

### Task

Implement the `generateSecret` function to return a random secret word for the game:
- add new arguments: `wordLength` and `alphabet`;

<div class="hint" title="Click me to see the new signature of the generateSecret function">

The signature of the function is:
```kotlin
fun generateSecret(wordLength: Int, alphabet: String): String
```
</div>

- implement the body that generates a random word with `wordLength` letters from the `alphabet` 
instead of always returning `ABCD`.

**Note**, you need to define a new variable `alphabet` in the `main` function and initialize it with the `ABCDEFGH` value to pass tests.
Don't forget to use the `wordLength` and `alphabet` variables when calling the `generateSecret` function.

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="Hint" title="Click me to learn how to implement the generateSecret function">

You can create a new list with `wordLength` elements using _random_ letters from `alphabet`.
Finally, you can use the `joinToString` function with an empty separator (`""`) to build the final string.
</div>
