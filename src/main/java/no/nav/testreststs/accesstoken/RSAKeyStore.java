package no.nav.testreststs.accesstoken;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;


public class RSAKeyStore {
	private RSAKey jwk;
	private JWKSet jwks;
	
	
	public RSAKeyStore() throws NoSuchAlgorithmException {
		// Generate the RSA key pair
		KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
		gen.initialize(2048); // Set the desired key length
		KeyPair keyPair = gen.generateKeyPair();
	
		// Convert to JWK format
		jwk = new RSAKey.Builder((RSAPublicKey)keyPair.getPublic())
		    .privateKey((RSAPrivateKey)keyPair.getPrivate())
		    .keyID(UUID.randomUUID().toString()) // Give the key some ID (optional)
		    .keyUse(KeyUse.SIGNATURE)
		    .algorithm(AccessTokenIssuer.OIDC_SIGNINGALG)
		    .build();
		List<JWK> jwkList = new ArrayList<>();
		jwkList.add(jwk);
		jwks = new JWKSet(jwkList);
	}
	
	public RSAKey getCurrentRSAKey() {
		return (RSAKey) jwk;
	}
	
	public JWKSet getPublicJWKSet() {			
		return (jwks != null ? jwks.toPublicJWKSet() : null);
	}

	public Boolean selfTest() {
		return (jwks != null);
	}

}
