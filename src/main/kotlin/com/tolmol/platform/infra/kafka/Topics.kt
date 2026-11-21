package com.tolmol.platform.infra.kafka

import org.springframework.context.annotation.*
import org.springframework.kafka.config.TopicBuilder
@Configuration
class Topics {
    @Bean fun auctionTopic() = TopicBuilder.name("auction.events").partitions(1).replicas(1).build()
    @Bean fun messagesTopic() = TopicBuilder.name("chat.message").partitions(1).replicas(1).build()
}
