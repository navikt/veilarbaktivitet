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
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KafkaTestService {

    private final ConsumerFactory<String, SpecificRecordBase> stringAvroConsumerFactory;

    private final ConsumerFactory<SpecificRecordBase, SpecificRecordBase> avroAvroConsumerFactory;

    private final ConsumerFactory<String, String> stringStringConsumerFactory;

    private final Admin kafkaAdminClient;

    /**
     * Lager en ny kafka consumer med random groupid på topic som leser fra slutten av topic.
     * Meldinger kan leses ved å bruke utility metoder i  KafkaTestUtils
     * @see org.springframework.kafka.test.utils.KafkaTestUtils#getSingleRecord(org.apache.kafka.clients.consumer.Consumer, java.lang.String, long)
     * @param topic Topic du skal lese fra
     * @return En kafka consumer
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

    public Consumer createStringStringConsumer(String topic) {
        String randomGroup = UUID.randomUUID().toString();
        Properties modifisertConfig = new Properties();
        modifisertConfig.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        Consumer newConsumer = stringStringConsumerFactory.createConsumer(randomGroup, null, null, modifisertConfig);
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

}
