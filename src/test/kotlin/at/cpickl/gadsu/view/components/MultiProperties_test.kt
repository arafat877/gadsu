package at.cpickl.gadsu.view.components

import at.cpickl.gadsu.client.xprops.view.XPropCellRenderer
import at.cpickl.gadsu.tcm.model.XProps
import at.cpickl.gadsu.testinfra.TestViewStarter
import at.cpickl.gadsu.testinfra.ui.SimpleUiTest
import at.cpickl.gadsu.view.ViewNames
import com.google.common.eventbus.EventBus
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.testng.annotations.Test
import org.uispec4j.*
import org.uispec4j.interception.MainClassAdapter
import java.awt.BorderLayout
import javax.swing.*


@Test
class MultiPropertiesTest {

    private val bullet = "*"
    private val noteHeader = "[NOTIZ]"
    private val value1 = "val1"
    private val values1 = listOf(value1)
    private val note = "test note"

    fun `formatData enum opts and note`() {
        assertThat(MultiProperties.buildRenderText(values1, note),
                equalTo("$bullet $value1\n\n$noteHeader\n$note"))
    }

    fun `formatData enum opts only`() {
        assertThat(MultiProperties.buildRenderText(values1, ""),
                equalTo("$bullet $value1"))
    }

    fun `formatData note only`() {
        assertThat(MultiProperties.buildRenderText(emptyList(), note),
                equalTo("$noteHeader\n$note"))
    }

}

@Test(groups = arrayOf("uiTest"))
class MultiPropertiesUiTest : SimpleUiTest() {

    private val container = JPanel(BorderLayout())
    private lateinit var driver: MultiPropertiesDriver

    override fun postInit(window: Window) {
        driver = MultiPropertiesDriver(this, window)
        container.add(driver.component, BorderLayout.CENTER)
    }

    override fun newMainClassAdapter(): MainClassAdapter {
        TestViewStarter.componentToShow = container
        return MainClassAdapter(TestViewStarter::class.java)
    }

    fun `change note and values`() {
        val myNote = "my note"
        val renderTcmlabel = XProps.SleepOpts.NeedLess.opt.label

        driver.assertRenderText("")

        driver.hitEditButton()
        driver.clickListItem(0)
        driver.hitDoneButton()
        driver.assertRenderText("* $renderTcmlabel")

        driver.hitEditButton()
        driver.enterNote(myNote)
        driver.hitDoneButton()
        driver.assertRenderText("* $renderTcmlabel\n\n[NOTIZ]\n$myNote")

        driver.hitEditButton()
        driver.clickListItem(0)
        driver.hitDoneButton()
        driver.assertRenderText("[NOTIZ]\n$myNote")

    }

}

private class MultiPropertiesDriver(
        private val test: UISpecTestCase,
        private val window: Window
) {

    val component: JComponent

    private val viewNameId = "mpTest"

    private val viewNameRenderText = ViewNames.Components.MultiProperties.RenderText(viewNameId)
    private val renderText: TextBox get() = window.getTextBox(viewNameRenderText)

    private val viewNameInputList = ViewNames.Components.MultiProperties.InputList(viewNameId)
    private val inputList: ListBox get() = window.getListBox(viewNameInputList)
    private val viewNameInputNote = ViewNames.Components.MultiProperties.InputNote(viewNameId)
    private val inputNote: TextBox get() = window.getTextBox(viewNameInputNote)
    private val viewNameButtonDone = ViewNames.Components.MultiProperties.ButtonDone(viewNameId)
    private val buttonDone: Button get() = window.getButton(viewNameButtonDone)


    init {
        val xprop = XProps.Sleep
        val bus = EventBus()
        val testee = MultiProperties(xprop.options, bus, XPropCellRenderer, viewNameId, { it.map { it.label } }, true)
        component = testee.toComponent()
    }

    fun clickListItem(rowToClick: Int) {
        inputList.click(rowToClick)
    }

    fun enterNote(note: String) {
        inputNote.setText(note, false)
    }

    fun hitEditButton() {
        assertRenderMode()
        Mouse.click(renderText)
    }

    fun hitDoneButton() {
        assertEditMode()
        buttonDone.click()
    }

    private fun assertRenderMode() {
        test.assertThat(renderText.isVisible)

        test.not(window.containsSwingComponent(JList::class.java, viewNameInputList))
        test.not(window.containsSwingComponent(JTextArea::class.java, viewNameInputNote))
        test.not(window.containsSwingComponent(JButton::class.java, viewNameButtonDone))
    }

    private fun assertEditMode() {
        test.not(window.containsSwingComponent(JTextArea::class.java, viewNameRenderText))

        test.assertThat(inputList.isVisible)
        test.assertThat(inputNote.isVisible)
        test.assertThat(buttonDone.isVisible)
    }

    fun assertRenderText(expected: String) {
        test.assertThat(renderText.textEquals(expected))
    }

}
