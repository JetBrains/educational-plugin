## Versions changelog

### XML format version

9.  Drop `AnswerPlaceholderSubtaskInfo` and move all info into `AnswerPlaceholder` object

	Previous format of `AnswerPlaceholder`:
	```xml
	<AnswerPlaceholder>
	  <option name="index" value="0" />
	  <option name="initialState">
	    <MyInitialState>
	      <option name="length" value="6" />
	      <option name="offset" value="22" />
	    </MyInitialState>
	  </option>
	  <option name="length" value="6" />
	  <option name="offset" value="22" />
	  <option name="selected" value="false" />
	  <option name="subtaskInfos">
	    <map>
	      <entry key="0">
	        <value>
	          <AnswerPlaceholderSubtaskInfo>
	            <option name="answer" value="" />
	            <option name="hasFrame" value="true" />
	            <option name="hints">
	              <list>
	                <option value="" />
	              </list>
	            </option>
	            <option name="needInsertText" value="false" />
	            <option name="placeholderText" value="TODO()" />
	            <option name="possibleAnswer" value="&quot;OK&quot;" />
	            <option name="selected" value="false" />
	            <option name="status" value="Unchecked" />
	          </AnswerPlaceholderSubtaskInfo>
	        </value>
	      </entry>
	    </map>
	  </option>
	  <option name="useLength" value="true" />
	</AnswerPlaceholder>
	```

	Current format of `AnswerPlaceholder`:
	```xml
	<AnswerPlaceholder>
	  <option name="index" value="0" />
	  <option name="initialState">
	    <MyInitialState>
	      <option name="length" value="6" />
	      <option name="offset" value="22" />
	    </MyInitialState>
	  </option>
	  <option name="length" value="6" />
	  <option name="offset" value="22" />
	  <option name="selected" value="false" />
	  <option name="useLength" value="true" />
	  <option name="placeholderText" value="TODO()" />
	  <option name="possibleAnswer" value="&quot;OK&quot;" />
	  <option name="status" value="TODO()" />
	  <option name="hints">
	    <list>
	      <option value="" />
	    </list>
	  </option>
	</AnswerPlaceholder>
	```

### JSON format version

4.  Skipped. 

    Name of additional materials was changed from `PyCharm additional materials` to `Edu additional materials`.
    We created the corresponding migration but only for local json representation (for Stepik we convert it into `PyCharm additional materials` again).
    So version wasn't really changed. Skip it to keep migration code in consistency.
    
5.  Drop `AnswerPlaceholderSubtaskInfo` and move all info into `AnswerPlaceholder` object.

    Previous format of `AnswerPlaceholder` in task object:
    ```json
	{
	  "offset": 1,
	  "length": 10,
	  "subtask_infos": [
	    {
	      "hints": [
	        "hint 1",
	        "hint 2"
	      ],
	      "possible_answer": "answer1",
	      "placeholder_text": "type here",
	      "has_frame": true,
	      "need_insert_text": false,
	      "index": 0
	    }
	  ]
	}
    ```
    or in submission reply object:
    ```json
	{
	  "offset": 1,
	  "length": 10,
	  "subtask_infos": {
	    "0": {
	      "hints": [
	        "hint 1",
	        "hint 2"
	      ],
	      "possible_answer": "answer1",
	      "placeholder_text": "type here",
	      "has_frame": true,
	      "need_insert_text": false,
	      "selected": false,
	      "status": "Solved"
	    }
	  }
	}
    ```

    Current format of `AnswerPlaceholder`:
    ```json
	{
	  "offset": 1,
	  "length": 10,
	  "hints": [
	    "hint 1",
	    "hint 2"
	  ],
	  "possible_answer": "answer1",
	  "placeholder_text": "type here",
	  "selected": false,
	  "status": "Solved"
	}
    ```
