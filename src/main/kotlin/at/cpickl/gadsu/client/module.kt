package at.cpickl.gadsu.client

import at.cpickl.gadsu.client.view.ClientMasterView
import at.cpickl.gadsu.client.view.ClientView
import at.cpickl.gadsu.client.view.ClientViewController
import at.cpickl.gadsu.client.view.SwingClientMasterView
import at.cpickl.gadsu.client.view.SwingClientView
import at.cpickl.gadsu.client.view.ThresholdCalculator
import at.cpickl.gadsu.client.view.ThresholdCalculatorImpl
import at.cpickl.gadsu.client.view.detail.ClientDetailView
import at.cpickl.gadsu.client.view.detail.SwingClientDetailView
import at.cpickl.gadsu.client.xprops.XPropsModule
import com.google.inject.AbstractModule
import com.google.inject.Scopes
import org.slf4j.LoggerFactory

class ClientModule : AbstractModule() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun configure() {
        log.debug("configure()")
        bind(ClientRepository::class.java).to(ClientJdbcRepository::class.java)

        bind(ClientViewController::class.java).asEagerSingleton()
        bind(ClientView::class.java).to(SwingClientView::class.java).`in`(Scopes.SINGLETON)
        bind(ClientMasterView::class.java).to(SwingClientMasterView::class.java).`in`(Scopes.SINGLETON)
        bind(ClientDetailView::class.java).to(SwingClientDetailView::class.java).`in`(Scopes.SINGLETON)
        bind(ThresholdCalculator::class.java).to(ThresholdCalculatorImpl::class.java).`in`(Scopes.SINGLETON)

        bind(ClientService::class.java).to(ClientServiceImpl::class.java)
        bind(ClientSearchController::class.java).asEagerSingleton()

        install(XPropsModule())
    }

}
