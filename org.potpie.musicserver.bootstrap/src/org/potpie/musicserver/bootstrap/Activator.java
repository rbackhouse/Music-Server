package org.potpie.musicserver.bootstrap;

import javax.servlet.ServletException;

import org.dojotoolkit.compressor.JSCompressorFactory;
import org.dojotoolkit.optimizer.JSOptimizerFactory;
import org.dojotoolkit.optimizer.servlet.osgi.ZazlServicesTracker;
import org.eclipse.equinox.http.registry.HttpContextExtensionService;
import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.potpie.musicserver.service.MusicServerServlet;

public class Activator implements BundleActivator, ZazlServicesTracker.ZazlServicesListener {
	private ZazlServicesTracker zazlServicesTracker = null;
	private ExtendedHttpService httpService = null;
	
	public Activator() {
		zazlServicesTracker = new ZazlServicesTracker("org.potpie.musicserver.httpcontext");
	}
	
	public void start(BundleContext context) throws Exception {
		zazlServicesTracker.addListener(this);
		zazlServicesTracker.start(context);
	}

	public void stop(BundleContext context) throws Exception {
		httpService.unregister("/service");
		zazlServicesTracker.stop();
		zazlServicesTracker.removeListener(this);
	}
	
	public void servicesAvailable(ExtendedHttpService httpService, 
            					  HttpContextExtensionService httpContextExtensionService,
            					  JSCompressorFactory jsCompressorFactory,
            					  JSOptimizerFactory jsOptimizerFactory,
            					  HttpContext httpContext) {
		this.httpService = httpService;
    	String root = System.getProperty("root");
    	String storageDir = System.getProperty("storageDir");
		try {
			httpService.registerServlet("/service", new MusicServerServlet(root, storageDir, false), null, httpContext);
		} catch (ServletException e) {
			e.printStackTrace();
		} catch (NamespaceException e) {
			e.printStackTrace();
		}
	}
}
