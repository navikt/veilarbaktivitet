package no.nav.veilarbaktivitet.util;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@Service
@RequiredArgsConstructor
public class KafkaTestService {

    public static int DEFAULT_WAIT_TIMEOUT_SEC = 5;


    private final ConsumerFactory<String, SpecificRecordBase> stringAvroConsumerFactory;

    private final ConsumerFactory<String, Object> stringJsonConsumerFactory;

    private final ConsumerFactory<SpecificRecordBase, SpecificRecordBase> avroAvroConsumerFactory;

    private final ConsumerFactory<String, String> stringStringConsumerFactory;

    private final Admin kafkaAdminClient;

    @Value("${app.kafka.consumer-group-id}")
    String onPremConsumerGroup;

    @Value("${spring.kafka.consumer.group-id}")
    String aivenGroupId;

    /**
     * Lager en ny kafka consumer med random groupid på topic som leser fra slutten av topic.
     * Meldinger kan leses ved å bruke utility metoder i  KafkaTestUtils
     *
     * @param topic Topic du skal lese fra
     * @return En kafka consumer
     * @see org.springframework.kafka.test.utils.KafkaTestUtils#getSingleRecord(org.apache.kafka.clients.consumer.Consumer, java.lang.String, long)
     */
    public Consumer createStringAvroConsumer(String topic) {
        String randomGroup = UUID.randomUUID().toString();
        Properties modifisertConfig = new Properties();
        modifisertConfig.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        Consumer newConsumer = stringAvroConsumerFactory.createConsumer(randomGroup, null, null, modifisertConfig);
        seekToEnd(topic, newConsumer);

        return newConsumer;
    }

    public Consumer createAvroAvroConsumer(String topic) {
        String randomGroup = UUID.randomUUID().toString();
        Properties modifisertConfig = new Properties();
        modifisertConfig.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        Consumer newConsumer = avroAvroConsumerFactory.createConsumer(randomGroup, null, null, modifisertConfig);
        seekToEnd(topic, newConsumer);

        return newConsumer;
    }

    public Consumer<String, String> createStringStringConsumer(String topic) {
        String randomGroup = UUID.randomUUID().toString();
        Properties modifisertConfig = new Properties();
        modifisertConfig.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        Consumer<String, String> newConsumer = stringStringConsumerFactory.createConsumer(randomGroup, null, null, modifisertConfig);
        seekToEnd(topic, newConsumer);

        return newConsumer;
    }

    public Consumer createStringJsonConsumer(String topic) {
        String randomGroup = UUID.randomUUID().toString();
        Properties modifisertConfig = new Properties();
        modifisertConfig.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        Consumer newConsumer = stringJsonConsumerFactory.createConsumer(randomGroup, null, null, modifisertConfig);
        seekToEnd(topic, newConsumer);

        return newConsumer;
    }

    public void seekToEnd(String topic, Consumer newConsumer) {
        List<PartitionInfo> partitionInfos = newConsumer.partitionsFor(topic);
        List<TopicPartition> collect = partitionInfos.stream().map(f -> new TopicPartition(topic, f.partition())).collect(Collectors.toList());

        newConsumer.assign(collect);
        newConsumer.seekToEnd(collect);

        collect.forEach(a -> newConsumer.position(a, Duration.ofSeconds(10)));

        newConsumer.commitSync(Duration.ofSeconds(10));
    }

    public void assertErKonsumertOnprem(String topic, long producerOffset, int timeOutSeconds) {
        await().atMost(timeOutSeconds, SECONDS).until(() -> erKonsumert(topic, onPremConsumerGroup, producerOffset));
    }

    public void assertErKonsumertAiven(String topic, long producerOffset, int timeOutSeconds) {
        await().atMost(timeOutSeconds, SECONDS).until(() -> erKonsumert(topic, aivenGroupId, producerOffset));
    }

    public void assertErKonsumertAiven(String topic, String groupId, long producerOffset, int timeOutSeconds) {
        await().atMost(timeOutSeconds, SECONDS).until(() -> erKonsumert(topic, groupId, producerOffset));
    }

    @SneakyThrows
    public boolean erKonsumert(String topic, String groupId, long producerOffset) {
        Map<TopicPartition, OffsetAndMetadata> topicPartitionOffsetAndMetadataMap = kafkaAdminClient.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get();
        OffsetAndMetadata offsetAndMetadata = topicPartitionOffsetAndMetadataMap.get(new TopicPartition(topic, 0));

        if (offsetAndMetadata == null) {
            return false;
        }

        long commitedOffset = offsetAndMetadata.offset();
        return commitedOffset >= producerOffset;
    }

    @SneakyThrows
    public Optional<Long> hentOffset(String topic, String groupId) {
        var offsetAndMetadata = kafkaAdminClient.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get()
                .get(new TopicPartition(topic, 0));
        return Optional.ofNullable(offsetAndMetadata == null ? null : offsetAndMetadata.offset());
    }

    @SneakyThrows
    public boolean harKonsumertAlleMeldinger(String topic, Consumer consumer) {
        consumer.commitSync();
        String groupId = consumer.groupMetadata().groupId();
        Map<TopicPartition, OffsetAndMetadata> topicPartitionOffsetAndMetadataMap = kafkaAdminClient.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get();
        OffsetAndMetadata offsetAndMetadata = topicPartitionOffsetAndMetadataMap.get(new TopicPartition(topic, 0));

        if (offsetAndMetadata == null) {
            // Hvis ingen commitede meldinger, så er alt konsumert
            return true;
        }

        List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
        List<TopicPartition> collect = partitionInfos.stream().map(f -> new TopicPartition(topic, f.partition())).collect(Collectors.toList());

        Map<TopicPartition, Long> map = consumer.endOffsets(collect);
        Long endOffset = map.get(collect.get(0));

        return offsetAndMetadata.offset() == endOffset;
    }

    public String getAivenConsumerGroup() {
        return aivenGroupId;
    }
}
