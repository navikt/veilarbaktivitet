package no.nav.veilarbaktivitet.avtaltMedNav;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Type {
    SEND_FORHAANDSORIENTERING("send_forhandsorientering"),
    SEND_PARAGRAF_11_9("send_paragraf_11_9"),
    IKKE_SEND_FORHAANDSORIENTERING("ikke_send_forhandsorientering");

    private final String value;
}