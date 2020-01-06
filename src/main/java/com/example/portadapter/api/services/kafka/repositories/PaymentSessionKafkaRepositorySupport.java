package com.example.portadapter.api.services.kafka.repositories;


import com.example.portadapter.api.domain.model.PaymentStatus;
import com.example.portadapter.api.services.kafka.model.PaymentSessionKafka;
import com.example.portadapter.api.services.kafka.model.PaymentSessionKafkaDomain;
import com.example.portadapter.api.services.kafka.processors.PaymentSessionProcessor;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Profile("kafka")
@Service
public class PaymentSessionKafkaRepositorySupport {

    private final Processor processor;

    private final InteractiveQueryService interactiveQueryService;

    private ReadOnlyKeyValueStore<String, PaymentSessionKafka> paymentSessionKafkaKeyValueStore;

    private final ReplyingKafkaTemplate<String, PaymentSessionKafka, PaymentSessionKafka> kafkaTemplate;

    @Value("${spring.cloud.stream.kafka.streams.bindings.ktable.consumer.materializedAs}")
    private String STORAGE;

    @Value("${spring.cloud.stream.bindings.input.destination}")
    private String requestTopicName;

    private ReadOnlyKeyValueStore<String, PaymentSessionKafka> store() {
        return interactiveQueryService.getQueryableStore(STORAGE, QueryableStoreTypes.keyValueStore());
    }

    public PaymentSessionKafkaRepositorySupport(Processor processor, PaymentSessionProcessor paymentSessionProcessor, InteractiveQueryService interactiveQueryService, ReplyingKafkaTemplate<String, PaymentSessionKafka, PaymentSessionKafka> kafkaTemplate) {
        this.processor = processor;
        this.interactiveQueryService = interactiveQueryService;
        this.kafkaTemplate = kafkaTemplate;
    }

    private Message<PaymentSessionKafka> createMessage(PaymentSessionKafka s) {
        return MessageBuilder.withPayload(s).setHeader(KafkaHeaders.MESSAGE_KEY, s.getAmountDueGuid()).build();
    }

    public PaymentSessionKafkaDomain save(PaymentSessionKafkaDomain paymentSession) {
        Message<PaymentSessionKafka> message = createMessage(fromDomain(paymentSession));
        //    processor.output().send(message);
        return toDomain(saveReplayingTemplate(fromDomain(paymentSession)));
    }

    @SneakyThrows
    public PaymentSessionKafka saveReplayingTemplate(PaymentSessionKafka paymentSessionKafka) {
        String key = paymentSessionKafka.getAmountDueGuid().toString();
        var record = new ProducerRecord<>(requestTopicName, key, paymentSessionKafka);

        RequestReplyFuture<String, PaymentSessionKafka, PaymentSessionKafka> sendAndReceive = this.kafkaTemplate
                .sendAndReceive(record);

        ConsumerRecord<String, PaymentSessionKafka> consumerRecord = sendAndReceive.get();
        return consumerRecord.value();
    }

    public PaymentSessionKafkaDomain findById(String id) {
        if (paymentSessionKafkaKeyValueStore == null) {
            paymentSessionKafkaKeyValueStore = store();
        }
        PaymentSessionKafka paymentSession = paymentSessionKafkaKeyValueStore.get(id);
        if (paymentSession == null)
            return null;
        return toDomain(paymentSessionKafkaKeyValueStore.get(id));
    }

    public PaymentSessionKafkaDomain toDomain(PaymentSessionKafka paymentSession) {
        return new PaymentSessionKafkaDomain(paymentSession.getId().toString(), paymentSession.getCurrency().toString(), paymentSession.getAmount(), PaymentStatus.valueOf(paymentSession.getStatus().name()), paymentSession.getAmountDueGuid().toString());
    }

    public PaymentSessionKafka fromDomain(PaymentSessionKafkaDomain paymentSession) {
        return PaymentSessionKafka.newBuilder().setId(paymentSession.getId()).setAmountDueGuid(paymentSession.getAmountDueGuid()).setAmount(paymentSession.getAmount()).setStatus(com.example.portadapter.api.services.kafka.model.PaymentStatus.valueOf(paymentSession.getStatus().name())).setCurrency(paymentSession.getCurrency()).build();
    }
}
