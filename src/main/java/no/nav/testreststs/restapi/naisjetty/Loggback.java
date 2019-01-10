package no.nav.testreststs.restapi.naisjetty;

public class Loggback {

    private static final String LOGBACK_PROP = "/webapp/logback-remote.xml";
    private static final String LOGBACK_PROP_LOCAL = "/webapp/logback-local.xml";

    public static String setLogbackType(){
        if(System.getenv("LOGGBACK_REMOTE") != null) {
            return LOGBACK_PROP;
        }else {
            return LOGBACK_PROP_LOCAL;
        }
    }
}
