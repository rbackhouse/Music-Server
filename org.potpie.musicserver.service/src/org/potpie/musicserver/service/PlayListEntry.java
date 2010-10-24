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

import java.util.Comparator;
import java.util.Map;

public class PlayListEntry implements Comparator<PlayListEntry> {
	private Map<String, Number> songIndex = null;
	private Map songData = null;
	
	public PlayListEntry(Map<String, Number> songIndex, Map songData) {
		this.songIndex = songIndex;
		this.songData = songData;
	}

	public Map<String, Number> getSongIndex() {
		return songIndex;
	}

	public Map getSongData() {
		return songData;
	}
	
	public int compare(PlayListEntry o1, PlayListEntry o2) {
		long offset1 = o1.songIndex.get("offset").longValue();
		int length1 = o1.songIndex.get("length").intValue();
		long offset2 = o2.songIndex.get("offset").longValue();
		int length2 = o2.songIndex.get("length").intValue();
		if (offset1 == offset2 && length1 == length2) {
			return 0;
		} else {
			return -1;
		}
	}
	
	public int hashCode() {
		long offset = songIndex.get("offset").longValue();
		return (int)offset;
	}

	public boolean equals(Object o) {
		boolean isEqual = false;
	
		if (o instanceof PlayListEntry) {
			long offset1 = ((PlayListEntry)o).songIndex.get("offset").longValue();
			int length1 = ((PlayListEntry)o).songIndex.get("length").intValue();
			long offset2 = songIndex.get("offset").longValue();
			int length2 = songIndex.get("length").intValue();
			if (offset1 == offset2 && length1 == length2) {
				isEqual = true;
			}
		}
		return isEqual;
	}

}
