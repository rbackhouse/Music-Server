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
package org.potpie.musicserver.callback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dojotoolkit.zazl.optimizer.OptimizerCallbackHandler;
import org.dojotoolkit.zazl.util.JSONUtils;
import org.potpie.musicserver.service.MusicPlayer;
import org.potpie.musicserver.service.db.MusicDB;

public class MusicServerCallbackHandler extends OptimizerCallbackHandler {
	private static final String[] musicServerCallbackNames = { 
    	"getArtists",
    	"getVolume",
    	"getPlayerState"
    };
	
	private MusicDB musicDB = null;
	private MusicPlayer musicPlayer = null;
	
	public MusicServerCallbackHandler(HttpServletRequest request) {
		super(request);
		musicDB = (MusicDB)request.getAttribute("musicDB");
		musicPlayer = (MusicPlayer)request.getAttribute("musicPlayer");
	}
	
	public MusicServerCallbackHandler() {
		super();
	}
	
	protected void collectCallbackNames(List<String> callbackNameList) {
		super.collectCallbackNames(callbackNameList);
		for (String callbackName : musicServerCallbackNames) {
			callbackNameList.add(callbackName);
		}
	}
	
	public String getArtists(String json) {
		if (musicDB == null) {
			return "[]";
		} else {
			return JSONUtils.toJson(musicDB.getAllArtists());
		}
	}
	
	@SuppressWarnings("unchecked")
	public String getVolume(String json) {
		if (musicDB == null) {
			return "{volume : 5}";
		} else {
			@SuppressWarnings("rawtypes")
			Map volume = new HashMap();
			volume.put("volume", new Float(musicPlayer.getVolume())*10);
			return JSONUtils.toJson(volume);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public String getPlayerState(String json) {
		Map state = new HashMap();
		if (musicPlayer == null) {
			state.put("currentState", "STOPPED");
			return JSONUtils.toJson(state);
		} else {
			state.put("currentState", musicPlayer.getState().getState().toString());
			return JSONUtils.toJson(state);
		}
	}
}
