package no.nav.testreststs.restapi.naisjetty;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.jndi.Resource;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.springframework.web.context.ContextLoaderListener;

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
public class NaisJarJettyServer {

	static {System.setProperty("logback.configurationFile", NaisJarJettyServer.class.getResource(Loggback.setLogbackType()).toString());}

    private static final String JETTY_WEB_XML = "/webapp/WEB-INF/web.xml";
    private static final String JETTY_LOCATION_BASE = "/webapp";
    private static final String CREDENTIAL_PROPS = "/webapp/WEB-INF/login.conf";

    private static final String JETTY_EMBEDDED_ATTRIBUTES_NAME = "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern";
    private static final String JETTY_EMBEDDED_ATTRIBUTES_VALUE = "^.*cxf-.*.jar$|^.*webapp-.*.jar$";


    private static final Configuration[] CONFIGURATIONS = new Configuration[]{
            new WebXmlConfiguration(),
            new AnnotationConfiguration(),
            new WebInfConfiguration(),
            new PlusConfiguration(),
            new EnvConfiguration(),
    };

    public static void main(String[] args) throws Exception{
        LocalEnvironment.checkIfLocalSetting(args);
        Server server = new Server();
        setTheConnectors(server);
        setupSikkerhetVariabler();

        try{
            setContextHandler(server);
            startJetty(server);
        } catch (Exception e) {
            throw new Exception("Exception when running Jetty Embedded Server setup: " + e);
        }
    }

    private static void setTheConnectors(Server server) {
        ServerConnector serverConnector = new ServerConnector(server);
        serverConnector.setHost(JettyHandler.HOST);
        serverConnector.setPort(JettyHandler.PORT);
        serverConnector.setIdleTimeout(JettyHandler.IDLE_TIMEOUT);
        server.addConnector(serverConnector);
    }

    private static void setupSikkerhetVariabler() {     
        System.setProperty("java.security.auth.login.config", NaisJarJettyServer.class.getResource(CREDENTIAL_PROPS).toString());
    }

    private static void setContextHandler(Server server) throws Exception{
        WebAppContext webapp = initContext();
        webapp = new JettySecurity().jettySikkerhetConfig(webapp);
        webapp.setParentLoaderPriority(true);
        server.setHandler(webapp);
    }

    private static WebAppContext initContext(){
        WebAppContext webAppContext = createContext();
        webAppContext.setAttribute(JETTY_EMBEDDED_ATTRIBUTES_NAME, JETTY_EMBEDDED_ATTRIBUTES_VALUE);
        webAppContext.setConfigurations(CONFIGURATIONS);
        return webAppContext;
    }

    private static WebAppContext createContext() {
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setContextPath("/");
        setServletContext(webAppContext);
        webAppContext.setResourceBase(NaisJarJettyServer.class.getResource(JETTY_LOCATION_BASE).toString());
        webAppContext.setDescriptor(NaisJarJettyServer.class.getResource(JETTY_WEB_XML).toString());
        return webAppContext;
    }

    private static void setServletContext(WebAppContext webAppContext) {
//        webAppContext.addServlet(MetricsServlet.class, "/prometheus");
        webAppContext.addServlet(IsAliveServlet.class, "/isAlive");
        webAppContext.addServlet(SelfTestServlet.class, "/isReady");
        webAppContext.addEventListener(new ContextLoaderListener());
        DefaultExports.initialize();
    }

    private static void startJetty(Server server) throws Exception {
        server.start();
        server.join();
    }
}