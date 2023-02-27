package no.nav.veilarbaktivitet.mock;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;

@Slf4j
public class ExecuteWithAuthContext implements InvocationInterceptor {

    private AuthContext context;

    private ExecuteWithAuthContext() {
    }

    public ExecuteWithAuthContext(AuthContext context) {
        this.context = context;
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
                             ExtensionContext extensionContext) throws Throwable {
        AuthContextHolderThreadLocal.instance().withContext(this.context, invocation::proceed);

    }
}
