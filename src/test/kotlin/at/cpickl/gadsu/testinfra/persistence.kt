package at.cpickl.gadsu.testinfra

import at.cpickl.gadsu.DatabaseManager
import at.cpickl.gadsu.JdbcX
import at.cpickl.gadsu.SpringJdbcX
import at.cpickl.gadsu.service.IdGenerator
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hsqldb.jdbc.JDBCDataSource
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.slf4j.LoggerFactory
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod


abstract class HsqldbTest {
    companion object {
        init {
            TestLogger().configureLog()
        }
    }

    private val log = LoggerFactory.getLogger(javaClass)
    private val allTables = arrayOf("treatment", "client")

    private var dataSource: EmbeddedDatabase? = null
    private var jdbcx: SpringJdbcX? = null

    protected var idGenerator: IdGenerator = mock(IdGenerator::class.java)

    @BeforeClass
    fun initDb() {
        val dataSource = JDBCDataSource()
        dataSource.url = "jdbc:hsqldb:mem:testDb${javaClass.simpleName}"
        dataSource.user = "SA"
        log.info("Using data source URL: ${dataSource.url}")

        DatabaseManager(dataSource).migrateDatabase()
//        arrayOf("V1__create_tables.sql").forEach {
//            ScriptUtils.executeSqlScript(dataSource!!.connection, ClassPathResource("/gadsu/persistence/$it"))
//        }

        jdbcx = SpringJdbcX(dataSource)
    }

    @BeforeMethod
    fun resetDb() {
        allTables.forEach { jdbcx!!.execute("DELETE FROM $it") }
    }

    @AfterClass
    fun shutdownDb() {
        dataSource?.shutdown()
    }

    protected fun jdbcx(): SpringJdbcX = jdbcx!!

    protected fun whenGenerateIdReturnTestUuid() {
        `when`(idGenerator.generate()).thenReturn(TEST_UUID)
    }

    protected fun nullJdbcx() = mock(JdbcX::class.java)

    protected fun assertEmptyTable(tableName: String) {
        assertThat("Expected table '$tableName' to be empty.", jdbcx().countTableEntries(tableName), equalTo(0))
    }

}

fun SpringJdbcX.countTableEntries(tableName: String): Int {
    var count: Int? = null
    jdbc.query("SELECT COUNT(*) AS cnt FROM $tableName") { rs -> count = rs.getInt("cnt") }
    return count!!
}
