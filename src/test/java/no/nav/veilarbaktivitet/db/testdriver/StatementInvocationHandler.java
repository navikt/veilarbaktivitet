package no.nav.veilarbaktivitet.db.testdriver;

import no.nav.veilarbaktivitet.util.ReflectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Statement;

public class StatementInvocationHandler implements InvocationHandler {

    private static final Method EXECUTE_METHOD = ReflectionUtils.getMethod(Statement.class, "execute", String.class);

    private final Statement statement;

    StatementInvocationHandler(Statement statement) {
        this.statement = statement;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (EXECUTE_METHOD.equals(method)) {
            args[0] = DatabaseSyntaxMapper.syntax((String) args[0]);
        }
        return method.invoke(statement, args);
    }

}
