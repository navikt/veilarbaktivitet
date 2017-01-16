package no.nav.fo.veilarbaktivitet.ws.provider;

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

    // TODO
    public static void settOppEndpoints(ApplicationContext applicationContext) {
//        new CXFEndpoint()
//                .address("/Aktivitet")
//                .serviceBean(applicationContext.getBean(AktivitetsoversiktWebService.class))
//                .kerberosInInterceptor()
//                .create();
    }

    @Override
    protected void loadBus(ServletConfig servletConfig) {
        super.loadBus(servletConfig);
        BusFactory.setDefaultBus(getBus());
        settOppEndpoints(WebApplicationContextUtils.getWebApplicationContext(servletConfig.getServletContext()));
    }


    // TODO mens vi venter på tjenestespesifikasjonen
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) req;
        HttpServletResponse httpServletResponse = (HttpServletResponse) res;

        String ident = req.getParameter("ident");

        WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        AktivitetsoversiktWebService aktivitetsoversiktWebService = webApplicationContext.getBean(AktivitetsoversiktWebService.class);

        if ("POST".equals(httpServletRequest.getMethod())) {
            AktivitetsoversiktWebService.WSOpprettNyEgenAktivitetRequest request = new AktivitetsoversiktWebService.WSOpprettNyEgenAktivitetRequest();
            request.ident = ident;
            write(httpServletResponse, aktivitetsoversiktWebService.opprettNyEgenAktivitet(request));
        } else {
            AktivitetsoversiktWebService.WSHentAktiviteterRequest hentAktiviteterRequest = new AktivitetsoversiktWebService.WSHentAktiviteterRequest();
            hentAktiviteterRequest.ident = ident;
            write(httpServletResponse, aktivitetsoversiktWebService.hentAktiviteter(hentAktiviteterRequest));
        }
    }

    private void write(HttpServletResponse httpServletResponse, Object response) throws IOException, ServletException {
        try {
            JAXBContext.newInstance(response.getClass()).createMarshaller().marshal(response, httpServletResponse.getWriter());
        } catch (JAXBException e) {
            throw new ServletException(e);
        }
    }
    ///////////////////////////

}
