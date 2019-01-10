package no.nav.testreststs.restapi.naisjetty;



public class LocalEnvironment {

    public static void checkIfLocalSetting(String[] args) {
        if (args.length > 0 && args[0].contains("-test")) {
            setupTestEnv();
        }
    }

    private static void setupTestEnv() {       
        // local test ldap
        System.setProperty("ldap.url", "ldap://ldapgw.test.local:389/");
        
        System.setProperty("ldap.basedn", "dc=test,dc=local");
        System.setProperty("ldap.serviceuser.basedn", "ou=ServiceAccounts,dc=test,dc=local");
         
        System.setProperty("test.rest.sts.issuerUrl", "https://test-rest-sts.nais.preprod.local");                       
    }
}
