package no.nav.testreststs.accesstoken;

import static org.junit.Assert.assertTrue;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import no.nav.testreststs.accesstoken.AccessTokenIssuer;
import no.nav.testreststs.accesstoken.OidcObject;
import no.nav.testreststs.accesstoken.AccessTokenIssuer.IdentType;
import no.nav.testreststs.restapi.AccessTokenResponse;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test-context.xml"})

public class AccessTokenIssuerTest {
	@Autowired 
	AccessTokenIssuer issuer;
	
	private final String ACCESSTOKEN_TYPE = "bearer";
		
	@BeforeClass
	public static void setupProperties() {
		 System.setProperty("test.rest.sts.issuerUrl", "https://test-rest-sts.nais.preprod.local");  
	}
	
	@Before
	public void setUpTests() {
		// fikk problemer med systemproperty med bindestrek. Disse blir ikke satt før etter bean creation ved nais bygg...? Må derfor sette denne eksplisitt for at testen skal kunne kjøres
		issuer.setIssuer("https://test-rest-sts.nais.preprod.local"); 
	}
	
	
	@Test	
	public void issueAndValidateOidcToken() throws NoSuchAlgorithmException, JOSEException, ParseException {		
		String username = "testuser";
		SignedJWT token = issuer.issueToken(username);		
		
		assertTrue(token != null);		
		JWTClaimsSet jwt = token.getJWTClaimsSet();
		
		
		assertTrue(jwt.getSubject().equals(username));		
		assertTrue(jwt.getClaim(OidcObject.AZP_CLAIM).equals(username));
		assertTrue(jwt.getIssuer().equals(issuer.getIssuer()));
		assertTrue(jwt.getStringClaim(OidcObject.VERSION_CLAIM).equals(AccessTokenIssuer.OIDC_VERSION));
		assertTrue(jwt.getJWTID() != null);
		assertTrue(jwt.getClaim(OidcObject.RESOURCETYPE_CLAIM).equals(IdentType.INTERNBRUKER.name));

		// sjekk audience
		List<String> l = token.getJWTClaimsSet().getAudience();
		assertTrue(l.size() == 2);
		assertTrue(l.get(0).equals(username));
		assertTrue(l.get(1).equals("preprod.local"));	

		// sjekk time settings
		assertTrue(jwt.getNotBeforeTime().compareTo(jwt.getIssueTime()) == 0);
		assertTrue((jwt.getExpirationTime().getTime() - jwt.getIssueTime().getTime())/1000 == AccessTokenIssuer.OIDC_DURATION_TIME);		
	    	                			

		// Test response
		AccessTokenResponse response = new AccessTokenResponse(token);
		assertTrue(response.getExpires_in() == AccessTokenIssuer.OIDC_DURATION_TIME);
		assertTrue(response.getToken_type().equalsIgnoreCase(ACCESSTOKEN_TYPE));
		
	 }
		

	@Test
	public void getJwkKeys() throws NoSuchAlgorithmException, JOSEException {
		SignedJWT token = issuer.issueToken("testuser");
		JWKSet jwks = issuer.getPublicJWKSet();
		List<JWK> jList = jwks.getKeys();
		assertTrue(jList.size() == 1); 		
	}
	

	@Test
	public void getIdentTypeTest() {
		assertTrue(issuer.getIdentType("testuser").equalsIgnoreCase(IdentType.INTERNBRUKER.name));
		assertTrue(issuer.getIdentType("srvTest").equalsIgnoreCase(IdentType.SYSTEMRESSURS.name));
		assertTrue(issuer.getIdentType("991825827").equalsIgnoreCase(IdentType.SAMHANDLER.name));
	}
		
	
	@Test
	public void exchangeDifiTokenToOidcTest() throws ParseException, NoSuchAlgorithmException, JOSEException {
		
		String difiToken = getDifiOidcToken();
		JWTClaimsSet difiJwt = SignedJWT.parse(difiToken).getJWTClaimsSet();
		String subject = (String) difiJwt.getClaim("client_orgno");
		Date issueAt = difiJwt.getIssueTime();
		
		// exchange token
		SignedJWT oidcToken = issuer.exchangeDifiTokenToOidc(difiToken);
		//System.out.println("oidcToken: " + base64DecodeString(oidcToken.serialize()));
			
		// check issued token
		JWTClaimsSet jwt = oidcToken.getJWTClaimsSet();
			
		assertTrue(jwt.getSubject().equals(subject));		
		assertTrue(jwt.getClaim(OidcObject.AZP_CLAIM).equals(subject));
		assertTrue(jwt.getIssuer().equals(issuer.getIssuer()));
		assertTrue(jwt.getStringClaim(OidcObject.VERSION_CLAIM).equals(AccessTokenIssuer.OIDC_VERSION));
		assertTrue(jwt.getJWTID() != null);
		assertTrue(jwt.getClaim(OidcObject.RESOURCETYPE_CLAIM).equals(IdentType.SAMHANDLER.name));
		assertTrue(jwt.getStringClaim(OidcObject.TRACKING_CLAIM).equals(difiJwt.getJWTID()));
		
		//System.out.println("### difi aud: " + difiJwt.getAudience() + " claim: " + difiJwt.getClaim("aud"));
		//System.out.println("### nav aud " + jwt.getAudience() + " claim: " + jwt.getClaim("aud"));
		assertTrue(jwt.getAudience().size() == difiJwt.getAudience().size());
		for(int i = 0; i < jwt.getAudience().size(); i++) {
			assertTrue(jwt.getAudience().get(i).equals(difiJwt.getAudience().get(i)));
		}
			
		System.out.println("jwt: " + jwt);
		System.out.println("issueTime: " + jwt.getIssueTime());
		
		System.out.println("notBefore " + jwt.getNotBeforeTime());
		//System.out.println("authtime " + jwt.getLongClaim(OidcObject.AUTHTIME_CLAIM));
		System.out.println("exptime " + jwt.getExpirationTime());	
	}
	
	
	@Test
	@Ignore
	public void decodeBase64_2() throws ParseException {
		String difiToken = "eyJraWQiOiJtcVQ1QTNMT1NJSGJwS3JzY2IzRUhHcnItV0lGUmZMZGFxWl81SjlHUjlzIiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJvaWRjX25hdl9wb3J0YWxfdGVzdGtvbnN1bWVudCIsInNjb3BlIjoibmF2OnRlc3RhcGkiLCJpc3MiOiJodHRwczpcL1wvb2lkYy12ZXIyLmRpZmkubm9cL2lkcG9ydGVuLW9pZGMtcHJvdmlkZXJcLyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJleHAiOjE1NDUwNDY0NTQsImlhdCI6MTU0NTA0NjMzNCwiY2xpZW50X29yZ25vIjoiODg5NjQwNzgyIiwianRpIjoiZHEyakwzaFA1cU13Tno5emRCM3B3eHdoTlRSbnNZbzBBUGdNVmxHVF9BOD0ifQ.USA6-syucnSlZzMjp-8DGxRwMy7p-QK3v4tszXNxTJXR-Kb5sIRWaxH0agjAv2JX2-Ckc_KaK9E4ae3_G9fJSyVg0blOpoeiux0FPYZa2d9cwR_DgTVjRwYG5u1H_UhAbnFZumCF9sg7aOnGy3msVqLMv1M5dhGbqRAnp34-YRUeaRw7xxMsAStypcobKUfyn8B4qMD7zUfu5H1FfOJtjugo3I5WPTkqzs4N4HRGCPlbbMa7zV5y9uH4gcqWd2GzLfwrrFGKHk1Jp7EnfWwz4lotpL99LolSocXwwMpV0rRJAzf2AMILmPhLT9gyZGgC7vG_nmo7L6Iwiz3FD4qCPQ";
		String token1 = "eyJraWQiOiJlMTMwZjgzNS0wYjdhLTQxZjUtYWY0Ni1iMmE4NWJjNGQ0MWYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI4ODk2NDA3ODIiLCJ2ZXIiOiIxLjAiLCJhdWRpdFRyYWNraW5nSWQiOiJkcTJqTDNoUDVxTXdOejl6ZEIzcHd4d2hOVFJuc1lvMEFQZ01WbEdUX0E4PSIsImlzcyI6Imh0dHBzOlwvXC90ZXN0LXJlc3Qtc3RzLm5haXMucHJlcHJvZC5sb2NhbCIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJhdWQiOiJvaWRjX25hdl9wb3J0YWxfdGVzdGtvbnN1bWVudCIsIm5iZiI6MTU0NzAyODIyMSwiYXpwIjoiODg5NjQwNzgyIiwiaWRlbnRUeXBlIjoiU2FtaGFuZGxlciIsImF1dGhfdGltZSI6MTU0NzAyODIyMSwic2NvcGUiOiJuYXY6dGVzdGFwaSIsImV4cCI6MTU0NzAzMTgyMSwiaWF0IjoxNTQ3MDI4MjIxLCJqdGkiOiI1Y2MyODgyMy05NmUzLTRlN2EtYjAwOC05ZTFjZjJlMGVlY2UiLCJjbGllbnRfb3Jnbm8iOiI4ODk2NDA3ODIifQ.hBn0GOMKa7rFzmKfngsdmpziDOaZrxbwlKsB4P9Agttao71TvKQtYsoGJZ06j5kTatUApgP1nwK3GKtFIeSm7DPFjR26ml_PdDgSkdfpD7Hb6p7haV3WeOiWSJ4ac2wn6zb32Vw2R1wxsR0RrmKudu-7eIsayZg0g3qzeB4tsTzzLKDYWA_9lCjTYieEYd2o7Ao0Vilb6rrDxFmqqdSXg3b3iByZyzEcKrNouLk8ggxGiBB-l_I3rKJ3z_aXMbJYDLi01P4GfFVU7tKYka97aAl1xY2FpRTrNCQEo1Vb3E_y7O53-FLY_hrSALlZ7yBEkmp3dlq3hPuBiL8IO-GBGA";
		JWTClaimsSet difiJwt = SignedJWT.parse(difiToken).getJWTClaimsSet();
		System.out.println("difitoken: " + difiJwt.toString());
		System.out.println("exchangetoken1: " + SignedJWT.parse(token1).getJWTClaimsSet().toString());
	}
		
	private static ZonedDateTime toZonedDateTime(Date d) {
		return ZonedDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
	}
	
	private String getDifiOidcToken() {
		return "eyJraWQiOiJtcVQ1QTNMT1NJSGJwS3JzY2IzRUhHcnItV0lGUmZMZGFxWl81SjlHUjlzIiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJvaWRjX25hdl9wb3J0YWxfdGVzdGtvbnN1bWVudCIsInNjb3BlIjoibmF2OnRlc3RhcGkiLCJpc3MiOiJodHRwczpcL1wvb2lkYy12ZXIyLmRpZmkubm9cL2lkcG9ydGVuLW9pZGMtcHJvdmlkZXJcLyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJleHAiOjE1NDQwODc0NjYsImlhdCI6MTU0NDA4NzM0NiwiY2xpZW50X29yZ25vIjoiODg5NjQwNzgyIiwianRpIjoibHFSQkRsYTNhbDFHUWRHcHpLaHE0MDNRbFVHMkdnYjB2RzFIMnFFS1ZDST0ifQ.NKFNsdO1zQTwDw_dmFBTKfNpqC7BiqFpQp4e6mTlTTP7r4efo7qPjlkgTFWl_v5RiSD8esBkCKgeeZrDQ1PAAM8VIja9H4vyNXBYwOvbzdxLdCgzwKF1kmkx6l0Cgw3GC8HFHX1UPJ7-rpyLst8V857m6QtC6FqjkIDcv4F249PwhLWhfmGUqwGpRZ3frAJ0SYljehCN-A3qWXU_xaibTmzLtRK56VlzxB2sY9kdKmWIW8yppSZKeroNNeFEsU7WW90jxR9d9EFvPgweSZrKWk3iAS2HUA-pxkctSb64nUAOVyB3ywCMftq0e_TvGboxYB4DcY0cP5ReZlE0bzZftg";
	}
	
}

