package no.nav.fo.veilarbaktivitet.mappers;

import no.nav.fo.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData;
import no.nav.fo.veilarbaktivitet.domain.InnsenderData;
import no.nav.fo.veilarbaktivitet.domain.StillingsoekEtikettData;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.AktivitetType;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Etikett;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.InnsenderType;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.TransaksjonType;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.EGENAKTIVITET;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.JOBBSOEKING;
import static no.nav.fo.veilarbaktivitet.domain.InnsenderData.BRUKER;
import static no.nav.fo.veilarbaktivitet.domain.InnsenderData.NAV;
import static no.nav.fo.veilarbaktivitet.domain.StillingsoekEtikettData.*;
import static no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.TransaksjonType.*;

public class Helpers {
    static final BidiMap<InnsenderType, InnsenderData> innsenderMap =
            new DualHashBidiMap<InnsenderType, InnsenderData>() {{
                put(InnsenderType.BRUKER, BRUKER);
                put(InnsenderType.NAV, NAV);
            }};


    static final BidiMap<AktivitetType, AktivitetTypeData> typeMap =
            new DualHashBidiMap<AktivitetType, AktivitetTypeData>() {{
                put(AktivitetType.JOBBSOEKING, JOBBSOEKING);
                put(AktivitetType.EGENAKTIVITET, EGENAKTIVITET);
            }};


    static final BidiMap<Etikett, StillingsoekEtikettData> etikettMap =
            new DualHashBidiMap<Etikett, StillingsoekEtikettData>() {{
                put(Etikett.AVSLAG, AVSLAG);
                put(Etikett.INNKALDT_TIL_INTERVJU, INNKALT_TIL_INTERVJU);
                put(Etikett.JOBBTILBUD, JOBBTILBUD);
                put(Etikett.SOEKNAD_SENDT, SOKNAD_SENDT);
            }};

    static final BidiMap<AktivitetTransaksjonsType, TransaksjonType> transaksjonsTypeMap =
            new DualHashBidiMap<AktivitetTransaksjonsType, TransaksjonType>() {{
                put(AktivitetTransaksjonsType.AVTALT, AVTALT);
                put(AktivitetTransaksjonsType.AVTALT_DATO_ENDRET, AVTALT_DATO_ENDRET);
                put(AktivitetTransaksjonsType.DETALJER_ENDRET, DETALJER_ENDRET);
                put(AktivitetTransaksjonsType.ETIKETT_ENDRET, ETIKETT_ENDRET);
                put(AktivitetTransaksjonsType.OPPRETTET, OPPRETTET);
                put(AktivitetTransaksjonsType.STATUS_ENDRET, STATUS_ENDRET);
            }};

}
