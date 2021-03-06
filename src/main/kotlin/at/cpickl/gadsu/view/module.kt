package at.cpickl.gadsu.view

import at.cpickl.gadsu.global.GadsuSystemProperty
import com.google.inject.AbstractModule
import org.slf4j.LoggerFactory

class ViewModule : AbstractModule() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun configure() {
        log.debug("configure()")


        bind(SwingFactory::class.java)

        // main window
        bind(MainFrame::class.java).to(SwingMainFrame::class.java).asEagerSingleton()
        bind(MainFrameController::class.java).asEagerSingleton()

        // mac handling
        val isMacApp = GadsuSystemProperty.isMacApp.isEnabledOrFalse()
        log.debug("isMacApp={}", isMacApp)
        bind(MacHandler::class.java).toInstance(if (isMacApp) ReflectiveMacHandler() else DisabledMacHandler() )

        // menu bar
        bind(GadsuMenuBar::class.java).asEagerSingleton()
        bind(GadsuMenuBarController::class.java).asEagerSingleton()

        // about
        install(AboutModule())

        // async
        bind(AsyncWorker::class.java).to(AsyncSwingWorker::class.java)
    }

}
