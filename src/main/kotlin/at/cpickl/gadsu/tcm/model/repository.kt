package at.cpickl.gadsu.tcm.model

// for better testability
interface AcupunctureRepository {
    fun loadAll(): List<Acupunct>
}

class StaticAcupunctureRepository : AcupunctureRepository {
    override fun loadAll(): List<Acupunct> = Acupuncts.allPuncts.value
}
