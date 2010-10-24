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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.PCMProcessor;
import org.kc7bfi.jflac.frame.Frame;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.util.ByteData;
import org.kc7bfi.jflac.util.WavWriter;

public class MusicStreamer {

    public static void stream(PlayList playList, HttpServletRequest request, HttpServletResponse response) throws IOException {
    	
    	String strOffset = request.getParameter("offset");
    	String strLength = request.getParameter("length");
    	if (strOffset != null && strLength != null) {
			Map<String, Number> songIndex = new HashMap<String, Number>();
			songIndex.put("length", new Long(strLength));
			songIndex.put("offset", new Long(strOffset));
			PlayListEntry entry = playList.findEntry(songIndex);
			Map songData = entry.getSongData();
			File musicFile = new File((String)songData.get("path"));
			String type = (String)songData.get("type");
			writeResponse(request, response, musicFile, type);
    	}
    }
    
    public static void writeResponse(HttpServletRequest request, HttpServletResponse response, File musicFile, String type) throws IOException {
    	int totallen = 0;
		InputStream is = null;
		OutputStream os = response.getOutputStream();
        try {
			response.addHeader("Content-Disposition","attachment; filename="+musicFile.getName());
			response.setHeader("Accept-Ranges", "bytes");
	    	String range = request.getHeader("Range");
	    	int start = 0;
    		long end = 0;
			is = new BufferedInputStream(new FileInputStream(musicFile));
	    	if (range != null) {
				response.setContentType("audio/mpeg");
	    		String[] split = range.split("=");
	    		String[] rangeValues = split[1].split("-");
	    		start = Integer.valueOf(rangeValues[0]);
	    		if (rangeValues.length > 1) {
	    			end = Long.valueOf(rangeValues[1]);
	    		} else {
	    			end = musicFile.length() - 1;
	    		}
	    		for (int i = start; i < end+1; i++) {
	    			totallen++;
	    			os.write(is.read());
	    		}
	    	} else {
				if (type.equals("flac")) {
					response.setContentType("audio/wav");
					writeFlacResponse(is, os, musicFile);
				} else {
					response.setContentType("audio/mpeg");
					byte[] buffer = new byte[4096];
					int len = 0;
					while ((len = is.read(buffer)) != -1) {
						os.write(buffer, 0, len);
						totallen += len;
					}
				}
	    	}
		} finally {
			if (is != null) { try { is.close(); } catch (IOException e) {}}
		}
    }
    
    private static void writeFlacResponse(InputStream is, OutputStream os, File musicFile) {
    	final WavWriter wavWriter = new WavWriter(os);
    	FLACDecoder decoder = new FLACDecoder(is);
    	decoder.addPCMProcessor(new PCMProcessor() {
			public void processPCM(ByteData bd) {
			}

			public void processStreamInfo(StreamInfo streamInfo) {
				try {
					wavWriter.writeHeader(streamInfo);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
    	});
    	try {
	        decoder.readMetadata();
			while (true) {
		    	Frame frame = decoder.readNextFrame();
		    	if (frame == null) {
		    		return;
		    	}
		        ByteData bd = decoder.decodeFrame(frame, null);
	            wavWriter.writePCM(bd);
			}
    	} catch(IOException e) {
    	}
    }
}
