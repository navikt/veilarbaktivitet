package no.nav.veilarbaktivitet.config;

import no.nav.veilarbaktivitet.aktivitetskort.service.AktivitetskortService;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaJsonTemplate;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringAvroTemplate;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MockitoSpyBean(types = {KafkaStringTemplate.class, KafkaStringAvroTemplate.class, AktivitetskortService.class})
@MockitoSpyBean(name = "navCommonKafkaJsonTemplate", types = {KafkaJsonTemplate.class})
public @interface SharedSpies {
}