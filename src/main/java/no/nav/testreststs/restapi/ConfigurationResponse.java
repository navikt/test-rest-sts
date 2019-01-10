package no.nav.testreststs.restapi;

public class ConfigurationResponse {
	private String issuer;
	private String token_endpoint;
	private String exchange_token_endpoint;
	private String jwks_uri;
	private String[] subject_types_supported = {"public"};

	public ConfigurationResponse(String issuer, String token_path, String exchange_path, String jwks_path) {
		this.issuer = issuer;
		this.token_endpoint = issuer + token_path;
		this.exchange_token_endpoint = issuer + exchange_path;
		this.jwks_uri = issuer + jwks_path;
	}
	
	public String getIssuer() {
		return issuer;
	}
	public String getToken_endpoint() {
		return token_endpoint;
	}
	public String getExchange_token_endpoint() {
		return exchange_token_endpoint;
	}
	public String getJwks_uri() {
		return jwks_uri;
	}

	public String[] getSubject_types_supported() {
		return subject_types_supported;
	}
	 
}
