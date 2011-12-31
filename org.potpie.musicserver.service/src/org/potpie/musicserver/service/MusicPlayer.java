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

import java.io.File;
import java.util.Map;

import javazoom.jlgui.basicplayer.BasicPlayer;

import org.potpie.musicserver.service.db.MusicDB;

public class MusicPlayer {
    private MusicFilePlayer musicFilePlayer = null;
    private PlayList playList = null;
    private MusicDB musicDB = null;
	private BasicPlayer musicPlayer = null;
	private float currentVolume = 1.0f;
    
	public MusicPlayer(MusicDB musicDB) {
		this.musicDB = musicDB;
		musicPlayer = new BasicPlayer();
    	playList = new PlayList(musicDB);
	}
	
	public PlayList getPlayList() {
		return playList;
	}
	
	public synchronized void next() {
		if (playList.next()) {
			PlayListEntry entry = playList.getCurrentlyPlaying();
			if (entry != null) {
				if (musicFilePlayer != null) {
					musicFilePlayer.stop();
					musicFilePlayer = null;
				}
				musicFilePlayer = createMusicFilePlayer(entry.getSongIndex());
				musicFilePlayer.playPause();
			}
		}
	}
	
	public synchronized void previous() {
		if (playList.previous()) {
			PlayListEntry entry = playList.getCurrentlyPlaying();
			if (entry != null) {
				if (musicFilePlayer != null) {
					musicFilePlayer.stop();
					musicFilePlayer = null;
				}
				musicFilePlayer = createMusicFilePlayer(entry.getSongIndex());
				musicFilePlayer.playPause();
			}
		}
	}
	
	public synchronized void play() {
		if (musicFilePlayer == null) {
			PlayListEntry entry = playList.getCurrentlyPlaying();
			if (entry != null) {
				musicFilePlayer = createMusicFilePlayer(entry.getSongIndex());
				musicFilePlayer.playPause();
			}
		}
	}
	
	public synchronized void pause() {
		if (musicFilePlayer != null) {
			musicFilePlayer.playPause();
		}
	}
	
	public synchronized void stop() {
		if (musicFilePlayer != null) {
			musicFilePlayer.stop();
			musicFilePlayer = null;
		}
	}

	public synchronized void setVolume(float value) {
		currentVolume = value;
		if (musicFilePlayer != null) {
	        musicFilePlayer.setVolume(value);
		}
	}
	
	public float getVolume() {
		return currentVolume;
	}
	
	public long getCurrentPosition() {
		if (musicFilePlayer != null) {
	        return musicFilePlayer.getPosition();
		}
		else {
			return 0;
		}
	}
	
	public MusicFilePlayerState getState() {
		if (musicFilePlayer != null) {
			return musicFilePlayer.getState();
		}
		else {
			return new MusicFilePlayerState(); 
		}
	}
	
	private MusicFilePlayer createMusicFilePlayer(Map songIndex) {
		MusicFilePlayer musicFilePlayer = null;
		
		Map songData = musicDB.getSongDataForSongIndex(songIndex);
		String type = (String)songData.get("type");
		File musicFile = new File((String)songData.get("path"));
		if (type.equals("mp3")) {
			musicFilePlayer = new MP3Player(this, musicPlayer, musicFile, currentVolume);
		}
		else if (type.equals("flac")) {
			musicFilePlayer = new FlacPlayer(this, musicFile, currentVolume);
		}
		return musicFilePlayer;
	}
	
	void songComplete() {
		musicFilePlayer = null;
		next();
	}
}
