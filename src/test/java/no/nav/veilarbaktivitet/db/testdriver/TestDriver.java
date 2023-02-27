package no.nav.veilarbaktivitet.db.testdriver;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.util.ProxyUtils;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

@Slf4j
public class TestDriver implements Driver {

    private static volatile boolean isInitialized = false;


    public static synchronized void init() {
        if (isInitialized) {
            return;
        }

        isInitialized = true;

        try {
            // Registrer test driver og deregistrer h2-driver slik at den ikke blir brukt med et uhell
            log.info("Registering TestDriver");
            DriverManager.registerDriver(new TestDriver());
            DriverManager.deregisterDriver(org.h2.Driver.load());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private final Driver driver = new org.h2.Driver();

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        return ProxyUtils.proxy(new ConnectionInvocationHandler(driver.connect(url, info)), Connection.class);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return driver.acceptsURL(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return driver.getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion() {
        return driver.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return driver.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return driver.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return driver.getParentLogger();
    }

}
