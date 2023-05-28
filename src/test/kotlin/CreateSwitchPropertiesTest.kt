import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.component
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.steps.CommonSteps
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import org.junit.jupiter.api.Test
import java.time.Duration

class CreateSwitchPropertiesTest {
    private val robot = RemoteRobot("http://127.0.0.1:8082")
    private val steps = CommonSteps(robot)

    @Test
    fun createPropSwitchFileTest() {
        step("Create project") {
            robot.welcomeFrame {
                component(byXpath("//div[(@accessiblename='New Project' and @class='MainButton') or @defaulticon='createNewProjectTab.svg']"))
                    .click()
                component(byXpath("//div[@class='ProjectTypeListWithSearch']"))
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

                step("Switch properties") {
                    widget().click()
                    popup().findText("property_1").moveMouse()
                    popup().popup().findText("value_1").click()
                }

                assert(
                    textEditor().editor.text == """
                    property_1=value_1
                    property_2=value_3
                    
                """.trimIndent()
                )

                step("Switch properties") {
                    widget().click()
                    popup().findText("property_2").moveMouse()
                    popup().popup().findText("value_2").click()
                }

                assert(
                    textEditor().editor.text == """
                    property_1=value_1
                    property_2=value_2
                    
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