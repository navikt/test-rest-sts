package no.nav.testreststs.ldap;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.eclipse.jetty.jaas.callback.ObjectCallback;
import org.eclipse.jetty.jaas.spi.AbstractLoginModule;
import org.eclipse.jetty.jaas.spi.UserInfo;
import org.eclipse.jetty.util.security.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LdapLoginModule extends AbstractLoginModule {
    private static final Logger log = LoggerFactory.getLogger(LdapLoginModule.class);
    private static Hashtable<String, String> env = new Hashtable<>();

    private static final String contextFactory = "com.sun.jndi.ldap.LdapCtxFactory";
    
    private static String[] ldapSearchBase = {PropertyUtil.getProperty("ldap.serviceuser.basedn"), "OU=ApplAccounts," + PropertyUtil.getProperty("ldap.serviceuser.basedn")};
    private String password;

    public LdapLoginModule() {
    	
    }

    @Override
    public void initialize(Subject subject,
                           CallbackHandler callbackHandler,
                           Map sharedState,
                           Map options) {
        super.initialize(subject, callbackHandler, sharedState, options);
//        this.initLdap = new InitLdap();
    }

    @Override
    public boolean commit() throws LoginException {
        return super.commit();
    }

    @Override
    public boolean abort() throws LoginException {
        return super.abort();
    }

    @Override
    public boolean login() throws LoginException {
    	
        try {
            if (getCallbackHandler() == null) {
                log.error("No callback handler");
                throw new LoginException("No callback handler");
            }

            boolean authed;
            Callback[] callbacks = configureCallbacks(); 
            getCallbackHandler().handle(callbacks);
 
            String webUserName = ((NameCallback) callbacks[0]).getName();
            Object webCredential = ((ObjectCallback) callbacks[1]).getObject();

            if (webUserName == null || webCredential == null) {
                setAuthenticated(false);
                return isAuthenticated();
            }          
            
            password =  (webCredential instanceof String) ? (String) webCredential : decryptUserPass(webCredential);
            UserInfo userInfo = getUserInfo(webUserName);

            if (userInfo == null) {
                setAuthenticated(false);
                return false;
            }

            setCurrentUser(new JAASUserInfo(userInfo));

            if (webCredential instanceof String){
                authed = credentialLogin(Credential.getCredential((String) webCredential));
            }else {
                authed = credentialLogin(webCredential);
            }

            if(authed){
                // Update roles only if user is found.
                getCurrentUser().fetchRoles();
            }
            return authed;

        } catch (UnsupportedCallbackException e) {
            log.error("Error obtaining callback information: " + e);
            throw new LoginException("Error obtaining callback information");
        } catch (Exception e) {
            log.error("Error obtaining user info: " + Exception.class);
            throw new LoginException("Error obtaining user info." + e);
        }
    }

    @Override
    public UserInfo getUserInfo(String username) throws Exception {    	
        String userinfo = LdapLoginModule.bindUser(username, password);
        return (userinfo == null ? null : new UserInfo(userinfo, Credential.getCredential(password)));
    }
    
    static public String bindUser(String username, String password) throws LoginException {
//    	log.debug("Ldap oppslag for user: " + username);
    	String user = null;
    	for(int i = 0; i < LdapLoginModule.ldapSearchBase.length; i++) {
        	user = getCredentials(username, password, LdapLoginModule.ldapSearchBase[i]);        	
        	if(user != null) {
        		break;
        	}
        }
    	if(user == null) {
    		 log.error("Ldap search failed, wrong username or password (user = " + username);
    	}
    	return user;
    }

    private boolean credentialLogin(Object webCredential){
        setAuthenticated(getCurrentUser().checkCredential(webCredential));
        return isAuthenticated();
    }


    private String decryptUserPass(Object passord){
        String s = Arrays.toString(((char[]) passord));
        s = s.substring(1 , s.length() -1);
        s = s.replaceAll(",", "").trim();
        String result = "";
        for (int i = 0; i < s.length(); i += 2){
            char rep = s.charAt(i);
            result += rep;
        }
        return result;
    }

    static public String getCredentials(String bruker, String passord, String ldapSearch) {    	
    	env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
        env.put(Context.PROVIDER_URL, PropertyUtil.getProperty("ldap.url"));
        env.put("com.sun.jndi.ldap.connect.pool", "true");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "CN=" + bruker + "," + ldapSearch);
        env.put(Context.SECURITY_CREDENTIALS, passord);

        try {
            new InitialLdapContext(env, new Control[0]);
            return bruker;
        } catch (Exception e) {
            return null;
        }        
    }
    
    
    
}
