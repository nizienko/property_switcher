# Property Switcher

The Property Switcher Plugin is a lightweight IntelliJ IDEA plugin that streamlines working with local property files. 
It offers convenient shortcuts for quickly switching property values, making it ideal for tasks like running tests locally. 
Additionally, the plugin provides a customizable status bar widget that displays the values of selected properties. 
Stay informed about important property values at a glance with the Property Switcher Plugin's intuitive widget. 
Simplify your property file management, boost productivity, and effortlessly replace property values with ease.

## Installation

* Open IntelliJ IDEA.
* Go to File -> Settings -> Plugins.
* Click on the Browse repositories... button.
* Search for "Property Switcher" in the plugin repository.
* Click the Install button next to the "Property Switcher" plugin.
* Restart IntelliJ IDEA to activate the plugin.

## Usage

Create a new file with the `.propswitch` extension in your project.
Define the properties and their options in the JSON format. Here's an example:

```json
{
  "propertyFile": "local.properties",
  "properties": [
    {
      "name": "url",
      "options": [
        "http://112.88.44.13",
        "http://112.88.46.33",
        "http://112.88.46.63"
      ],
      "showInStatusBar": true
    },
    {
      "name": "theme",
      "options": [
        "light",
        "dark"
      ],
      "showInStatusBar": true
    }
  ]
}
```

| Property        | Description                                                                                           |
|-----------------|-------------------------------------------------------------------------------------------------------|
| propertyFile    | Specify the name of the property file where the properties will be modified (e.g., local.properties). |
| properties      | Define an array of properties.                                                                        |
| name            | Specify the name of the property.                                                                     |
| options         | Provide an array of available options for the property.                                               |
| showInStatusBar | Set this to true if you want the property value to be displayed in the status bar widget. [optional]  |

 - Save the JSON specification file with 'Reload Property Switcher' in the editor context menu(mouse right click inside the editor)

 - Press `Shift + Command + 1` (or equivalent) to invoke the Property Switcher Plugin from anywhere in IntelliJ IDEA.
 - In the popup menu, select the desired property from the available options.
 - Choose the new value for the selected property.
 - Press Enter to replace the original property value with the chosen value in the respective property file.

If you've configured the status bar widget, current values of the specified properties will be displayed in the
status bar.

## License

This plugin is licensed under the [MIT License](https://opensource.org/licenses/MIT).