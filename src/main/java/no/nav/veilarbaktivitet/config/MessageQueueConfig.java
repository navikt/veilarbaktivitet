package no.nav.veilarbaktivitet.config;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsConstants;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import no.nav.common.utils.Credentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;

import static com.ibm.msg.client.jms.JmsConstants.WMQ_PROVIDER;
import static com.ibm.msg.client.wmq.common.CommonConstants.*;
import static no.nav.common.utils.EnvironmentUtils.*;

@Configuration
@EnableJms
public class MessageQueueConfig {

    private static final String MQGATEWAY03_HOSTNAME_PROPERTY = "MQGATEWAY03_HOSTNAME";
    private static final String MQGATEWAY03_PORT_PROPERTY = "MQGATEWAY03_PORT";
    private static final String MQGATEWAY03_NAME_PROPERTY = "MQGATEWAY03_NAME";
    private static final String VARSELPRODUKSJON_VARSLINGER_QUEUENAME_PROPERTY = "VARSELPRODUKSJON_VARSLINGER_QUEUENAME";

    @Bean
    public JmsTemplate varselQueue(ConnectionFactory factory, JMSContext context) {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(factory);
        jmsTemplate.setDefaultDestination(context.createQueue(getRequiredProperty(VARSELPRODUKSJON_VARSLINGER_QUEUENAME_PROPERTY)));
        return jmsTemplate;
    }

    @Bean
    JMSContext jmsContext(ConnectionFactory factory) {
        return factory.createContext();
    }

    @Bean
    public ConnectionFactory connectionFactory(Credentials credentials) throws JMSException {
        JmsFactoryFactory jmsFactoryFactory = JmsFactoryFactory.getInstance(WMQ_PROVIDER);
        JmsConnectionFactory connectionFactory = jmsFactoryFactory.createConnectionFactory();

        String env = requireNamespace().equals("default") ? "p" : requireNamespace();

        connectionFactory.setStringProperty(WMQ_HOST_NAME, getRequiredProperty(MQGATEWAY03_HOSTNAME_PROPERTY));
        connectionFactory.setStringProperty(WMQ_PORT, getRequiredProperty(MQGATEWAY03_PORT_PROPERTY));
        connectionFactory.setStringProperty(WMQ_CHANNEL, String.format("%s_%s", env, requireApplicationName()).toUpperCase());
        connectionFactory.setIntProperty(WMQ_CONNECTION_MODE, WMQ_CM_CLIENT);
        connectionFactory.setStringProperty(WMQ_QUEUE_MANAGER, getRequiredProperty(MQGATEWAY03_NAME_PROPERTY));
        connectionFactory.setStringProperty(USERID, credentials.username);
        connectionFactory.setStringProperty(PASSWORD, credentials.password);
        connectionFactory.setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true);

        return connectionFactory;
    }
}
