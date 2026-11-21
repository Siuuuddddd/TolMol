package com.tolmol
import com.fasterxml.jackson.databind.JsonNode
import com.tolmol.user.domain.model.Role
import com.tolmol.platform.infra.config.Databases
import org.springframework.beans.factory.annotation.Autowired
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.*

class DiscoveryStreamTest : SocketFixture() {
    @Autowired lateinit var db: Databases
    @Test fun liveDiscoveryUsesCurrentLocationForEachEvent() {
        val users = listOf(user(Role.BUYER),user(Role.BUYER,22.0),user(Role.BUYER))
        val queues = users.map { LinkedBlockingQueue<JsonNode>() }
        val sockets = users.mapIndexed { i,id -> connect(id,queues[i]) }
        try {
            sockets.forEachIndexed { i,s ->
                s.sendText("""{"type":"nearby.subscribe"}""",true).join()
                assertEquals("ack",queues[i].poll(10,TimeUnit.SECONDS)["type"].asText())
            }
            db.location.sql.sql("update positions set updated_at=now()-interval '16 minutes' where user_id=?").param(users.last()).update()
            val listing = lot(user(Role.SELLER)).id
            org.awaitility.Awaitility.await().atMost(15,TimeUnit.SECONDS).until { queues.first().any { it.path("aggregate").asText()==listing.toString() } }
            val event = queues.first().first { it.path("aggregate").asText()==listing.toString() }
            assertEquals("listing.available",event["type"].asText())
            assertFalse(event.toString().contains("latitude",ignoreCase=true))
            queues.drop(1).forEach { q -> assertFalse(q.any { it.path("aggregate").asText()==listing.toString() }) }
        } finally { sockets.forEach { it.abort() } }
    }
}
