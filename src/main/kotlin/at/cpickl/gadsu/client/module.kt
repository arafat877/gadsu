package at.cpickl.gadsu.client

import at.cpickl.gadsu.client.view.ClientDetailView
import at.cpickl.gadsu.client.view.ClientMasterView
import at.cpickl.gadsu.client.view.ClientView
import at.cpickl.gadsu.client.view.ClientViewController
import at.cpickl.gadsu.client.view.SwingClientDetailView
import at.cpickl.gadsu.client.view.SwingClientMasterView
import at.cpickl.gadsu.client.view.SwingClientView
import com.google.inject.AbstractModule
import com.google.inject.Scopes
import org.slf4j.LoggerFactory

class ClientModule : AbstractModule() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun configure() {
        log.debug("configure()")
        bind(ClientRepository::class.java).to(ClientSpringJdbcRepository::class.java)

        bind(ClientViewController::class.java).asEagerSingleton()
        bind(ClientView::class.java).to(SwingClientView::class.java).`in`(Scopes.SINGLETON)
        bind(ClientMasterView::class.java).to(SwingClientMasterView::class.java).`in`(Scopes.SINGLETON)
        bind(ClientDetailView::class.java).to(SwingClientDetailView::class.java).`in`(Scopes.SINGLETON)

        bind(ClientService::class.java).to(ClientServiceImpl::class.java)
    }

}
