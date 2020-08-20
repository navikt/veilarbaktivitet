package no.nav.veilarbaktivitet.db.testdriver;

import no.nav.veilarbaktivitet.util.ProxyUtils;
import no.nav.veilarbaktivitet.util.ReflectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;

public class ConnectionInvocationHandler implements InvocationHandler {

    private static final Method CREATE_STATEMENT_METHOD = ReflectionUtils.getMethod(Connection.class, "createStatement");

    private final Connection connection;

    ConnectionInvocationHandler(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object realResult = method.invoke(connection, args);
        if (CREATE_STATEMENT_METHOD.equals(method)) {
            return ProxyUtils.proxy(new StatementInvocationHandler((Statement) realResult), Statement.class);
        }
        return realResult;
    }

}
