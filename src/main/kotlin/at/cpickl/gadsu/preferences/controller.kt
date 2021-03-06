package at.cpickl.gadsu.preferences

import at.cpickl.gadsu.global.GADSU_DIRECTORY
import at.cpickl.gadsu.global.QuitEvent
import at.cpickl.gadsu.persistence.BackupController
import at.cpickl.gadsu.preferences.view.PreferencesWindow
import at.cpickl.gadsu.service.InternetConnectionEstablishedEvent
import at.cpickl.gadsu.service.InternetConnectionLostEvent
import at.cpickl.gadsu.service.LOG
import at.cpickl.gadsu.service.Logged
import at.cpickl.gadsu.service.formatDateTimeLong
import com.google.common.eventbus.Subscribe
import javax.inject.Inject

@Logged
open class PreferencesController @Inject constructor(
        private val window: PreferencesWindow,
        private val prefs: Prefs,
        private val backupController: BackupController
) {

    private val log = LOG(javaClass)

    init {
        // could happen, that app is going to quit, without the prefs window has been shown => ensure the window (read data) got properly initialized
        window.initData(prefs.preferencesData)
    }

    @Subscribe open fun onShowPreferencesEvent(@Suppress("UNUSED_PARAMETER") event: ShowPreferencesEvent) {
        window.initData(prefs.preferencesData)
        window.start()
        window.txtApplicationDirectory = GADSU_DIRECTORY.absolutePath
        window.txtLatestBackup = backupController.findLatestBackup()?.date?.formatDateTimeLong() ?: "N/A"
    }

    @Subscribe open fun onPreferencesWindowClosedEvent(event: PreferencesWindowClosedEvent) {
        if (event.persistData) {
            prefs.preferencesData = window.readData()
        } else {
            log.trace("Not persisting preferences data (user closed via X button in window title, instead of hitting the close button.)")
        }
    }

    @Subscribe open fun onInternetConnectionLostEvent(event: InternetConnectionLostEvent) {
        window.btnCheckUpdate.isEnabled = false
    }

    @Subscribe open fun onInternetConnectionEstablishedEvent(event: InternetConnectionEstablishedEvent) {
        window.btnCheckUpdate.isEnabled = true
    }

    @Subscribe open fun onQuitEvent(@Suppress("UNUSED_PARAMETER") event: QuitEvent) {
        prefs.preferencesData = window.readData() // store data back (again?! seems so...)
        window.closeWindow()
    }
}
