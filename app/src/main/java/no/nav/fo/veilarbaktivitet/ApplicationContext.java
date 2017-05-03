package no.nav.fo.veilarbaktivitet;

import no.nav.apiapp.ApiApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableAspectJAutoProxy
@ComponentScan("no.nav.fo.veilarbaktivitet")
public class ApplicationContext implements ApiApplication {

    @Override
    public Sone getSone() {
        return Sone.FSS;
    }

}
