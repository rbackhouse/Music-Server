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
package org.potpie.musicserver.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.VorbisComment;

public class FlacMover {
	public FlacMover(String dir) {
		File directory = new File(dir);
		if (directory.exists()) {
			System.out.println("Processing ["+dir+"] for flac files");
			File[] files = directory.listFiles();
			for (File file : files) {
				if (file.getPath().endsWith(".flac")) {
					checkFlacFile(directory, file);
				}
			}
		}
	}
	
	private void checkFlacFile(File directory, File flacFile) {
		InputStream is = null;
		boolean move = false;
		String title = null;
		String artist = null;
		String album = null;
		String track = null;
		try {
			is = new FileInputStream(flacFile);
			FLACDecoder decoder = new FLACDecoder(is);
			Metadata[] metadatas = decoder.readMetadata();
			for (Metadata metadata : metadatas) {
            	if (metadata instanceof VorbisComment) {
            		VorbisComment comment = (VorbisComment)metadata;
            		String[] s = comment.getCommentByName("TITLE");
            		if (s != null && s.length > 0) {
            			title = s[0];
            		}
            		s =	comment.getCommentByName("ARTIST");
            		if (s != null && s.length > 0) {
            			artist = s[0];
            		}
            		s =	comment.getCommentByName("ALBUM");
            		if (s != null && s.length > 0) {
            			album = s[0];
            		}
            		s =	comment.getCommentByName("TRACKNUMBER");
            		if (s != null && s.length > 0) {
            			track = s[0];
            		}
            	}
			}
			if (title == null || artist == null || album == null || track == null) {
				System.out.println("flac file ["+flacFile.getName()+"] is missing some metadata");
				System.out.println("artist ["+artist+"] album ["+album+"] track [ "+track+"] title ["+title+"]");
				return;
			}
			move = true;
		} catch (IOException e) {
			e.printStackTrace();
		} catch(Throwable t) {
			System.out.println("flac file ["+flacFile.getName()+"] has corrupt some metadata");
			t.printStackTrace();
 		} finally {
			if (is != null){try{is.close();}catch(IOException ex){}}
		}
		if (move) {
			moveFlacFile(directory, flacFile, artist, album);
		}
	}
	
	private void moveFlacFile(File directory, File flacFile, String artist, String album) {
		File artistDirectory = new File(directory, artist);
		if (!artistDirectory.exists()) {
			boolean created = artistDirectory.mkdir();
			if (created) {
				System.out.println("Created artist directory ["+artistDirectory.getPath()+"]");
			}
			else {
				System.out.println("Failed to create artist directory ["+artistDirectory.getPath()+"]");
			}
		}
		
		File albumDirectory = new File(artistDirectory, album);
		if (!albumDirectory.exists()) {
			boolean created = albumDirectory.mkdir();
			if (created) {
				System.out.println("Created album directory ["+albumDirectory.getPath()+"]");
			}
			else {
				System.out.println("Failed to create album directory ["+albumDirectory.getPath()+"]");
			}
		}
		
		File newFile = new File(albumDirectory, flacFile.getName());
		
		if (flacFile.renameTo(newFile)) {
			System.out.print("Renamed ["+flacFile.getPath()+"] to ["+newFile.getPath()+"]");
		}
		else {
			System.out.print("Failed to rename ["+flacFile.getPath()+"] to ["+newFile.getPath()+"]");
		}
	}
	
	public static void main(String[] args) {
		if (args.length > 0) {
			new FlacMover(args[0]);
		}
		else {
			System.out.println("Provide a directory path");
		}
	}
}
