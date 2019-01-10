package no.nav.testreststs.restapi.naisjetty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class IsAliveServlet extends HttpServlet {

    private static final int HTTP_OK = 200;
    private static final int HTTP_ERROR = 500;

    private static final long serialVersionUID = 247032125328316762L;
    
    @Autowired
    private SelfTest selfTest;
    
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp){
        
        if (selfTest.isAlive()) {
            resp.setStatus(HTTP_OK);
        } else {
            resp.setStatus(HTTP_ERROR);
        }
    }
}
