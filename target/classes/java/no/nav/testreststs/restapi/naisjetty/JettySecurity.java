package no.nav.testreststs.restapi.naisjetty;

import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.DefaultAuthenticatorFactory;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JettySecurity {

    private static final Logger log = LoggerFactory.getLogger(JettySecurity.class);

    public WebAppContext jettySikkerhetConfig(WebAppContext webAppContext) throws IOException {
        JAASLoginService loginService = new JAASLoginService("JAAS Login");
        loginService.setLoginModuleName("ldaploginmodule");
        loginService.setIdentityService(new DefaultIdentityService());
        ConstraintSecurityHandler sh = new ConstraintSecurityHandler();
        sh.setAuthenticatorFactory(new DefaultAuthenticatorFactory());
        sh.setLoginService(loginService);
        sh.setDenyUncoveredHttpMethods(true);
        webAppContext.setSecurityHandler(sh);
        log.debug("Security setup NAV LDAP. Name: {}, ID-service{}", loginService.getName(), loginService.getIdentityService());
        return webAppContext;
    }
}
