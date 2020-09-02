package no.nav.veilarbaktivitet.config;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;

import static no.nav.common.utils.EnvironmentUtils.*;

@Configuration
@EnableJms
public class MessageQueueConfig {

    private static final String MQGATEWAY03_HOSTNAME_PROPERTY = "MQGATEWAY03_HOSTNAME";
    private static final String MQGATEWAY03_PORT_PROPERTY = "MQGATEWAY03_PORT";
    private static final String MQGATEWAY03_NAME_PROPERTY = "MQGATEWAY03_NAME";
    private static final String VARSELPRODUKSJON_VARSLINGER_QUEUENAME_PROPERTY = "VARSELPRODUKSJON_VARSLINGER_QUEUENAME";

    @Bean
    public JmsTemplate varselQueue(ConnectionFactory connectionFactory) {
        return queue(connectionFactory, getRequiredProperty(VARSELPRODUKSJON_VARSLINGER_QUEUENAME_PROPERTY));
    }

    private JmsTemplate queue(ConnectionFactory connectionFactory, String queueName) {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(connectionFactory);

        JMSContext context = connectionFactory.createContext();
        jmsTemplate.setDefaultDestination(context.createQueue(queueName));

        return jmsTemplate;
    }

    @Bean
    public ConnectionFactory connectionFactory() throws JMSException {
        JmsFactoryFactory jmsFactoryFactory = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
        JmsConnectionFactory connectionFactory = jmsFactoryFactory.createConnectionFactory();

        String env = requireNamespace().equals("default") ? "p" : requireNamespace();

        connectionFactory.setStringProperty(WMQConstants.WMQ_HOST_NAME, getRequiredProperty(MQGATEWAY03_HOSTNAME_PROPERTY));
        connectionFactory.setStringProperty(WMQConstants.WMQ_PORT, getRequiredProperty(MQGATEWAY03_PORT_PROPERTY));
        connectionFactory.setStringProperty(WMQConstants.WMQ_CHANNEL, String.format("%s_%s", env, requireApplicationName()).toUpperCase());
        connectionFactory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
        connectionFactory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, getRequiredProperty(MQGATEWAY03_NAME_PROPERTY));
        connectionFactory.setStringProperty(WMQConstants.USERID, "srvappserver");

        return connectionFactory;
    }
}
