package no.nav.veilarbaktivitet.kafka;

import lombok.Getter;
import lombok.Setter;

import static java.lang.String.format;
import static no.nav.common.utils.EnvironmentUtils.isProduction;
import static no.nav.veilarbaktivitet.util.EnumUtils.getName;

@Getter
@Setter
public class KafkaTopics {

    public enum Topic {
        ENDRING_PA_AKTIVITET, // Produce
        OPPFOLGING_AVSLUTTET, // Consume
        KVP_AVSLUTTET // Consume
    }

    private String endringPaAktivitet;

    private String oppfolgingAvsluttet;

    private String kvpAvsluttet;

    private KafkaTopics() {}

    public static KafkaTopics create(String envSuffix) {
        KafkaTopics kafkaTopics = new KafkaTopics();

        kafkaTopics.setEndringPaAktivitet("aapen-fo-endringPaaAktivitet-v4-" + envSuffix);
        kafkaTopics.setOppfolgingAvsluttet("aapen-arbeidsrettetOppfolging-oppfolgingAvsluttet-v1-" + envSuffix);
        kafkaTopics.setKvpAvsluttet("aapen-arbeidsrettetOppfolging-kvpAvsluttet-v1-" + envSuffix);

        return kafkaTopics;
    }

    public String[] getAllTopics() {
        return new String[]{
                this.getEndringPaAktivitet(),
                this.getOppfolgingAvsluttet(),
                this.getKvpAvsluttet()
        };
    }

    public String topicToStr(Topic topic) {
        switch (topic) {
            case ENDRING_PA_AKTIVITET:
                return endringPaAktivitet;
            case OPPFOLGING_AVSLUTTET:
                return oppfolgingAvsluttet;
            case KVP_AVSLUTTET:
                return kvpAvsluttet;
            default:
                throw new IllegalArgumentException(format("Klarte ikke å mappe %s til string", getName(topic)));
        }
    }

    public Topic strToTopic(String topicStr) {
        if (endringPaAktivitet.equals(topicStr)) {
            return Topic.ENDRING_PA_AKTIVITET;
        } else if (oppfolgingAvsluttet.equals(topicStr)) {
            return Topic.OPPFOLGING_AVSLUTTET;
        } else if (kvpAvsluttet.equals(topicStr)) {
            return Topic.KVP_AVSLUTTET;
        }

        throw new IllegalArgumentException(format("Klarte ikke å mappe %s til enum", topicStr));
    }

    public static String requireKafkaTopicEnv() {
        return isProduction().orElseThrow() ? "p" : "q1";
    }

}
