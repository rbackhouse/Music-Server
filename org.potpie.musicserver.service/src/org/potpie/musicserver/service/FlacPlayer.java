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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.frame.Frame;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.SeekTable;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.util.ByteData;

public class FlacPlayer implements MusicFilePlayer, Runnable {
	private static Logger logger = Logger.getLogger("org.potpie.musicserver.service");
	public static final int INACTIVE = 0; 
	public static final int PLAYING = 1; 
	public static final int PAUSED = 2; 
	public static final int STOPPED = 3; 
	private File file = null;
    private AudioFormat fmt;
    private DataLine.Info info;
    private SourceDataLine line;
    private SeekTable seekTable = null;
    private StreamInfo streamInfo = null;
    private FLACDecoder decoder = null;
    private int state = INACTIVE;
    private MusicPlayer musicPlayer = null;
    private FloatControl masterGain = null;
    private float initialVolume = 0; 
	private MusicFilePlayerState playerState = null;
	
	public FlacPlayer(MusicPlayer musicPlayer, File file, float volume) {
		this.musicPlayer = musicPlayer;
		this.file = file;
		this.playerState = new MusicFilePlayerState();
		initialVolume = volume;
	}
	
	public void playPause() {
		logger.logp(Level.FINE, getClass().getName(), "playPause", "["+playerState.getState()+"]");
		if (state == INACTIVE) {
			new Thread(this).start();
		}
		else {
			state = (state == PLAYING) ? PAUSED : PLAYING;
			if (playerState.getState() == MusicFilePlayerState.State.PLAYING) {
				playerState.setPaused();
			} else {
				playerState.setPlaying();
			}
		}
	}
	
	public void stop() {
		state = STOPPED;
        playerState.setStopped();

		while (state != INACTIVE) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		logger.logp(Level.FINE, getClass().getName(), "stop", "Flac ["+file.getPath()+"] stop complete");
	}
	
	public void setVolume(float value) {
		if (masterGain != null) {
			try {
	            double minGainDB = masterGain.getMinimum();
	            double ampGainDB = ((10.0f / 20.0f) * masterGain.getMaximum()) - masterGain.getMinimum();
	            double cste = Math.log(10.0) / 20;
	            double valueDB = minGainDB + (1 / cste) * Math.log(1 + (Math.exp(cste * ampGainDB) - 1) * value);
				
				masterGain.setValue((float) valueDB);
				logger.logp(Level.FINE, getClass().getName(), "setVolume", "Gain set to "+masterGain.getValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public long getPosition() {
		if (line != null) {
			return line.getMicrosecondPosition();
		}
		else {
			return 0;
		}
	}

	public void run() {
		InputStream is = null;
        try {
        	is = new BufferedInputStream(new FileInputStream(file), 32768);
            decoder = new FLACDecoder(is);
            Metadata[] metadataArray = decoder.readMetadata();
            for (Metadata metadata : metadataArray) {
                if (metadata instanceof SeekTable) {
                	seekTable = (SeekTable)metadata;
                }
            }

            streamInfo = decoder.getStreamInfo();
            fmt = streamInfo.getAudioFormat();
            info = new DataLine.Info(SourceDataLine.class, fmt, AudioSystem.NOT_SPECIFIED);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(fmt, AudioSystem.NOT_SPECIFIED);
            line.start();
            masterGain = (FloatControl)line.getControl(FloatControl.Type.MASTER_GAIN);
            setVolume(initialVolume);
            state = PLAYING;
            playerState.setPlaying();

			logger.logp(Level.FINE, getClass().getName(), "run", "Flac ["+file.getPath()+"] started");
            play();
			logger.logp(Level.FINE, getClass().getName(), "run", "Flac ["+file.getPath()+"] stopped");
        } catch (Throwable e) {
        	e.printStackTrace();
        	musicPlayer.songComplete();
        }
        finally {
        	if (is != null) {
        		try {is.close();}catch(IOException e) {}
        	}
        }

        line.drain();
        line.close();
		state = INACTIVE;
        playerState.setStopped();

		logger.logp(Level.FINE, getClass().getName(), "run", "Flac ["+file.getPath()+"] complete");
	}
	
	public MusicFilePlayerState getState() {
		return playerState;
	}
	
	private void play() throws IOException {
		while (true) {
			switch (state) {
				case PLAYING: {
			    	Frame frame = decoder.readNextFrame();
			    	if (frame == null) {
			    		musicPlayer.songComplete();
			    		return;
			    	}
			        ByteData pcm = decoder.decodeFrame(frame, null);
			        line.write(pcm.getData(), 0, pcm.getLen());
					break;
				}
				case PAUSED: {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}
					break;
				}
				case STOPPED: {
					return;
				}
			}
		}
	}
}
