package no.nav.testreststs.accesstoken;

import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSHeader.Builder;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;


public class OidcObject {
	public static String VERSION_CLAIM = "ver";
	public static String CONSUMERID_CLAIM = "cid";
	public static String AUTHLEVEL_CLAIM = "acr";
	public static String RESOURCETYPE_CLAIM = "identType";
	public static String AUTHTIME_CLAIM = "auth_time";
	public static String AZP_CLAIM = "azp";
	public static String UTY_CLAIM = "uty";
	public static String TRACKING_CLAIM = "auditTrackingId";
	
	private String issuer;
	private String version;
	private String id;
	private String subject;	
	private List<String> audience;
	private String azp;
	private String authLevel;
	private String consumerId;
	private String resourceType;
	private Date notBeforeTime;
	private Date issueTime;
	private Date expirationTime;
	private Date authTime;
	private String auditTrackingId;
	private SignedJWT signedJWT;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public OidcObject(ZonedDateTime issueTime, long duration) {
		this.id = UUID.randomUUID().toString();
		this.issueTime = toDate(issueTime);
		this.authTime = this.issueTime;
		this.notBeforeTime = this.issueTime;
		this.expirationTime = toDate(issueTime.plusSeconds(duration));
	}
	
	public OidcObject(ZonedDateTime issueTime, ZonedDateTime expirationTime) {
		this.id = UUID.randomUUID().toString();
		this.issueTime = toDate(issueTime);
		this.authTime = this.issueTime;
		this.notBeforeTime = this.issueTime;
		this.expirationTime = toDate(expirationTime);
	}
	
	public OidcObject(String oidcToken) throws ParseException {
		// parse token
		signedJWT = SignedJWT.parse(oidcToken);
		JWTClaimsSet claimSet = signedJWT.getJWTClaimsSet();		
		
		// get claims
		issuer = claimSet.getIssuer();
		version = claimSet.getStringClaim(VERSION_CLAIM);
		id = claimSet.getJWTID();
		subject = claimSet.getSubject();
		audience = claimSet.getAudience();
		azp = claimSet.getStringClaim(AZP_CLAIM);
		authLevel = claimSet.getStringClaim(AUTHLEVEL_CLAIM);
		consumerId = claimSet.getStringClaim(CONSUMERID_CLAIM);
		resourceType = claimSet.getStringClaim(RESOURCETYPE_CLAIM);
		notBeforeTime = claimSet.getNotBeforeTime();
		issueTime = claimSet.getIssueTime();
		expirationTime = claimSet.getExpirationTime();
		authTime = claimSet.getDateClaim(AUTHTIME_CLAIM);
		auditTrackingId = claimSet.getStringClaim(TRACKING_CLAIM);
	}
	

	
	public SignedJWT getSignedToken(RSAKey key, JWSAlgorithm alg) {
		signedJWT = getSignedJWT(getJWTClaimsSet(), key, alg);
		return signedJWT;
	}
	

	private JWTClaimsSet getJWTClaimsSet() {
		
		return new JWTClaimsSet.Builder()
				.issuer(issuer)
				.claim(VERSION_CLAIM, version)
				.jwtID(id)				
				.subject(subject)
				.audience(audience)				     	
				.claim(AUTHTIME_CLAIM, authTime)
				.notBeforeTime(notBeforeTime)
				.issueTime(issueTime)
				.expirationTime(expirationTime)
				.claim(AZP_CLAIM, azp)			
				.claim(RESOURCETYPE_CLAIM, resourceType) 					
				.build();
	}
	   	
 
	private SignedJWT getSignedJWT(JWTClaimsSet claimsSet, RSAKey key, JWSAlgorithm alg) {
		log.debug("getSignedToken SignedJWT");
        try {
            JWSHeader.Builder header = new Builder(alg)
                    						.keyID(key.getKeyID())
                    						.type(JOSEObjectType.JWT);
            
            SignedJWT signedJWT = new SignedJWT(header.build(), claimsSet);
            JWSSigner signer = new RSASSASigner(key.toPrivateKey());
            signedJWT.sign(signer);
            return signedJWT;
        } 
        catch (JOSEException e) {
            throw new RuntimeException(e);
        }
	}
	
	public Object getClaim(String claimName) throws ParseException {		
		return (signedJWT != null ? signedJWT.getJWTClaimsSet().getClaims().get(claimName) : null);
	}
	
	public SignedJWT getSignedTokenCopyAndAddClaimsFrom(OidcObject copyOidc, List<String> copyClaimsList, RSAKey key, JWSAlgorithm alg) throws ParseException {
		// copy claims in copyClaimsList from copyOidc to this and add extra claims from copyOidc to this
		Map<String, Object> copyClaims = copyOidc.signedJWT.getJWTClaimsSet().getClaims();
		Map<String, Object> newClaims = getJWTClaimsSet().getClaims();
		JWTClaimsSet.Builder cBuilder = new JWTClaimsSet.Builder();
		for(String cName : newClaims.keySet()) {
			if(copyClaimsList.contains(cName)) {
				cBuilder.claim(cName, copyClaims.get(cName));
			} else {
				cBuilder.claim(cName, newClaims.get(cName));
			}
		}
		cBuilder.claim(TRACKING_CLAIM, getAuditTrackingId());
		
		for(String cName : copyClaims.keySet()) {
			if(!newClaims.containsKey(cName)) {
				cBuilder.claim(cName, copyClaims.get(cName));
			}
		}		
		// generate signedJWT
		return getSignedJWT(cBuilder.build(), key, alg);
	}
	
	
	public String getIssuer() {
		return issuer;
	}

	public String getVersion() {
		return version;
	}

	public String getId() {
		return id;
	}

	public String getSubject() {
		return subject;
	}

	public List<String> getAudience() {
		return audience;
	}

	public String getAzp() {
		return azp;
	}

	public String getAuthLevel() {
		return authLevel;
	}

	public String getConsumerId() {
		return consumerId;
	}

	public String getResourceType() {
		return resourceType;
	}

	public Date getNotBeforeTime() {
		return notBeforeTime;
	}

	public Date getIssueTime() {
		return issueTime;
	}

	public Date getExpirationTime() {
		return expirationTime;
	}

	public String getKeyId() {
		return (signedJWT != null ? signedJWT.getHeader().getKeyID() : null);
	}
	
	public String getAuditTrackingId() {
		return auditTrackingId;
	}

	public void setAuditTrackingId(String auditTrackingId) {
		this.auditTrackingId = auditTrackingId;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	
	public void setId(String id) {
		this.id = id;
	}

	public void setAudience(List<String> audience) {
		this.audience = audience;
	}

	public void setAudience(String audience) {
		this.audience = new ArrayList<>();
		this.audience.add(audience);
	}
	
	public void setAudience(String aud1, String aud2) {
		this.audience = new ArrayList<>();
		this.audience.add(aud1);
		this.audience.add(aud2);
	}
	
	public void setAzp(String azp) {
		this.azp = azp;
	}

	public void setAuthLevel(String authLevel) {
		this.authLevel = authLevel;
	}

	public void setConsumerId(String consumerId) {
		this.consumerId = consumerId;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}
	

	public static Date toDate(ZonedDateTime d) {
		return Date.from(d.toInstant());
	}

	
	
}
