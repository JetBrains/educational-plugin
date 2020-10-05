1. Here's what Marketplace expects from our `course.zip/course.json`:

    ```json
    {
      "properties": {
        "environment": {
          "description": "Additional information about course environment. For example, Kotlin courses are supported for Android environment",
          "type": [
            "string",
            "null"
          ]
        },
        "summary": {
          "description": "Summary describing what the course is about",
          "type": "string"
        },
        "title": {
          "description": "Title of the course",
          "type": "string"
        },
        "programming_language": {
          "description": "Programming language of the course",
          "type": "string"
        },
        "language": {
          "description": "Language code used for course materials",
          "type": "string",
          "examples": [
            "en",
            "ru"
          ]
        },
        "edu_plugin_version": {
          "description": "EduTools plugin version. Used to calculate compatibility on the marketplace.",
          "type": "string",
          "examples": [
            "3.7-2019.3-5266"
          ]
        },
        "vendor": {
          "description": "Vendor name, email and url information",
          "type": [
            "object",
            "null"
          ],
          "properties": {
            "name": {
              "type": "string",
              "description": "Vendor name"
            },
            "url": {
              "type": "string",
              "description": "Vendor url"
            },
            "email": {
              "type": "string",
              "description": "Vendor email"
            }
          },
          "required": [
            "name"
          ],
          "additionalProperties": false
        },
        "course_type": {
          "description": "Course type",
          "type": "string",
          "enum": [
            "edu",
            "coursera"
          ]
        },
        "items": {
          "description": "Lessons or sections included in the course",
          "type": [
            "array"
          ],
          "uniqueItems": true
        }
      },
      "required": [
        "title",
        "summary",
        "items",
        "vendor",
        "language",
        "programming_language",
        "edu_plugin_version"
      ]
    }
    ```

2. Marketplace course archive could contain `courseIcon.svg` icon in the archive root. This image will be shown on the plugin page.