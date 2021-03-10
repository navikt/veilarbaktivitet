package no.nav.veilarbaktivitet.mock;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

@Slf4j
public class AuthContextRule implements MethodRule {

    private AuthContext context;

    public AuthContextRule() {
    }

    public AuthContextRule(AuthContext context) {
        this.context = context;
    }

    @Override
    public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object o) {
        return new Statement() {
            @Override
            public void evaluate() {
                AuthContextHolderThreadLocal.instance().withContext(context, statement::evaluate);
            }
        };
    }
}
