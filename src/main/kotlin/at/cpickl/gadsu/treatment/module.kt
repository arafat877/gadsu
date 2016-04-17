package at.cpickl.gadsu.treatment

import at.cpickl.gadsu.client.Client
import at.cpickl.gadsu.treatment.inclient.TreatmentsInClientController
import at.cpickl.gadsu.treatment.inclient.TreatmentsInClientView
import at.cpickl.gadsu.treatment.view.SwingTreatmentView
import at.cpickl.gadsu.treatment.view.TreatmentController
import at.cpickl.gadsu.treatment.view.TreatmentView
import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder


class TreatmentModule : AbstractModule() {
    override fun configure() {

        bind(TreatmentRepository::class.java).to(TreatmentSpringJdbcRepository::class.java).asEagerSingleton()

        // the table which is located in the client view
        bind(TreatmentsInClientView::class.java).asEagerSingleton()
        bind(TreatmentsInClientController::class.java).asEagerSingleton()

        // view will be created dynamically based on current client
        install(FactoryModuleBuilder()
                .implement(TreatmentView::class.java, SwingTreatmentView::class.java)
                .build(TreatmentViewFactory::class.java))

        bind(TreatmentController::class.java).asEagerSingleton()
        bind(TreatmentService::class.java).to(TreatmentServiceImpl::class.java).asEagerSingleton()
    }
}

interface TreatmentViewFactory {
    fun create(client: Client, treatment: Treatment): TreatmentView
}
