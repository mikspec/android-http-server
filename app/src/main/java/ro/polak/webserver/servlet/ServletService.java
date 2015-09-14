package ro.polak.webserver.servlet;

 /**
 * Little servlet service for loading and rolling servlets
 *
 * @author Piotr Polak piotr [at] polak [dot] ro
 * @version 201509
 * @since 200802
 */
public class ServletService implements IServletServiceDriver {

    private IServletServiceDriver driver;

    /**
     * Default constructor
     * @param driver
     */
    public ServletService(IServletServiceDriver driver) {
        this.driver = driver;
    }

    /**
     * Loads requested little servlet
     *
     * @param servletPath the path of the little servlet (requested URI)
     * @return true if little servlet found and loaded
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassCastException
     */
    public boolean loadServlet(String servletPath) throws InstantiationException, IllegalAccessException, ClassCastException {
        return driver.loadServlet(servletPath);
    }

    /**
     * Runs (starts) servlet
     *
     * @param request  http request
     * @param response http response
     */
    public void rollServlet(HTTPRequest request, HTTPResponse response) {
        driver.rollServlet(request, response);
    }
}
