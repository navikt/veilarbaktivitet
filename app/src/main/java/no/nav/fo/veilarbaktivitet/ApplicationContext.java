package no.nav.fo.veilarbaktivitet;

import no.nav.apiapp.ApiApplication;
import no.nav.dialogarena.aktor.AktorConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableAspectJAutoProxy
@ComponentScan("no.nav.fo.veilarbaktivitet")
@Import(AktorConfig.class)
public class ApplicationContext implements ApiApplication {

    public final String APPLICATION_NAME = "veilarbaktivitet";

    @Override
    public String getApplicationName() {
        return APPLICATION_NAME;
    }

    @Override
    public Sone getSone() {
        return Sone.FSS;
    }

}
