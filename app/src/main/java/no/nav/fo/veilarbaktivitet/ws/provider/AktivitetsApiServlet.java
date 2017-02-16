package no.nav.fo.veilarbaktivitet.ws.provider;

import no.nav.sbl.dialogarena.common.cxf.CXFEndpoint;
import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.IOException;

public class AktivitetsApiServlet extends CXFNonSpringServlet {

    private static void settOppEndpoints(ApplicationContext applicationContext) {
        new CXFEndpoint()
                .address("/AktivitetData")
                .serviceBean(applicationContext.getBean(AktivitetsoversiktWebService.class))
                .kerberosInInterceptor()
                .create();
    }

    @Override
    protected void loadBus(ServletConfig servletConfig) {
        super.loadBus(servletConfig);
        BusFactory.setDefaultBus(getBus());
        settOppEndpoints(WebApplicationContextUtils.getWebApplicationContext(servletConfig.getServletContext()));
    }

}
