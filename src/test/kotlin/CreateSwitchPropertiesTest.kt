import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.steps.CommonSteps
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import com.intellij.remoterobot.utils.waitForIgnoringError
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Duration
import kotlin.concurrent.thread

class CreateSwitchPropertiesTest {
    companion object {
        private val robot = RemoteRobot("http://127.0.0.1:8082")
        private val steps = CommonSteps(robot)

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            robot.runIde()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            steps.invokeAction("Exit")
            robot.idea {
                find<ContainerFixture>(byXpath("//div[@class='MyDialog']"))
                    .find<JButtonFixture>(byXpath("//div[@text='Exit']"))
                    .click()
            }
        }
    }

    @Test
    fun createPropSwitchFileTest() {
        step("Create project") {
            robot.welcomeFrame {
                component(byXpath("//div[(@accessiblename='New Project' and @class='MainButton') or @defaulticon='createNewProjectTab.svg']"))
                    .click()
                component(byXpath("//div[@class='ProjectTypeListWithSearch']"), Duration.ofSeconds(15))
                    .findText("New Project")
                    .click()
                component(byXpath("//div[contains(@action, 'Kotlin') and @class='SegmentedButton']"))
                    .click()
                component(byXpath("//div[@text='Create']"))
                    .click()
            }
        }
        step("Create File") {
            robot.idea {
                step("Wait for smart mode 5 minutes") {
                    steps.waitForSmartMode(5)
                }
                step("Invoke actionId 'NewElement'") {
                    steps.invokeAction("NewElement")
                }
                popup().findText("Property Switcher")
                    .click()
                waitFor { popup().hasText("New Property Switcher") }
                keyboard {
                    enterText("local")
                    enter()
                }
                component(byXpath("//div[@class='EditorComponentImpl']"))
                    .rightClick()
                popup().findText("Reload Property Switcher")
                    .click()

                waitFor { widget().hasText("value_3") }

                step("Open local.properties") {
                    steps.invokeAction("GotoFile")
                    keyboard {
                        enterText("local.properties")
                        enter()
                    }
                }

                assert(
                    textEditor().editor.text == """
                    property_1=value_3
                    property_2=value_3
                    
                """.trimIndent()
                )
                step("Add third parameter") {
                    textEditor().editor.clickOnOffset(textEditor().editor.text.length)
                    keyboard {
                        backspace()
                        enterText(" # comment")
                        enter()
                        enterText("property_3=value_1")
                        enter()
                    }
                }

                step("Switch properties") {
                    widget().click()
                    popup().apply {
                        findText("property_1").moveMouse()
                        popup().findText("value_1").click()
                    }
                }

                assert(
                    textEditor().editor.text == """
                    property_1=value_1
                    property_2=value_3 # comment
                    property_3=value_1
                    
                """.trimIndent()
                )

                step("Switch properties") {
                    widget().click()
                    popup().apply {
                        findText("property_2").moveMouse()
                        popup().findText("value_2").click()
                    }
                }

                assert(
                    textEditor().editor.text == """
                    property_1=value_1
                    property_2=value_2 # comment
                    property_3=value_1
                    
                """.trimIndent()
                )
            }
        }
    }
}

private fun RemoteRobot.welcomeFrame(code: CommonContainerFixture.() -> Unit): CommonContainerFixture {
    return find(CommonContainerFixture::class.java, byXpath("//div[@class='FlatWelcomeFrame']")).apply(code)
}


private fun RemoteRobot.idea(code: CommonContainerFixture.() -> Unit): CommonContainerFixture {
    return find(
        CommonContainerFixture::class.java,
        byXpath("//div[@class='IdeFrameImpl']"),
        Duration.ofSeconds(30)
    ).apply(code)
}

private fun ContainerFixture.popup(): ContainerFixture {
    return find(ContainerFixture::class.java, byXpath("//div[@class='HeavyWeightWindow']"), Duration.ofSeconds(10))
}

private fun CommonContainerFixture.widget(): ComponentFixture =
    component(byXpath("//div[@class='IdeStatusBarImpl']//div[@class='TextPresentationComponent']"))

private fun RemoteRobot.runIde() {
    thread {
        val process = Runtime.getRuntime().exec("./gradlew :runIdeForUiTests")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            println(line)
        }
    }
    waitForIgnoringError(Duration.ofSeconds(120)) {
        findAll(CommonContainerFixture::class.java, byXpath("//div[@class='FlatWelcomeFrame']"))
            .size == 1
    }
}