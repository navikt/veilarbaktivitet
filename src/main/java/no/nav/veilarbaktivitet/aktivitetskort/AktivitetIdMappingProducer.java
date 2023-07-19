package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMapping;
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDto;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
/**
 * Publiserer koblingen mellom teknisk id, arena-id og funksjonell id.
 * Denne brukes i dialog for å gjøre løpende migrering fra arena-id til teknisk id på gamle dialoger på arena-aktiviteter.
 */
public class AktivitetIdMappingProducer {

    @Autowired
    KafkaProducerClient<String, String> aivenProducerClient;

    @Value("${topic.ut.aktivitetskort-idmapping}")
    String idmappingTopic;


    public void publishAktivitetskortIdMapping(IdMapping idMapping) {
        IdMappingDto melding = IdMappingDto.map(idMapping);
        var producerRecord = new ProducerRecord<String, String>(idmappingTopic, melding.funksjonellId().toString(), JsonUtils.toJson(melding));
        aivenProducerClient.sendSync(producerRecord);
    }

}
