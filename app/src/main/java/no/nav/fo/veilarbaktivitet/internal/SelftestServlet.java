package no.nav.fo.veilarbaktivitet.internal;

import no.nav.sbl.dialogarena.common.web.selftest.SelfTestBaseServlet;
import no.nav.sbl.dialogarena.types.Pingable;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.Collection;

import static org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext;

public class SelftestServlet extends SelfTestBaseServlet {

    private ApplicationContext applicationContext;

    @Override
    public void init() throws ServletException {
        super.init();
        applicationContext = getWebApplicationContext(getServletContext());
    }

    @Override
    public String getApplicationName() {
        return "veilarbaktivitet";
    }

    @Override
    public Collection<? extends Pingable> getPingables() {
        return new ArrayList<>(applicationContext.getBeansOfType(Pingable.class).values());
    }

}
