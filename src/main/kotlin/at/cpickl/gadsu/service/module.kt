package at.cpickl.gadsu.service

import com.google.inject.AbstractModule
import com.google.inject.Scopes

class ServiceModule() : AbstractModule() {
    override fun configure() {

        bind(Clock::class.java).to(RealClock::class.java).`in`(Scopes.SINGLETON)

        bind(IdGenerator::class.java).to(UuidGenerator::class.java).`in`(Scopes.SINGLETON)

        bind(MetaInf::class.java).toProvider(MetaInfLoader::class.java).`in`(Scopes.SINGLETON)

        bind(WebPageOpener::class.java).to(SwingWebPageOpener::class.java).asEagerSingleton()

        install(CurrentModule())
    }
}
