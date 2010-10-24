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

public class FlacRenamer {

	public FlacRenamer(String dir) {
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
		boolean rename = false;
		String fileName = null;
		try {
			is = new FileInputStream(flacFile);
			FLACDecoder decoder = new FLACDecoder(is);
			Metadata[] metadatas = decoder.readMetadata();
			String title = null;
			String artist = null;
			String album = null;
			String track = null;
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
			fileName = artist + " - " + album + " - " + track + " - " + title + ".flac";
			if (flacFile.getName().equals(fileName)) {
				System.out.println("flac file ["+flacFile.getName()+"] has correct name");
			}
			else {
				rename = true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch(Throwable t) {
			System.out.println("flac file ["+flacFile.getName()+"] has corrupt some metadata");
			t.printStackTrace();
 		} finally {
			if (is != null){try{is.close();}catch(IOException ex){}}
		}
		if (rename) {
			renameFlacFile(directory, flacFile, fileName);
		}
	}
	/*
	private void copyFlacFile(File directory, File flacFile, String flacFileName) {
		File outputFile = new File(directory, flacFileName);
		try {
			if (!outputFile.createNewFile()) {
				System.out.println("flac file ["+outputFile.getName()+"]already exists");
				return;
			}
		}
		catch (IOException e) {
			System.out.println("Unable to create flac file ["+outputFile.getName()+"]");
			return;
		}
		InputStream is = null;
		OutputStream os = null;
		try {
			System.out.println("Copying flac file ["+flacFile.getName()+"] to ["+flacFileName+"]");
			is = new BufferedInputStream(new FileInputStream(flacFile));
			os = new BufferedOutputStream(new FileOutputStream(new File(directory, flacFileName)));
			byte[] buffer = new byte[4096];
			
			while (is.read(buffer) != -1) {
				os.write(buffer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null){try{is.close();}catch(IOException ex){}}
			if (os != null){try{os.close();}catch(IOException ex){}}
		}
	}
	*/
	
	private void renameFlacFile(File directory, File flacFile, String flacFileName) {
		File outputFile = new File(directory, flacFileName);
		if (!flacFile.renameTo(outputFile)) {
			System.out.println("["+flacFile.getName()+"]failed to get renamed to ["+outputFile.getName()+"]");
		}
		else {
			System.out.println("["+flacFile.getName()+"] renamed to ["+outputFile.getName()+"]");
		}
	}
	
	public static void main(String[] args) {
		if (args.length > 0) {
			new FlacRenamer(args[0]);
		}
		else {
			System.out.println("Provide a directory path");
		}
	}
}
