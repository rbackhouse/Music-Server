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
import java.util.logging.Level;
import java.util.logging.Logger;

import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

public class MP3Player implements MusicFilePlayer, BasicPlayerListener {
	private static Logger logger = Logger.getLogger("org.potpie.musicserver.service");
	
	private BasicPlayer basicPlayer = null;
    private int state = BasicPlayerEvent.UNKNOWN;
    private long position = 0;
    private File musicFile = null;
    private MusicPlayer musicPlayer = null;
    private float initialVolume = 0; 
	private MusicFilePlayerState playerState = null;

	public MP3Player(MusicPlayer musicPlayer, BasicPlayer basicPlayer, File musicFile, float volume) {
		this.musicPlayer = musicPlayer;
		this.basicPlayer = basicPlayer;
		this.musicFile = musicFile;
		this.playerState = new MusicFilePlayerState();
		initialVolume = volume;
		basicPlayer.addBasicPlayerListener(this);
	}
	
	public void playPause() {
		if (state == BasicPlayerEvent.PLAYING || state == BasicPlayerEvent.RESUMED || state == BasicPlayerEvent.GAIN) {
			try {
				basicPlayer.pause();
				playerState.setPaused();
			} catch (BasicPlayerException e) {
				e.printStackTrace();
			}
		}
		else if (state == BasicPlayerEvent.PAUSED) {
			try {
				basicPlayer.resume();
				playerState.setPlaying();
			} catch (BasicPlayerException e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				basicPlayer.open(musicFile);
				basicPlayer.play();
				playerState.setPlaying();
			} catch (BasicPlayerException e) {
				e.printStackTrace();
			}
		}
	}

	public void stop() {
		try {
			basicPlayer.stop();
			playerState.setStopped();
			basicPlayer.removeBasicPlayerListener(this);
		} catch (BasicPlayerException e) {
			e.printStackTrace();
		}
	}
	
	public void setVolume(float value) {
		if (state == BasicPlayerEvent.PLAYING || state == BasicPlayerEvent.GAIN || state == BasicPlayerEvent.RESUMED) {
			try {
				basicPlayer.setGain(value);
			} catch (BasicPlayerException e) {
				e.printStackTrace();
			}
		}
	}
	
	public long getPosition() {
		return position;
	}
	
	public void opened(Object stream, Map properties) {
	}

	public void progress(int bytesread, long microseconds, byte[] pcmdata, Map properties) {
		position = microseconds;
	}

	public void setController(BasicController basicController) {
	}

	public void stateUpdated(BasicPlayerEvent event) {
		state = event.getCode();
		if (state == BasicPlayerEvent.EOM) {
			basicPlayer.removeBasicPlayerListener(this);
			musicPlayer.songComplete();
			playerState.setStopped();
		}
		switch (state) {
		case BasicPlayerEvent.OPENED: 
			logger.logp(Level.FINE, getClass().getName(), "stateUpdated", "OPENED");
			break;
		case BasicPlayerEvent.EOM: 
			logger.logp(Level.FINE, getClass().getName(), "stateUpdated", "EOM");
			break;
		case BasicPlayerEvent.OPENING: 
			logger.logp(Level.FINE, getClass().getName(), "stateUpdated", "OPENING");
			break;
		case BasicPlayerEvent.PAUSED: 
			logger.logp(Level.FINE, getClass().getName(), "stateUpdated", "PAUSED");
			break;
		case BasicPlayerEvent.PLAYING: 
			logger.logp(Level.FINE, getClass().getName(), "stateUpdated", "PLAYING");
			setVolume(initialVolume);
			break;
		case BasicPlayerEvent.RESUMED: 
			logger.logp(Level.FINE, getClass().getName(), "stateUpdated", "RESUMED");
			break;
		case BasicPlayerEvent.STOPPED: 
			logger.logp(Level.FINE, getClass().getName(), "stateUpdated", "STOPPED");
			break;
		case BasicPlayerEvent.UNKNOWN: 
			logger.logp(Level.FINE, getClass().getName(), "stateUpdated", "UNKNOWN");
			break;
		case BasicPlayerEvent.GAIN: 
			logger.logp(Level.FINE, getClass().getName(), "stateUpdated", "GAIN");
			break;
		}
	}
	
	public MusicFilePlayerState getState() {
		return playerState;
	}
}
