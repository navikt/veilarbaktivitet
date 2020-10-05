package no.nav.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class AvsluttetOppfolgingFeedDTO implements Comparable<AvsluttetOppfolgingFeedDTO> {
    public static final String FEED_NAME = "avsluttetoppfolging";

    public String aktoerid;
    public ZonedDateTime sluttdato;
    public ZonedDateTime oppdatert;

    @Override
    public int compareTo(AvsluttetOppfolgingFeedDTO avsluttetOppfolgingFeedDTO) {
        return oppdatert.compareTo(avsluttetOppfolgingFeedDTO.oppdatert);
    }
}