package tenshi.hinanawi.filebrowser.service

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.BeforeClass
import tenshi.hinanawi.filebrowser.table.FavoriteFileTable
import tenshi.hinanawi.filebrowser.table.FavoriteTable
import javax.sql.DataSource
import kotlin.test.AfterTest

abstract class BaseServiceTest {

  companion object {
    private lateinit var testDatabase: Database
    private lateinit var dataSource: DataSource

    @BeforeClass
    @JvmStatic
    fun setupDatabase() {
      val config = HikariConfig().apply {
        driverClassName = "org.h2.Driver"
        jdbcUrl = "jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        username = "sa"
        password = ""
        maximumPoolSize = 5
        minimumIdle = 1
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
      }
      dataSource = HikariDataSource(config)
      testDatabase = Database.connect(dataSource)

      transaction {
        SchemaUtils.create(FavoriteTable, FavoriteFileTable)
      }
    }
  }

  @AfterTest
  fun tearDown() {
    transaction {
      FavoriteTable.deleteAll()
      FavoriteFileTable.deleteAll()
    }
  }
}