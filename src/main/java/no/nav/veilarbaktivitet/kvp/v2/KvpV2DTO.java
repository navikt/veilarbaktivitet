package no.nav.veilarbaktivitet.kvp.v2;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class KvpV2DTO implements Comparable<KvpV2DTO> {
    private long kvpId;
    private long serial;
    private String aktorId;
    private String enhet;
    private String opprettetAv;
    private ZonedDateTime opprettetDato;
    private String opprettetBegrunnelse;
    private String avsluttetAv;
    private ZonedDateTime avsluttetDato;
    private String avsluttetBegrunnelse;

    @Override
    public int compareTo(KvpV2DTO k) {
        return Long.compare(serial, k.serial);
    }
}