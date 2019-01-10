package no.nav.testreststs.restapi;

import java.util.Base64;
import java.util.List;

public class BasicAuthHeaderUtil {

    public static String getUserName(List<String> basicAuthHeaders) {
        try {
            return getCredentials(basicAuthHeaders)[0];
        } catch (Exception e) {
            throw new SecurityException("Ugyldig Autorization header i request, kan ikke hente ut username",e);
        }
    }

    public static String getPassword(List<String> basicAuthHeaders) {
        try {
            return getCredentials(basicAuthHeaders)[1];
        } catch (Exception e) {
            throw new SecurityException("Ugyldig Autorization header i request, kan ikke hente ut passord",e);
        }
    }

	private static String[] getCredentials(List<String> basicAuthHeaders) {
		String authHeader = basicAuthHeaders.get(0);
		String encodedString = authHeader.substring("Basic".length()).trim();
		String decodedString = decode(encodedString);
		String[] credentials = decodedString.split(":");
		return credentials;
	}

    private static String decode(String s) {
        return new String(Base64.getDecoder().decode(s));  
    }
}
