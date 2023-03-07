package no.nav.veilarbaktivitet.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;

@Slf4j
class SkalLoggeTest {


    @Test
    @SneakyThrows
    void testLogging() {
        PrintStream out = System.out;

        LoadLogbackConfig("/logback-spring.xml");
        ByteArrayOutputStream outputStream = captureSystemOut();

        log.debug("Debug-melding");
        log.info("Info-melding");
        log.warn("Advarsel");
        log.error("Feilmelding");

        flushLogs();

        String afterText = outputStream.toString();

        Assertions.assertTrue(afterText.contains("Info-melding"));
        Assertions.assertTrue(afterText.contains("Advarsel"));
        Assertions.assertTrue(afterText.contains("Feilmelding"));
        outputStream.toString();

        System.setOut(out);
    }

    private void LoadLogbackConfig(String path) throws JoranException {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();
        loggerContext.putProperty("testName", "LogbackTest");
        loggerContext.putProperty("logDirectory", "logs");
        // Sett konfigurasjonsfilen for LoggerContext

        URL configUrl = getClass().getResource(path);
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        configurator.doConfigure(configUrl);
        StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
    }

    @NotNull
    private static ByteArrayOutputStream captureSystemOut() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        return outputStream;
    }

    private static void flushLogs() {
        LoggerContext loggerContext1 = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext1.stop();
    }
}
