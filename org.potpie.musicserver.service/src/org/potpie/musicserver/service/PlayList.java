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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.potpie.musicserver.service.db.MusicDB;

public class PlayList {
    private List<PlayListEntry> playList = null;
    private int currentIndex = 0;
    private MusicDB musicDB = null;

    public PlayList(MusicDB musicDB) {
    	this.musicDB = musicDB;
    	playList = new ArrayList<PlayListEntry>();
    }
    
	public synchronized boolean add(Map<String, Number> songIndex) {
		boolean added = false;
		Map songData = musicDB.getSongDataForSongIndex(songIndex);
		PlayListEntry entry = new PlayListEntry(songIndex, songData);
		if (!playList.contains(entry)) {
			added = true;
			playList.add(entry);
		}
		return added;
	}
	
	public synchronized void clear() {
		playList.clear();
		currentIndex = 0;
	}
	
	public synchronized void remove(Map<String, Number> songIndex) {
		PlayListEntry entry = findEntry(songIndex);
		if (getCurrentlyPlaying() == null || !getCurrentlyPlaying().equals(entry)) {
			playList.remove(entry);
		}
	}
	
	public synchronized Iterator<PlayListEntry> getPlayList() {
		return playList.iterator();
	}
	
	public synchronized PlayListEntry findEntry(Map<String, Number> songIndex) {
		Long offset = (Long)songIndex.get("offset");
		Long length = (Long)songIndex.get("length");
		PlayListEntry entry = null;
		for (PlayListEntry playListEntry : playList) {
			if (playListEntry.getSongIndex().get("offset").equals(offset) && playListEntry.getSongIndex().get("length").equals(length)) {
				entry = playListEntry;
				break;
			}
		}
		return entry;
	}
	
	public synchronized PlayListEntry getCurrentlyPlaying() {
		try {
			return (currentIndex != -1) ? playList.get(currentIndex) : null;
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
    
	public synchronized boolean next() {
		if (currentIndex < (playList.size()-1)) {
			++currentIndex;
			return true;
		} else {
			return false;
		}
	}
	
	public synchronized boolean previous() {
		if (currentIndex > 0) {
			--currentIndex;
			return true;
		} else {
			return false;
		}
	}
}
