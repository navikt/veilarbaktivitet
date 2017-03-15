package no.nav.fo.veilarbaktivitet.ws.provider;

import no.nav.sbl.dialogarena.common.cxf.CXFEndpoint;
import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;

public class ApiServlet extends CXFNonSpringServlet {

    private static void settOppEndpoints(ApplicationContext applicationContext) {
        new CXFEndpoint()
                .address("/Aktivitet")
                .serviceBean(applicationContext.getBean(SoapService.class))
                .create();
    }

    @Override
    protected void loadBus(ServletConfig servletConfig) {
        super.loadBus(servletConfig);
        BusFactory.setDefaultBus(getBus());
        settOppEndpoints(WebApplicationContextUtils.getWebApplicationContext(servletConfig.getServletContext()));
    }

}
