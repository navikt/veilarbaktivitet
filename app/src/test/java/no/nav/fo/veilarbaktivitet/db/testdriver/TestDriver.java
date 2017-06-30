package no.nav.fo.veilarbaktivitet.db.testdriver;

import no.nav.fo.veilarbaktivitet.util.ProxyUtils;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

import static java.sql.DriverManager.registerDriver;

public class TestDriver implements Driver {

    private static final String BASE_URL = TestDriver.class.getSimpleName();

    private static int count;
    public static String getURL() {
        return BASE_URL + "-" + count++;
    }

    static {
        try {
            registerDriver(new TestDriver());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Driver driver = new org.hsqldb.jdbcDriver();

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        return ProxyUtils.proxy(new ConnectionInvocationHandler(driver.connect(getHsqlUrl(url), info)), Connection.class);
    }

    private String getHsqlUrl(String url) {
        return "jdbc:hsqldb:mem:veilarbaktivitet" + url.substring(BASE_URL.length());
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return driver.acceptsURL(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return driver.getPropertyInfo(getHsqlUrl(url), info);
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
