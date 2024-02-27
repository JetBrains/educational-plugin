It's time for practice!

### Task

Add variables of different types to customize the game 
and display the game information in the console:
- `Int` variable `wordLength` to define the length of the word
- `Int` variable `maxAttemptsCount` to define the max number of user attempts
- `String` variable `secretExample` to define an example of the hidden word (to explain it to the user)

You can later initialize these variables with any value you like, but currently, let's define them as follows:
```text
wordLength with value 4
maxAttemptsCount with value 3
secretExample with value ACEB
```

Then, just call already implemented function `getGameRules` with three arguments: `wordLength`, `maxAttemptsCount`, `secretExample` and print it's output:
```kotlin
println(getGameRules(wordLength, maxAttemptsCount, secretExample))
```

This function will return the game rules with values from the variables, that use defined as a string and then we will print the rules 
with the already familiar function `println`.
We will consider custom functions in the next task in more detail.

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="hint" title="Push to learn about line breaks in different OS">

  Different operating systems use different line break characters. 
  Use the predefined variable `newLineSymbol` with a newline character instead of `\n` to 
  separate lines correctly.
</div>
