package no.nav.testreststs.restapi;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import no.nav.testreststs.accesstoken.AccessTokenIssuer;


@Api(protocols = "https")
@Path("v1/sts") // restcontext satt i web.xml 
@Produces(MediaType.APPLICATION_JSON)
public class AccessTokenController {
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	AccessTokenIssuer issuer;
	
	@ApiOperation(value = "get OIDC token from username og password", authorizations = {@Authorization(value="basicAuth")})
	@ApiResponses(value = {
	            @ApiResponse(code = 200, message = "OK - Oidc token issued", response = AccessTokenResponse.class),
	            @ApiResponse(code = 400, message = "Bad request", response = ErrorResponse.class),
	            @ApiResponse(code = 401, message = "Unauthorized", response = ErrorResponse.class),
	            @ApiResponse(code = 500, message = "Internal server error, teknisk feil")	            
	    })
//	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/token") 
	@GET
	public Response getOidcToken(@Context SecurityContext securityContext, @Context HttpHeaders headers,  
			 @ApiParam(value = "", required = true, defaultValue = "client_credentials") @QueryParam("grant_type") String grantType,
			 @ApiParam(value = "", required = true, defaultValue = "openid") @QueryParam("scope") String scope) throws Exception {			
		if(grantType == null || !grantType.equalsIgnoreCase("client_credentials") ||
			scope == null || !scope.equalsIgnoreCase("openid")) {		
			log.debug("Invalid request, grant_type= " + grantType + " scope " + scope);			
			return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse("invalid_request")).build();
		}
		String username = null;
		try {
			username = ControllerUtil.getUserName(securityContext);			
		}
		catch(Exception e) {
			log.error("Error, invalid_client: " + e.getMessage());
			return Response.status(Response.Status.UNAUTHORIZED).entity(new ErrorResponse("invalid_client")).build();
		}
		log.debug("Issue oidc token for: " + username);
		SignedJWT oidcToken = null;
		try {
			oidcToken = issuer.issueToken(username);			
		}
		catch(Exception e) {
			log.error("Error: " + e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Internal server error, teknisk feil").build();
		}
		return Response.status(Response.Status.OK).entity(new AccessTokenResponse(oidcToken)).header("Cache-Control", "no-store").header("Pragma", "no-cache").build();				
	}
	
	@ApiOperation(value = "get jwks (public keys)")
	@Path("jwks")  
	@GET
	public Response getKeys() throws Exception {		
		JWKSet jwks = issuer.getPublicJWKSet();		
		return Response.status(Response.Status.OK).entity(jwks.toJSONObject()).header("Cache-Control", "no-store").header("Pragma", "no-cache").build();
	}
	
	
	@ApiOperation(hidden=true, value = "")
	@Path("/isAlive")  
	@GET
	public Response ping() throws Exception {
		return Response.status(Response.Status.OK).build();
	}
	
	@ApiOperation(hidden=true, value = "")
	@Path("/selftest")  
	@GET
	public Response isAlive() throws Exception {		
		// sjekk et lite kall til databasen
		if(issuer.selfTest()) {
			return Response.status(Response.Status.OK).build();
		}
		return Response.status(Response.Status.BAD_GATEWAY).build();
	}

	@ApiOperation(hidden=false, value = "get configuration info")
	@Path("/.well-known/openid-configuration")  
	@GET
	public Response getConfiguration() throws Exception {
		return Response.status(Response.Status.OK).entity(new ConfigurationResponse(issuer.getIssuer(), "/rest/v1/sts/token", null, "/rest/v1/sts/jwks")).build();
	}
	
	
	@ApiOperation(hidden=true, value = "exchange DIFI OIDC token to OIDC token issued by this issuer", authorizations = {@Authorization(value="basicAuth")})
	@Path("/difitoken/exchange") 
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@POST
	public Response exchangeDifiOidcToken(@Context SecurityContext securityContext, @Context HttpHeaders headers,  
											@HeaderParam("token") String difiToken) throws Exception {			
		log.debug("Exchange difi token to oidc token");
		try {			
			ControllerUtil.getUserName(securityContext);					
		}
		catch(Exception e) {
			log.error("Error, invalid_client: " + e.getMessage());	
			return Response.status(Response.Status.UNAUTHORIZED).entity(new ErrorResponse("invalid_client")).build();
		}
		if(difiToken == null || difiToken.isEmpty()) {
			log.error("Exchange difi token called with null or empty difi token");
			return Response.status(Response.Status.BAD_REQUEST).entity("Bad request, difi token is null or empty" ).build(); 
		}
		SignedJWT oidcToken;
		try {			
			oidcToken = issuer.exchangeDifiTokenToOidc(difiToken);			
		} catch(Exception e) {
			log.error("Failed to exchange difi oidc token to oidc token: " + e.getMessage());
			return Response.status(Response.Status.BAD_REQUEST).entity("Exchangedifi failed " + e.getMessage()).build();
		}
		return Response.status(Response.Status.OK).entity(new AccessTokenResponse(oidcToken)).header("Cache-Control", "no-store").header("Pragma", "no-cache").build();
	} 
}



