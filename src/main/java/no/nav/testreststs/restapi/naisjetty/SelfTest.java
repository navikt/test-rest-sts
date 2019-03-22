package no.nav.testreststs.restapi.naisjetty;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import no.nav.testreststs.accesstoken.AccessTokenIssuer;

public class SelfTest {

    private static final Logger log = LoggerFactory.getLogger(SelfTest.class);

    @Autowired
    private AccessTokenIssuer tokenIssuer;

    public SelfTest(AccessTokenIssuer tokenIssuer) {
        this.tokenIssuer = tokenIssuer;
    }

    public SelfTest() { 
    	
    }

    public boolean isAlive() {
        if (tokenIssuer == null) {
            log.error("isAlive returnerer false: NULL AccessTokenIssuer (feil i initialisering).");
            return false;
        }
        return true;
    }

    public boolean selftest() {    	
    	try {
    		return tokenIssuer.selfTest();
    	} catch (Exception e) {
    		return false;	
    	}
    }

}
