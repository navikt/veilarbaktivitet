package no.nav.veilarbaktivitet.db.testdriver;

import no.nav.veilarbaktivitet.util.ProxyUtils;

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

    private Driver driver = new org.h2.Driver();

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        return ProxyUtils.proxy(new ConnectionInvocationHandler(driver.connect(getH2Url(url), info)), Connection.class);
    }

    public static String getH2Url(String url) {
        String uniktNavn = url.substring(BASE_URL.length());
        return String.format("jdbc:h2:mem:veilarbaktivitet-%s;DB_CLOSE_DELAY=-1;MODE=Oracle", uniktNavn);
    }

    @Override
    public boolean acceptsURL(String url) {
        return url.startsWith(BASE_URL);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return driver.getPropertyInfo(getH2Url(url), info);
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
