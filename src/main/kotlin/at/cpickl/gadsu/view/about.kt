package at.cpickl.gadsu.view

import at.cpickl.gadsu.QuitEvent
import at.cpickl.gadsu.UserEvent
import at.cpickl.gadsu.service.Logged
import at.cpickl.gadsu.service.MetaInf
import at.cpickl.gadsu.service.OpenWebpageEvent
import at.cpickl.gadsu.service.formatDateTime
import at.cpickl.gadsu.view.components.GridPanel
import at.cpickl.gadsu.view.components.HtmlEditorPane
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.AbstractModule
import com.google.inject.Scopes
import org.joda.time.DateTime
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.Insets
import javax.inject.Inject
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel

fun main(args: Array<String>) {
    AboutWindow(MetaInf("1.0.0", DateTime.now()), null, EventBus()).isVisible = true
}

class ShowAboutDialogEvent : UserEvent() {}

class AboutModule : AbstractModule() {
    override fun configure() {
        bind(AboutController::class.java).asEagerSingleton()
        bind(AboutWindow::class.java).`in`(Scopes.SINGLETON)
    }
}


@Logged
open class AboutController @Inject constructor(
        private val window: AboutWindow
) {

    @Subscribe open fun onAbout(@Suppress("UNUSED_PARAMETER") event: ShowAboutDialogEvent) {
        window.setVisible(true)
    }

    @Subscribe open fun onQuit(@Suppress("UNUSED_PARAMETER") event: QuitEvent) {
        window.setVisible(false)
        window.dispose()
    }
}


class AboutWindow @Inject constructor(
        metaInf: MetaInf,
        mainFrame: MainFrame?,
        bus: EventBus
) : JFrame() {
    init {
        title = ""
        // https://developer.apple.com/library/mac/technotes/tn2007/tn2196.html#//apple_ref/doc/uid/DTS10004439
        rootPane.putClientProperty("Window.style", "small")

        val panel = GridPanel()
        panel.border = BorderFactory.createEmptyBorder(10, 40, 10, 40)
        panel.c.anchor = GridBagConstraints.CENTER
        panel.c.insets = Insets(0, 0, 10, 0)

        panel.add(JLabel(ImageIcon(javaClass.getResource("/gadsu/logo100.png"))))
        panel.c.gridy++
        val title = JLabel("Gadsu")
        title.font = title.font.deriveFont(17.0F).deriveFont(Font.BOLD)
        panel.add(title)
        panel.c.gridy++
        val aboutText = HtmlEditorPane()
        aboutText.changeLabelFont(10.0F)
        aboutText.text =
                "<div style='text-align:center;'>" + //font-family:${title.font.fontName};font-weight:normal;font-size:10pt'>" +
                "Version ${metaInf.applicationVersion}<br>" +
                "(${metaInf.built.formatDateTime()})<br>" +
                "By Christoph Pickl<br>" +
                "<br>" +
                """Visit the <a href="https://github.com/christophpickl/gadsu">Website</a>"""
        aboutText.addOnUrlClickListener { bus.post(OpenWebpageEvent(it)) }

        panel.add(aboutText)


        contentPane.layout = BorderLayout()
        contentPane.add(panel, BorderLayout.CENTER)
        pack()
        isResizable = false
        setLocationRelativeTo(mainFrame?.asJFrame())
    }
}
