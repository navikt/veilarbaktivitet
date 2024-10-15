package no.nav.veilarbaktivitet.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.config.kafka.NavCommonKafkaConfig;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;

@Service
@Slf4j
public class KafkaTestService {

    public static int DEFAULT_WAIT_TIMEOUT_SEC = 10;
    public static Duration DEFAULT_WAIT_TIMEOUT_DURATION = Duration.of(DEFAULT_WAIT_TIMEOUT_SEC, ChronoUnit.SECONDS);


    public KafkaTestService(
            @Qualifier("stringAvroConsumerFactory")
            ConsumerFactory<String, SpecificRecordBase> stringAvroConsumerFactory,
            @Qualifier("stringJsonConsumerFactory")
            ConsumerFactory<String, Object> stringJsonConsumerFactory,
            @Qualifier("avroAvroConsumerFactory")
            ConsumerFactory<SpecificRecordBase, SpecificRecordBase> avroAvroConsumerFactory,
            @Qualifier("stringStringConsumerFactory")
            ConsumerFactory<String, String> stringStringConsumerFactory,
            ConcurrentKafkaListenerContainerFactory<String, String> stringStringKafkaListenerContainerFactory,
            Admin kafkaAdminClient
    ) {
        this.stringAvroConsumerFactory = stringAvroConsumerFactory;
        this.stringJsonConsumerFactory = stringJsonConsumerFactory;
        this.avroAvroConsumerFactory = avroAvroConsumerFactory;
        this.stringStringConsumerFactory = stringStringConsumerFactory;
        this.stringStringKafkaListenerContainerFactory = stringStringKafkaListenerContainerFactory;
        this.kafkaAdminClient = kafkaAdminClient;
    }

    private final ConsumerFactory<String, SpecificRecordBase> stringAvroConsumerFactory;
    private final ConsumerFactory<String, Object> stringJsonConsumerFactory;
    private final ConsumerFactory<SpecificRecordBase, SpecificRecordBase> avroAvroConsumerFactory;
    private final ConsumerFactory<String, String> stringStringConsumerFactory;
    private final ConcurrentKafkaListenerContainerFactory<String, String> stringStringKafkaListenerContainerFactory;
    private final Admin kafkaAdminClient;

    @Value("${spring.kafka.consumer.group-id}")
    String aivenGroupId;

    private Properties kafkaTestConfig() {
        Properties modifisertConfig = new Properties();
        modifisertConfig.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return modifisertConfig;
    }

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
        Consumer newConsumer = stringAvroConsumerFactory.createConsumer(randomGroup, null, null, kafkaTestConfig());
        seekToEnd(topic, newConsumer);
        return newConsumer;
    }

    public Consumer createAvroAvroConsumer(String topic) {
        String randomGroup = UUID.randomUUID().toString();
        Consumer newConsumer = avroAvroConsumerFactory.createConsumer(randomGroup, null, null, kafkaTestConfig());
        seekToEnd(topic, newConsumer);
        return newConsumer;
    }

    public Consumer<String, String> createStringStringConsumer(String topic) {
        var randomGroup = UUID.randomUUID().toString();
        var newConsumer = stringStringConsumerFactory.createConsumer(randomGroup, null, null, kafkaTestConfig());
        seekToEnd(topic, newConsumer);
        return newConsumer;
    }

    public Consumer createStringJsonConsumer(String topic) {
        String randomGroup = UUID.randomUUID().toString();
        Consumer newConsumer = stringJsonConsumerFactory.createConsumer(randomGroup, null, null, kafkaTestConfig());
        seekToEnd(topic, newConsumer);
        return newConsumer;
    }

    public void seekToEnd(String topic, Consumer newConsumer) {
        List<PartitionInfo> partitionInfos = newConsumer.partitionsFor(topic);
        List<TopicPartition> topics = partitionInfos.stream().map(f -> new TopicPartition(topic, f.partition())).collect(Collectors.toList());
        newConsumer.assign(topics);
        newConsumer.seekToEnd(topics);
        topics.forEach(a -> newConsumer.position(a, Duration.ofSeconds(10)));
        newConsumer.commitSync(DEFAULT_WAIT_TIMEOUT_DURATION);
    }

    public void assertErKonsumert(String topic, String groupId, long producerOffset) {
        await()
                .with()
                .conditionEvaluationListener(condition -> log.debug("Venter på melding med offset {} på topic {} for groupId {} - Tid brukt: {}ms Tid gjenstående: {}ms", producerOffset, topic, groupId, condition.getElapsedTimeInMS(), condition.getRemainingTimeInMS()))
                .atMost(DEFAULT_WAIT_TIMEOUT_DURATION).until(() -> erKonsumert(topic, groupId, producerOffset));
    }

    public void assertErKonsumert(String topic, long producerOffset) {
        await()
                .with()
                .conditionEvaluationListener(condition -> log.debug("Venter på melding med offset {} på topic {} for groupId {} - Tid brukt: {}ms Tid gjenstående: {}ms", producerOffset, topic, aivenGroupId, condition.getElapsedTimeInMS(), condition.getRemainingTimeInMS()))
                .atMost(DEFAULT_WAIT_TIMEOUT_DURATION).until(() -> erKonsumert(topic, aivenGroupId, producerOffset));
    }

    public void assertErKonsumertNavCommon(String topic, long producerOffset) {
        await()
                .with()
                .conditionEvaluationListener(condition -> log.debug("Venter på melding med offset {} på topic {} for groupId {} - Tid brukt: {}ms Tid gjenstående: {}ms", producerOffset, topic, aivenGroupId, condition.getElapsedTimeInMS(), condition.getRemainingTimeInMS()))
                .atMost(DEFAULT_WAIT_TIMEOUT_DURATION).until(() -> erKonsumert(topic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, producerOffset));
    }

    @SneakyThrows
    private boolean erKonsumert(String topic, String groupId, long producerOffset) {
        Map<TopicPartition, OffsetAndMetadata> topicPartitionOffsetAndMetadataMap = kafkaAdminClient.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get();
        OffsetAndMetadata offsetAndMetadata = topicPartitionOffsetAndMetadataMap.get(new TopicPartition(topic, 0));

        if (offsetAndMetadata == null) {
            return false;
        }

        long commitedOffset = offsetAndMetadata.offset();
        // Consumer-group offsets er på meldinger som ikke er committet/behandlet enda
        return (commitedOffset - 1) >= producerOffset;
    }

    @SneakyThrows
    public boolean harKonsumertAlleMeldinger(String topic, Consumer consumer) {
        consumer.commitSync();
        String groupId = consumer.groupMetadata().groupId();
        Map<TopicPartition, OffsetAndMetadata> topicPartitionOffsetAndMetadataMap = kafkaAdminClient
                .listConsumerGroupOffsets(groupId)
                .partitionsToOffsetAndMetadata().get();
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
}
