package com.tolmol.platform.infra.config

import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.support.JdbcTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.stereotype.Component
import jakarta.annotation.PreDestroy

class Database(val pool: HikariDataSource) {
    val sql = JdbcClient.create(pool)
    val transactions = TransactionTemplate(JdbcTransactionManager(pool))
}

@Component
class Databases(env: Environment) {
    private fun open(name: String, env: Environment): Database {
        val pool = HikariDataSource().apply {
            jdbcUrl = env.getProperty("tolmol.db.base", "jdbc:postgresql://localhost:15432/") + name
            username = env.getProperty("tolmol.db.user", "tolmol")
            password = env.getRequiredProperty("tolmol.db.password")
            maximumPoolSize = 6
        }
        Flyway.configure().dataSource(pool).locations("classpath:db/$name").load().migrate()
        return Database(pool)
    }
    val users = open("users", env)
    val location = open("location", env)
    val marketplace = open("marketplace", env)
    val chat = open("chat", env)
    @PreDestroy fun close() = listOf(users, location, marketplace, chat).forEach { it.pool.close() }
}
