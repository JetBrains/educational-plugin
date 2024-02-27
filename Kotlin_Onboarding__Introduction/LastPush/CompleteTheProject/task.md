On this step, you need to complete the app.

### Task

Implement the `applyGenerator` function, which accepts `pattern`, `generatorName`, `width`, and `height`,
then trims the `pattern`, and finally, applies the `canvasGenerator` or `canvasWithGapsGenerator` function.

<div class="hint" title="Click me to see the new signature of the applyGenerator function">

The signature of the function is:
```kotlin
fun applyGenerator(pattern: String, generatorName: String, width: Int, height: Int): String
```
</div>

The possible values for the `generatorName` argument are:

- `canvas` – it calls the `canvasGenerator` function
- `canvasGaps` - it calls the `canvasWithGapsGenerator` function

The `applyGenerator` function should throw an error to alert the user about an unexpected filter name.

<div class="hint" title="Click me to see the patterns generator project example">

  ![The patterns generator example](../../utils/src/main/resources/images/part1/last.push/app.gif "The patterns generator example")

</div>


Also, the `main` function will be checked - just uncomment the code in the `main` function.

Good luck!
