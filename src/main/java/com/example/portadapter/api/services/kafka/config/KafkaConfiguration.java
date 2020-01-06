package com.example.portadapter.api.services.kafka.config;

import com.example.portadapter.api.services.kafka.model.PaymentSessionKafka;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.adapter.FilteringBatchMessageListenerAdapter;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;

import java.time.Duration;
import java.util.Objects;

@Configuration
public class KafkaConfiguration {

    @Value("${spring.cloud.stream.bindings.input.destination}")
    private String requestTopicName;

    @Bean
    public ReplyingKafkaTemplate<String, PaymentSessionKafka, PaymentSessionKafka> replyingTemplate(
            ProducerFactory<String, PaymentSessionKafka> producerFactory,
            ConcurrentMessageListenerContainer<String, PaymentSessionKafka> repliesContainer) {
        var replayingKafkaTemplate = new ReplyingKafkaTemplate<>(producerFactory, repliesContainer);
        replayingKafkaTemplate.setSharedReplyTopic(true);
       // addUnrecognizableEventsFilter(repliesContainer.getContainerProperties(), replayingKafkaTemplate);
        replayingKafkaTemplate.setDefaultReplyTimeout(Duration.ofSeconds(15));
        return replayingKafkaTemplate;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, PaymentSessionKafka> repliesContainer(
            ConcurrentKafkaListenerContainerFactory<String, PaymentSessionKafka> containerFactory) {
        ConcurrentMessageListenerContainer<String, PaymentSessionKafka> repliesContainer =
                containerFactory.createContainer(requestTopicName);
        repliesContainer.getContainerProperties().setGroupId("out-topic");
        repliesContainer.setAutoStartup(false);
        return repliesContainer;
    }

}
