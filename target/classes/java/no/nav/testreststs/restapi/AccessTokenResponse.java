package no.nav.testreststs.restapi;



import java.text.ParseException;

import com.nimbusds.jwt.SignedJWT;

public class AccessTokenResponse {
	private String access_token;
	private String token_type;
	private Long expires_in;
	
	public AccessTokenResponse() {
		
	}
	
	public AccessTokenResponse(SignedJWT oidcToken) throws ParseException {
		this.access_token = oidcToken.serialize();
		this.token_type = "Bearer";
		this.expires_in = (oidcToken.getJWTClaimsSet().getExpirationTime().getTime() - oidcToken.getJWTClaimsSet().getIssueTime().getTime())/1000;
	}

	public String getAccess_token() {
		return access_token;
	}

	public String getToken_type() {
		return token_type;
	}

	public Long getExpires_in() {
		return expires_in;
	}
	
}
