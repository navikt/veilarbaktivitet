package no.nav.veilarbaktivitet.domain;

import java.util.Date;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AvsluttetOppfolgingFeedDTO
	implements Comparable<AvsluttetOppfolgingFeedDTO> {
	public static final String FEED_NAME = "avsluttetoppfolging";

	public String aktoerid;
	public Date sluttdato;
	public Date oppdatert;

	@Override
	public int compareTo(AvsluttetOppfolgingFeedDTO avsluttetOppfolgingFeedDTO) {
		return oppdatert.compareTo(avsluttetOppfolgingFeedDTO.oppdatert);
	}
}
