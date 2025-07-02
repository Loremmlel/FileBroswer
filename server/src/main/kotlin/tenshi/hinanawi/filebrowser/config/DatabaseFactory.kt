package tenshi.hinanawi.filebrowser.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import tenshi.hinanawi.filebrowser.table.FavoriteFiles
import tenshi.hinanawi.filebrowser.table.Favorites

object DatabaseFactory {
  fun init() {
    Database.connect(createHikariDatasource())

    transaction {
      SchemaUtils.create(Favorites)
      SchemaUtils.create(FavoriteFiles)
    }
  }

  private fun createHikariDatasource() = HikariDataSource(HikariConfig().apply {
    driverClassName = AppConfig.Database.driver
    jdbcUrl = AppConfig.Database.url
    username = AppConfig.Database.user
    password = AppConfig.Database.password
    maximumPoolSize = AppConfig.Database.maxPoolSize
    minimumIdle = AppConfig.Database.minPoolSize
    isAutoCommit = false
    transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    validate()
  })
}