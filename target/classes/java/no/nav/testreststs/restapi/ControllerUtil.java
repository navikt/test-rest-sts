package no.nav.testreststs.restapi;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;

public class ControllerUtil {

    public static String getUserName(SecurityContext securityContext) {
        if (securityContext == null) {
            throw new SecurityException("SecurityContext er null");
        }
        if (securityContext.getUserPrincipal() == null) {
            throw new SecurityException("UserPrincipal er null i SecurityContext");
        }
        return securityContext.getUserPrincipal().getName();
    }
    
    public static String getPassword(HttpHeaders headers) {
    	if (headers == null || headers.getRequestHeader(HttpHeaders.AUTHORIZATION) == null || headers.getRequestHeader(HttpHeaders.AUTHORIZATION).isEmpty()) {
            throw new SecurityException("Ingen Autorization header i request");
    	}
        return BasicAuthHeaderUtil.getPassword(headers.getRequestHeader(HttpHeaders.AUTHORIZATION));
    }
}
