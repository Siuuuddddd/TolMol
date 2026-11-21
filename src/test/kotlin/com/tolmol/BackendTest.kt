package com.tolmol

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

@SpringBootTest
abstract class BackendTest {
    companion object {
        private val redis = org.testcontainers.containers.GenericContainer("redis:7").apply { withExposedPorts(6379); start() }
        private val kafka = org.testcontainers.kafka.KafkaContainer("apache/kafka:4.0.0").apply { start() }
        private val pg = PostgreSQLContainer("postgres:17-alpine").apply {
            withDatabaseName("marketplace")
            start()
            for (name in listOf("users","chat","location")) {
                check(execInContainer("psql","-U",username,"-d",databaseName,"-c","create database $name").exitCode == 0L.toInt())
            }
        }
        @JvmStatic @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) {
            registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
            registry.add("spring.data.redis.port") { redis.firstMappedPort }
            registry.add("tolmol.db.base") { "jdbc:postgresql://${pg.host}:${pg.firstMappedPort}/" }
            registry.add("tolmol.db.user") { pg.username }
            registry.add("tolmol.db.password") { pg.password }
            registry.add("tolmol.jwt.secret") { "integration-test-signing-key-32-bytes" }
        }
    }
}
