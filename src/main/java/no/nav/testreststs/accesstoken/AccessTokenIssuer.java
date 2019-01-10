package no.nav.testreststs.accesstoken;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.crypto.KeySelector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;

import no.nav.testreststs.ldap.PropertyUtil;

// DENNE SKAL KUN VÆRE GYLDIG I PREPROD (skal aldri deployes til prod)

public class AccessTokenIssuer {	
	private String issuer = PropertyUtil.getProperty("test.rest.sts.issuerUrl");
	
	private String domain = getDomainFromIssuerURL(issuer);  
	public static long OIDC_DURATION_TIME = 60 * 60; // seconds
	public static String OIDC_VERSION = "1.0";
	public static JWSAlgorithm OIDC_SIGNINGALG = JWSAlgorithm.RS256;	
	public static long EXCHANGE_TOKEN_EXTENDED_TIME = 30; // seconds
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private RSAKeyStore keystore;
	
	public enum IdentType { 
			SYSTEMRESSURS ("Systemressurs"), 
			INTERNBRUKER ("InternBruker"),
			EKSTERNBRUKER("EksternBruker"),
			SAMHANDLER("Samhandler");
			
			public final String name;
			
			IdentType(String name) {
				this.name= name;
			}
	}	
	
	public SignedJWT issueToken(String username) throws NoSuchAlgorithmException, JOSEException {	
		log.debug("issueToken for " + username);
		if(username == null || username.isEmpty()) {
			throw new IllegalArgumentException("Failed to issue oidc token, username is null");
		}
		OidcObject oidcObj = new OidcObject(ZonedDateTime.now(), OIDC_DURATION_TIME);
		oidcObj.setSubject(username);
		oidcObj.setIssuer(getIssuer());
		oidcObj.setVersion(OIDC_VERSION);
		oidcObj.setAudience(username, getDomain());
		oidcObj.setAzp(username);
		oidcObj.setResourceType(getIdentType(username));
		
		return oidcObj.getSignedToken(keystore.getCurrentRSAKey(), OIDC_SIGNINGALG);
	}
	
	public SignedJWT exchangeDifiTokenToOidc(String difiToken) throws ParseException, NoSuchAlgorithmException, JOSEException {
		log.debug("exchangeDifiTokenToOidc");
		
		// validate difi token
		OidcObject difiOidcObj = new OidcObject(difiToken);
		
		// issue new oidc token
		OidcObject oidcObj = new OidcObject(ZonedDateTime.now(), OIDC_DURATION_TIME);
		String subject = (String) difiOidcObj.getClaim("client_orgno");
		oidcObj.setSubject(subject);
		oidcObj.setIssuer(getIssuer());
		oidcObj.setVersion(OIDC_VERSION);
		//oidcObj.setAudience(username, getDomain()); copy this from difi token instead
		oidcObj.setAzp(subject);
		oidcObj.setResourceType(getIdentType(subject));
		oidcObj.setAuditTrackingId(difiOidcObj.getId());
		List<String> copyClaims = Collections.singletonList("aud");
		
		return oidcObj.getSignedTokenCopyAndAddClaimsFrom(difiOidcObj, copyClaims, keystore.getCurrentRSAKey(), OIDC_SIGNINGALG);
	}

	public String getIssuer() {
		return issuer;
	}
		
	public void setIssuer(String issuer) { // for test 
		this.issuer = issuer;
		domain = getDomainFromIssuerURL(issuer);
	}
		
	public String getDomain() {
		if(!domain.contains("preprod")) {
			throw new RuntimeException("This is not a valid domain: " + domain);
		}
		return domain;
	}

	public Boolean selfTest() {
		return keystore.selfTest();
	}
	
	
	public JWKSet getPublicJWKSet() {
		return keystore.getPublicJWKSet();
	}
	
	
	public static String getDomainFromIssuerURL(String issuer) {
		String domainPrefix = "nais.";
		if(issuer == null || issuer.length() < domainPrefix.length()) {
			throw new IllegalArgumentException("Failed to find domain from issuerUrl: " + issuer);
		}
		return issuer.substring(issuer.indexOf(domainPrefix) + domainPrefix.length());
	}
			
	
	public static String getIdentType(String subject) { 
		if(subject.toLowerCase().startsWith("srv")) {
			return IdentType.SYSTEMRESSURS.name;
		}
		if(subject.length() == 9 && subject.matches("[0-9]+")) {
			return IdentType.SAMHANDLER.name;
		}
		return IdentType.INTERNBRUKER.name; 	
	}
		

	public static ZonedDateTime toZonedDateTime(Date d) {
		return ZonedDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
	}

}
