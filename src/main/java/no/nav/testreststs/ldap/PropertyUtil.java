package no.nav.testreststs.ldap;

public class PropertyUtil {

    public static String getProperty(String key) {
        String val = System.getProperty(key);
        if (val == null) {
            val = System.getenv(key.toUpperCase().replace('.', '_').replace('-', '_'));
        }
        return val;
    }
}
