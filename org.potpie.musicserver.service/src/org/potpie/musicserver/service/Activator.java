/*******************************************************************************
* Copyright (c) 2010 Richard Backhouse
* 
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
*******************************************************************************/
package org.potpie.musicserver.service;

import javax.servlet.ServletException;

import org.dojotoolkit.zazl.servlet.osgi.ZazlHttpContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer  {
    private HttpService httpService = null;
    private ServiceTracker httpServiceTracker = null;
    private BundleContext context = null;

	public void start(BundleContext context) throws Exception {
        this.context = context;
        httpServiceTracker = new ServiceTracker(context, HttpService.class.getName(), this);
        httpServiceTracker.open();
	}

	public void stop(BundleContext context) throws Exception {
        httpServiceTracker.close();
	}

	public Object addingService(ServiceReference reference) {
        Object service = context.getService(reference);

        if (service instanceof HttpService && httpService == null) {
            httpService = (HttpService)service;
            MusicServerServlet musicServerServlet = new MusicServerServlet("/Users/rbackhouse/dev/webmediaplayer/test", "/Users/rbackhouse/dev/webmediaplayer/data");
            //MusicServerServlet musicServerServlet = new MusicServerServlet("/Users/rbackhouse/dev/webmediaplayer/testflac", "/Users/rbackhouse/dev/webmediaplayer/data");
            //MusicServerServlet musicServerServlet = new MusicServerServlet("/Volumes/mp3/music", "/Users/rbackhouse/dev/webmediaplayer/data");
            //MusicServerServlet musicServerServlet = new MusicServerServlet("/Volumes/flac/music", "/Users/rbackhouse/dev/webmediaplayer/data");
            //MusicServerServlet musicServerServlet = new MusicServerServlet(null, null);
            try {
				httpService.registerServlet("/service", musicServerServlet, null, ZazlHttpContext.getSingleton(httpService.createDefaultHttpContext()));
			} catch (ServletException e) {
				e.printStackTrace();
			} catch (NamespaceException e) {
				e.printStackTrace();
			}
        }
        return service;
	}

	public void modifiedService(ServiceReference reference, Object service) {
	}

	public void removedService(ServiceReference reference, Object service) {
        if (service instanceof HttpService) {
        	httpService.unregister("/service");
        	httpService = null;
        }
	}
}
