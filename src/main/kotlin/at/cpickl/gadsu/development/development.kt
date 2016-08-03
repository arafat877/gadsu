package at.cpickl.gadsu.development

import at.cpickl.gadsu.GadsuSystemProperty
import at.cpickl.gadsu.UserEvent
import at.cpickl.gadsu.view.GadsuMenuBar
import com.google.common.eventbus.EventBus
import javax.swing.JMenu
import javax.swing.JMenuItem


class Development {
    companion object {

        val ENABLED: Boolean = GadsuSystemProperty.development.isEnabledOrFalse()
        val COLOR_ENABLED = ENABLED && false

        init {
            if (ENABLED) {
                println("Development mode is enabled via '-D${GadsuSystemProperty.development.key}=true'")
            }
        }

        fun fiddleAroundWithMenuBar(menu: GadsuMenuBar, bus: EventBus) {
            if (!ENABLED) {
                return
            }
            val menuDevelopment = JMenu("Development")
            menu.add(menuDevelopment)


            addItemTo(menuDevelopment, "Development Window", ShowDevWindowEvent(), bus)
            menuDevelopment.addSeparator()
            addItemTo(menuDevelopment, "Reset Data", DevelopmentResetDataEvent(), bus)
            addItemTo(menuDevelopment, "Reset Screenshot Data", DevelopmentResetScreenshotDataEvent(), bus)
            addItemTo(menuDevelopment, "Clear Data", DevelopmentClearDataEvent(), bus)
        }

        private fun addItemTo(menu: JMenu, label: String, event: UserEvent, bus: EventBus) {
            val item = JMenuItem(label)
            item.addActionListener { bus.post(event) }
            menu.add(item)
        }

    }

}

