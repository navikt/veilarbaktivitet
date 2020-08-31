package no.nav.veilarbaktivitet.helsesjekk;

import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;

@Component
public class VarselQueHelsesjekk implements HealthCheck {

    private JmsTemplate varselQueue;

    public VarselQueHelsesjekk(JmsTemplate varselQueue) {
        this.varselQueue = varselQueue;
    }

    @Override
    public HealthCheckResult checkHealth() {

        try {
            varselQueue.getConnectionFactory().createConnection().close();
        } catch (JMSException e) {
            return HealthCheckResult.unhealthy("Helsesjekk feilet mot mq for servicevarsel", e);
        }
        return HealthCheckResult.healthy();
    }
}

