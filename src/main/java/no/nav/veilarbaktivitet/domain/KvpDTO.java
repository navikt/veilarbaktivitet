package no.nav.veilarbaktivitet.domain;


import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class KvpDTO implements Comparable<KvpDTO> {
    public static final String FEED_NAME = "kvp";

    private long kvpId;
    private long serial;
    private String aktorId;
    private String enhet;
    private String opprettetAv;
    private Date opprettetDato;
    private String opprettetBegrunnelse;
    private String avsluttetAv;
    private Date avsluttetDato;
    private String avsluttetBegrunnelse;

    @Override
    public int compareTo(KvpDTO k) {
        return Long.compare(serial, k.serial);
    }
}