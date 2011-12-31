package org.potpie.musicserver.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dojotoolkit.compressor.JSCompressorFactory;
import org.dojotoolkit.optimizer.JSOptimizerFactory;
import org.dojotoolkit.optimizer.servlet.JSHandler;
import org.dojotoolkit.optimizer.servlet.JSServlet;
import org.dojotoolkit.server.util.osgi.OSGiResourceLoader;
import org.dojotoolkit.server.util.resource.ResourceLoader;
import org.dojotoolkit.server.util.rhino.RhinoClassLoader;
import org.eclipse.equinox.http.registry.HttpContextExtensionService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.potpie.musicserver.service.MusicServerServlet;

public class Activator implements BundleActivator {
    private HttpService httpService = null;
    private ServiceTracker httpServiceTracker = null;
	private ServiceTracker httpContextExtensionServiceTracker = null;
	private ServiceTracker jsCompressorFactoryTracker = null;
	private ServiceTracker jsOptimizerFactoryServiceTracker = null;
	private boolean servletsRegistered = false;
	private ServiceReference httpServiceReference = null;
	private HttpContextExtensionService httpContextExtensionService = null;
	private JSCompressorFactory jsCompressorFactory = null;
	private JSOptimizerFactory jsOptimizerFactory = null;

	private BundleContext context;

	public void start(BundleContext context) throws Exception {
		this.context = context;
		httpServiceTracker = new HttpServiceTracker(context);
		httpServiceTracker.open();
		httpContextExtensionServiceTracker = new HttpContextExtensionServiceTracker(context);
		httpContextExtensionServiceTracker.open();
		boolean useV8 = Boolean.valueOf(System.getProperty("V8", "false"));
		String compressorType = System.getProperty("compressorType");
		if (compressorType != null && !compressorType.equals("none")) {
			jsCompressorFactoryTracker = new JSCompressorFactoryServiceTracker(context, useV8, compressorType);
			jsCompressorFactoryTracker.open();
		}
		jsOptimizerFactoryServiceTracker = new JSOptimizerFactoryServiceTracker(context, useV8, System.getProperty("jsHandlerType"));
		jsOptimizerFactoryServiceTracker.open();
	}

	public void stop(BundleContext bundleContext) throws Exception {
		httpContextExtensionServiceTracker.close();
		httpContextExtensionServiceTracker = null;
		httpServiceTracker.close();
		httpServiceTracker = null;
		if (jsCompressorFactoryTracker != null) {
			jsCompressorFactoryTracker.close();
			jsCompressorFactoryTracker = null;
		}
		jsOptimizerFactoryServiceTracker.close();
		jsOptimizerFactoryServiceTracker = null;
		this.context = null;
	}

	public Object addingService(ServiceReference reference) {
        Object service = context.getService(reference);

        if (service instanceof HttpService && httpService == null) {
            httpService = (HttpService)service;
        }
        return service;
	}

	public void modifiedService(ServiceReference reference, Object service) {
	}

	public void removedService(ServiceReference reference, Object service) {
        if (service instanceof HttpService) {
        	httpService = null;
        }
	}

	private void registerServlets() {
		boolean compressorReady = (jsCompressorFactoryTracker == null) ? true : jsCompressorFactory != null;
		if (!servletsRegistered && httpService != null && httpContextExtensionService != null && compressorReady && jsOptimizerFactory != null) {
			HttpContext httpContext = httpContextExtensionService.getHttpContext(httpServiceReference, "org.potpie.musicserver.httpcontext");
			if (httpContext == null) {
				System.out.println("Unable to obtain HttpContext for org.dojotoolkit.optimizer.samples.httpcontext");
				return;
			}
			boolean javaChecksum = Boolean.valueOf(System.getProperty("javaChecksum", "false"));
			List<String> bundleIdList = new ArrayList<String>();
			String bundleIdsString = System.getProperty("searchBundleIds");
			if (bundleIdsString != null) {
				StringTokenizer st = new StringTokenizer(bundleIdsString, ",");
				while (st.hasMoreTokens()) {
					bundleIdList.add(st.nextToken().trim());
				}
			}
			String[] bundleIds = new String[bundleIdList.size()];
			bundleIds = bundleIdList.toArray(bundleIds);
			ResourceLoader resourceLoader = new OSGiResourceLoader(context, bundleIds);
			RhinoClassLoader rhinoClassLoader = new RhinoClassLoader(resourceLoader);
			String jsHandlerType = System.getProperty("jsHandlerType");
			
			JSServlet jsServlet = new JSServlet(resourceLoader, jsOptimizerFactory, rhinoClassLoader, javaChecksum, jsHandlerType, null, jsCompressorFactory);
			try {
				httpService.registerServlet("/_javascript", jsServlet, null, httpContext);
				httpService.registerServlet("/", new ResourceServlet(resourceLoader), null, httpContext);
	        	String root = System.getProperty("root");
	        	String storageDir = System.getProperty("storageDir");
				httpService.registerServlet("/service", new MusicServerServlet(root, storageDir, false), null, httpContext);
				//httpService.registerServlet("/service", new MusicServerServlet(), null, httpContext);
				servletsRegistered = true;
			} catch (ServletException e) {
				e.printStackTrace();
			} catch (NamespaceException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class HttpServiceTracker extends ServiceTracker {
		public HttpServiceTracker(BundleContext context) {
			super(context, HttpService.class.getName(), null);
		}

		public Object addingService(ServiceReference reference) {
			httpServiceReference = reference;
			httpService = (HttpService) context.getService(reference);
			registerServlets();
			return httpService;
		}

		public void removedService(ServiceReference reference, Object service) {
			final HttpService httpService = (HttpService) service;
			if (servletsRegistered) {
				httpService.unregister("/_javascript");
				httpService.unregister("/service");
				httpService.unregister("/");
			}
			super.removedService(reference, service);
		}			
	}
	
	private class HttpContextExtensionServiceTracker extends ServiceTracker {
		public HttpContextExtensionServiceTracker(BundleContext context) {
			super(context, HttpContextExtensionService.class.getName(), null);
		}
		
		public Object addingService(ServiceReference reference) {
			httpContextExtensionService = (HttpContextExtensionService)context.getService(reference);
			registerServlets();
			return httpContextExtensionService;
		}
	}
	
	private class JSCompressorFactoryServiceTracker extends ServiceTracker {
		private boolean useV8 = false; 
		private String compressorType = null;
		
		public JSCompressorFactoryServiceTracker(BundleContext context, boolean useV8, String compressorType) {
			super(context, JSCompressorFactory.class.getName(), null);
			this.useV8 = useV8;
			this.compressorType = compressorType;
		}
		
		public Object addingService(ServiceReference reference) {
			String dojoServiceId = null;
			if (compressorType != null) {
				if (compressorType.equals("shrinksafe")) {
					dojoServiceId = "ShrinksafeJSCompressor";
				} else if (compressorType.equals("uglifyjs")) {
					if (useV8) {
						dojoServiceId = "V8UglifyJSCompressor";
					} else {
						dojoServiceId = "RhinoUglifyJSCompressor";
					}
				}
			}
			if (dojoServiceId != null && reference.getProperty("dojoServiceId").equals(dojoServiceId)) {
				jsCompressorFactory = (JSCompressorFactory)context.getService(reference);
				registerServlets();
				
			}
			return context.getService(reference);
		}
	}
	
	private class JSOptimizerFactoryServiceTracker extends ServiceTracker {
		private boolean useV8 = false;
		private String jsHandlerType = null;
		
		public JSOptimizerFactoryServiceTracker(BundleContext context, boolean useV8, String jsHandlerType) {
			super(context, JSOptimizerFactory.class.getName(), null);
			this.useV8 = useV8;
			this.jsHandlerType = jsHandlerType;
		}
		
		public Object addingService(ServiceReference reference) {
			String dojoServiceId = null;
			if (jsHandlerType.equals(JSHandler.SYNCLOADER_HANDLER_TYPE)) {
				if (useV8) {
					dojoServiceId = "V8JSOptimizer";
				} else {
					dojoServiceId = "RhinoJSOptimizer";
				}
			} else {
				if (useV8) {
					dojoServiceId = "AMDV8JSOptimizer";
				} else {
					dojoServiceId = "AMDRhinoJSOptimizer";
				}
			}
			if (dojoServiceId != null && reference.getProperty("dojoServiceId").equals(dojoServiceId)) {
				jsOptimizerFactory = (JSOptimizerFactory)context.getService(reference);
				registerServlets();
			}
			return context.getService(reference);
		}
	}
	
	public class ResourceServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
		private ResourceLoader resourceLoader = null;
		
		public ResourceServlet(ResourceLoader resourceLoader) {
			this.resourceLoader = resourceLoader;
		}

		protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			String target = request.getPathInfo();
			URL url = resourceLoader.getResource(target);
			if (url != null) {
				String mimeType = getServletContext().getMimeType(target);
				if (mimeType == null) {
					mimeType = "text/plain";
				}
				response.setContentType(mimeType);
				InputStream is = null;
				URLConnection urlConnection = null;
				ServletOutputStream os = response.getOutputStream();
				try {
					urlConnection = url.openConnection();
					long lastModifed = urlConnection.getLastModified();
					if (lastModifed > 0) {
					    String ifNoneMatch = request.getHeader("If-None-Match");
						
					    if (ifNoneMatch != null && ifNoneMatch.equals(Long.toString(lastModifed))) {
					    	response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					        return;
					    }

			 			response.setHeader("ETag", Long.toString(lastModifed));
					}
					is = urlConnection.getInputStream();
					byte[] buffer = new byte[4096];
					int len = 0;
					while((len = is.read(buffer)) != -1) {
						os.write(buffer, 0, len);
					}
				}
				finally {
					if (is != null) {try{is.close();}catch(IOException e){}}
				}
			}
			else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "path ["+target+"] not found");
			}
		}
	}
}
