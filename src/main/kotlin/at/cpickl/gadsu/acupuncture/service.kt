package at.cpickl.gadsu.acupuncture

import at.cpickl.gadsu.service.SearchableService
import at.cpickl.gadsu.acupuncture.Acupunct
import at.cpickl.gadsu.acupuncture.AcupunctureRepository
import org.slf4j.LoggerFactory
import javax.inject.Inject


interface AcupunctureService : SearchableService<Acupunct> {
    // all via super interface or events
}


class AcupunctureServiceImpl @Inject constructor (
        // needs better DI usage: private val data: List<Acupunct>
        repository: AcupunctureRepository
) : AcupunctureService {
    private val log = LoggerFactory.getLogger(javaClass)

    private val data: List<Acupunct> = repository.loadAll() // eager loading

    override fun all() = data

    override fun find(searchTerm: String): List<Acupunct> {
        log.debug("find(searchTerm='{}')", searchTerm)
        val cleanedSearchTerm = searchTerm.trim().toLowerCase()
        if (cleanedSearchTerm.isEmpty()) {
            return emptyList()
        }

        return data.filter {
            log.trace("  find acupunct: {}", it)

            val matched = it.indications.map { it.toLowerCase() }.find {
                log.trace("    checking indication (lower cased): {}", it)
                it.contains(cleanedSearchTerm)
            } != null
            log.trace("  acupunct matched: {}", matched)

            matched
        }
    }

}
