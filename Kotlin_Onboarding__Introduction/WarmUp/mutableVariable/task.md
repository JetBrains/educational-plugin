Loops often require changing the value of a variable.
To do that, you can use the [`var`](https://kotlinlang.org/docs/basic-syntax.html#variables) variable:
```kotlin
var y = 5
do {
    println(y)
    y += 2
} while(y < 10)
```
This code initializes the variable `y` with the value `5` and next, changes the variable in the loop.
The loop will be executed in 3 steps:
1) y = 5. println(y). y = 7.
2) because y < 10, println(y), y = 9.
3) because y < 10, println(y), y = 11.
4) because y > 10, stop the loop.
   In the end, the following numbers: 5, 7, and 9 will be printed in the console.
