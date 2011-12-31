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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.dojotoolkit.json.JSONParser;
import org.dojotoolkit.json.JSONSerializer;
import org.potpie.musicserver.service.db.MusicDB;

public class MusicServerServlet extends HttpServlet {
	private static Logger logger = Logger.getLogger("org.potpie.musicserver.service");
    private static final long serialVersionUID = 1L;
    private MusicDB musicDB = null;
    private MusicPlayer musicPlayer = null;
	private Random random = new Random();
    private String rootDir = null;
    private String storageDir = null;
    private boolean skipScan = false;
	private File tempDir = null;
    
    public MusicServerServlet() {}

    public MusicServerServlet(String rootDir, String storageDir, boolean skipScan) {
    	this.rootDir = rootDir;
    	this.storageDir = storageDir;
    	this.skipScan = skipScan;
    }
    
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		tempDir = (File)config.getServletContext().getAttribute("javax.servlet.context.tempdir");
		logger.logp(Level.INFO, getClass().getName(), "init", "tempDir = ["+tempDir+"]");
		File configFile = new File(tempDir, "config.properties");
		if (configFile.exists()) {
			Properties configProps = new Properties();
			InputStream is = null;
			try {
				is = new BufferedInputStream(new FileInputStream(configFile));
				configProps.load(is);
				storageDir = configProps.getProperty("storageDir");
				rootDir = configProps.getProperty("rootDir");
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (is != null) { try { is.close(); } catch (IOException e) {}}
			}
		}
		if (storageDir == null) {
			storageDir = config.getServletContext().getInitParameter("storageDir");
		}
		if (rootDir == null) {
			rootDir = config.getServletContext().getInitParameter("root");
		}
		String strSkipScan = config.getServletContext().getInitParameter("skipScan");
		if (strSkipScan != null) {
			skipScan = Boolean.valueOf(strSkipScan);
		}
		if (storageDir != null && rootDir != null) {
			initializeDB(false);
		}
	}

	public void destroy() {
		super.destroy();
		if (musicDB != null) {
			musicDB.stop();
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		/*
		if (musicDB == null) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server failed to initialize correctly. Check the logs");
			return;
		}
		*/
		String[] segments = getSegments(req.getPathInfo());
		if (segments.length > 0) {
			if (segments[0].equals("artist")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				if (segments.length > 1) {
					List<Map<String, Number>> songs = musicDB.getSongsForArtist(segments[1]);
					Map songDataStore = new HashMap();
					songDataStore.put("identifier", "id");
					songDataStore.put("label", "title");
					List items = new ArrayList();
					songDataStore.put("items", items);
					int i = 0;
					for (Map<String, Number> songIndex : songs) {
						Map songData = musicDB.getSongDataForSongIndex(songIndex);
						songData.put("id", new Integer(++i));
						songData.put("offset", songIndex.get("offset"));
						songData.put("length", songIndex.get("length"));
						items.add(songData);
					}
					JSONSerializer.serialize(w, songDataStore, true);
				}
				else {
					List<String> artists = musicDB.getAllArtists();
					Map artistDataStore = new HashMap();
					List tabs = null;
					artistDataStore.put("identifier", "artist");
					artistDataStore.put("label", "artist");
					List items = new ArrayList();
					artistDataStore.put("items", items);
					if (req.getParameter("listId") != null) {
						try {
							artistDataStore.put("listId", req.getParameter("listId"));
							int startIndex = Integer.valueOf(req.getParameter("startIndex")).intValue();
							int endIndex = Integer.valueOf(req.getParameter("endIndex")).intValue();
							
							for (int i = startIndex; i <= endIndex; i++) {
								String artist = artists.get(i);
								Map artistData = new HashMap();
								artistData.put("artist", artist);
								items.add(artistData);
							}
						}
						catch (NumberFormatException e) {
						}
					} else if (req.getParameter("tabs") != null) {
						tabs = new ArrayList();
						Map tab = new HashMap();
						tab.put("label", "A-");
						List artistList = new ArrayList();
						tab.put("artistList", artistList);
						tab.put("startIndex", new Integer(0));
						tabs.add(tab);
						List currentList = artistList;
						int currentListCount = 0;
						int maxPerTab = artists.size() / 5;
						for (int i = 0; i < artists.size(); i++) {
							if (currentListCount > maxPerTab) {
								tab.put("label",(String)tab.get("label") + artists.get(i-1).charAt(0)); 
								tab.put("endIndex", new Integer(i-1));
								tab = new HashMap();
								tab.put("label", artists.get(i-1).charAt(0)+"-");
								artistList = new ArrayList();
								tab.put("artistList", artistList);
								tab.put("startIndex", new Integer(i));
								tabs.add(tab);
								currentList = null;
								currentListCount = 0;
							}
							if (currentList != null) {
								Map artistItem = new HashMap();
								artistItem.put("id", i+1);
								artistItem.put("label", artists.get(i));
								currentList.add(artistItem);
							}
							currentListCount++;
						}
						tab.put("label",(String)tab.get("label") + artists.get(artists.size()-1).charAt(0)); 
						tab.put("endIndex", new Integer(artists.size()-1));
					} else {
						for (String artist: artists) {
							Map artistData = new HashMap();
							artistData.put("artist", artist);
							items.add(artistData);
						}
					}
					if (tabs != null) {
						JSONSerializer.serialize(w, tabs, true);
					} else {
						JSONSerializer.serialize(w, artistDataStore, true);
					}
				}
			}
			else if (segments[0].equals("album")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				if (segments.length > 1) {
					String artist = req.getParameter("artist");
					List<Map<String, Number>> songs = musicDB.getSongsForAlbum(segments[1], artist);
					Map songDataStore = new HashMap();
					songDataStore.put("identifier", "id");
					songDataStore.put("label", "title");
					List items = new ArrayList();
					songDataStore.put("items", items);
					int i = 0;
					for (Map<String, Number> songIndex : songs) {
						Map songData = musicDB.getSongDataForSongIndex(songIndex);
						songData.put("id", new Integer(++i));
						songData.put("offset", songIndex.get("offset"));
						songData.put("length", songIndex.get("length"));
						items.add(songData);
					}
					JSONSerializer.serialize(w, songDataStore, true);
				}
				else {
					List<String> albums = musicDB.getAllAlbums();
					Map albumDataStore = new HashMap();
					albumDataStore.put("identifier", "album");
					albumDataStore.put("label", "album");
					List items = new ArrayList();
					albumDataStore.put("items", items);
					for (String album: albums) {
						Map albumData = new HashMap();
						albumData.put("album", album);
						albumData.put("artist", musicDB.getArtistForAlbum(album));
						items.add(albumData);
					}
					JSONSerializer.serialize(w, albumDataStore, true);
				}
			}
			else if (segments[0].equals("playList")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				Iterator<PlayListEntry> itr = musicPlayer.getPlayList().getPlayList();
				writePlayList(w, itr);
			}
			else if (segments[0].equals("streamPlayList")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				Iterator<PlayListEntry> itr = getSessionPlayList(req.getSession()).getPlayList();
				writePlayList(w, itr);
			}
			else if (segments[0].equals("play")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				musicPlayer.play();
				writeCurrentlyPlaying(w);
			}
			else if (segments[0].equals("streamPlay")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				PlayList playList = getSessionPlayList(req.getSession());
				//playList.play();
				writeCurrentlyStreaming(w, playList);
			}
			else if (segments[0].equals("stop")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				musicPlayer.stop();
				writeCurrentlyPlaying(w);
			}
			else if (segments[0].equals("pause")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				musicPlayer.pause();
				writeCurrentlyPlaying(w);
			}
			else if (segments[0].equals("next")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				musicPlayer.next();
				writeCurrentlyPlaying(w);
			}
			else if (segments[0].equals("streamNext")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				PlayList playList = getSessionPlayList(req.getSession());
				if (playList.next()) {
					writeCurrentlyStreaming(w, playList);
				} else {
					JSONSerializer.serialize(w, new HashMap(), true);
				}
			}
			else if (segments[0].equals("previous")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				musicPlayer.previous();
				writeCurrentlyPlaying(w);
			}
			else if (segments[0].equals("streamPrevious")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				PlayList playList = getSessionPlayList(req.getSession());
				if (playList.previous()) {
					writeCurrentlyStreaming(w, playList);
				} else {
					JSONSerializer.serialize(w, new HashMap(), true);
				}
			}
			else if (segments[0].equals("currentlyPlaying")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				writeCurrentlyPlaying(w);
			}
			else if (segments[0].equals("currentlyStreaming")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				PlayList playList = getSessionPlayList(req.getSession());
				writeCurrentlyStreaming(w, playList);
			}
			else if (segments[0].equals("clearPlayList")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				musicPlayer.stop();
				musicPlayer.getPlayList().clear();
				writeCurrentlyPlaying(w);
			}
			else if (segments[0].equals("clearStreamPlayList")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				PlayList playList = getSessionPlayList(req.getSession());
				playList.clear();
				playList = null;
				playList = new PlayList(musicDB);
				req.getSession().setAttribute("StreamPlayList", playList);
				writeCurrentlyStreaming(w, playList);
			}
			else if (segments[0].equals("setVolume")) {
				resp.setContentType("test/json");
				try {
					Float value = Float.valueOf(segments[1]);
					musicPlayer.setVolume(value.floatValue());
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
			else if (segments[0].equals("getVolume")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				float currentVolume = musicPlayer.getVolume();
				Map volume = new HashMap();
				volume.put("volume", new Float(currentVolume));
				JSONSerializer.serialize(w, volume, true);
			}
			else if (segments[0].equals("addAlbumToPlayList")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				if (segments.length > 1) {
					addAlbumToPlayList(w, segments[1], req.getParameter("artist"), musicPlayer.getPlayList());
				}
				writeCurrentlyPlaying(w);
			}
			else if (segments[0].equals("addAlbumToStreamPlayList")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				PlayList playList = getSessionPlayList(req.getSession());
				if (segments.length > 1) {
					addAlbumToPlayList(w, segments[1], req.getParameter("artist"), playList);
				}
				writeCurrentlyStreaming(w, playList);
			}
			else if (segments[0].equals("randomPlaylist")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				musicPlayer.getPlayList().clear();
				String artist = null;
				if (segments.length > 1) {
					artist = segments[1];
				}
				createRandomPlayList(w, musicPlayer.getPlayList(), artist);
				writeCurrentlyPlaying(w);
			}
			else if (segments[0].equals("randomStreamPlaylist")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				PlayList playList = getSessionPlayList(req.getSession());
				playList.clear();
				playList = null;
				playList = new PlayList(musicDB);
				req.getSession().setAttribute("StreamPlayList", playList);
				String artist = null;
				if (segments.length > 1) {
					artist = segments[1];
				}
				createRandomPlayList(w, playList, artist);
				writeCurrentlyStreaming(w, playList);
			}
			else if (segments[0].equals("albumsForArtist")) {
				resp.setContentType("test/json");
				Writer w = resp.getWriter();
				if (segments.length > 1) {
					List<String> albums = musicDB.getAlbumsForArtist(segments[1]);
					Map albumDataStore = new HashMap();
					albumDataStore.put("identifier", "album");
					albumDataStore.put("label", "album");
					List items = new ArrayList();
					albumDataStore.put("items", items);
					for (String album: albums) {
						Map albumData = new HashMap();
						albumData.put("album", album);
						albumData.put("artist", musicDB.getArtistForAlbum(album));
						items.add(albumData);
					}
					JSONSerializer.serialize(w, albumDataStore, true);
				}
			}
			else if (segments[0].equals("stream")) {
				PlayList playList = getSessionPlayList(req.getSession());
				MusicStreamer.stream(playList, req, resp);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("test/json");
		String[] segments = getSegments(req.getPathInfo());
		if (segments.length > 0) {
			if (segments[0].equals("addToPlayList")) {
				List<Map<String, Number>> songs = (List<Map<String, Number>>)JSONParser.parse(req.getReader());
				for (Map<String, Number> song : songs) {
					musicPlayer.getPlayList().add(song);
				}
			} else if (segments[0].equals("addToStreamPlayList")) {
				List<Map<String, Number>> songs = (List<Map<String, Number>>)JSONParser.parse(req.getReader());
				for (Map<String, Number> song : songs) {
					PlayList playList = getSessionPlayList(req.getSession());
					playList.add(song);
				}
			} else if (segments[0].equals("removeFromPlayList")) {
				List<Map<String, Number>> songs = (List<Map<String, Number>>)JSONParser.parse(req.getReader());
				for (Map<String, Number> song : songs) {
					musicPlayer.getPlayList().remove(song);
				}
			} else if (segments[0].equals("removeFromStreamPlayList")) {
				List<Map<String, Number>> songs = (List<Map<String, Number>>)JSONParser.parse(req.getReader());
				for (Map<String, Number> song : songs) {
					PlayList playList = getSessionPlayList(req.getSession());
					playList.remove(song);
				}
			} else if (segments[0].equals("stream")) {
				PlayList playList = getSessionPlayList(req.getSession());
				MusicStreamer.stream(playList, req, resp);
			} else if (segments[0].equals("initialize")) {
				Map<String, String> config = (Map<String, String>)JSONParser.parse(req.getReader());
				this.rootDir = config.get("rootDir");
				this.storageDir = config.get("storageDir");
				initializeDB(true);
				writeCurrentlyPlaying(resp.getWriter());
			}
		}
	}

	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (musicDB == null) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server failed to initialize correctly. Check the logs");
			return;
		}
		String[] segments = getSegments(req.getPathInfo());
		if (segments.length > 0) {
			if (segments[0].equals("stream")) {
				PlayList playList = getSessionPlayList(req.getSession());
				MusicStreamer.stream(playList, req, resp);
			}
		}
	}
	
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (musicDB == null) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server failed to initialize correctly. Check the logs");
			return;
		}
		String[] segments = getSegments(req.getPathInfo());
		if (segments.length > 0) {
			if (segments[0].equals("stream")) {
				PlayList playList = getSessionPlayList(req.getSession());
				MusicStreamer.stream(playList, req, resp);
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void writeCurrentlyPlaying(Writer w) throws IOException {
		Map currentlyPlaying = new HashMap();
		if (musicDB != null) {
			PlayListEntry entry = musicPlayer.getPlayList().getCurrentlyPlaying();
			if (entry != null) {
				long seconds = TimeUnit.MICROSECONDS.toSeconds(musicPlayer.getCurrentPosition());
				long minutes = 0;
				if (seconds > 59) {
					minutes = seconds / 60;
					seconds = seconds % 60;
				}
				Map song = musicDB.getSongDataForSongIndex(entry.getSongIndex());
				currentlyPlaying.put("currentlyPlaying", song.get("author") + " : "+song.get("title"));
				currentlyPlaying.put("currentPosition", minutes + " mins "+ seconds + " secs");
			}
			else {
				currentlyPlaying.put("currentlyPlaying", "Nothing");
				currentlyPlaying.put("currentPosition", "0 Secs");
			}
			currentlyPlaying.put("currentState", musicPlayer.getState().getState().ordinal());
			currentlyPlaying.put("currentVolume", new Float(musicPlayer.getVolume()*10));
		} else {
			currentlyPlaying.put("currentState", new Integer(3));
		}
		JSONSerializer.serialize(w, currentlyPlaying, true);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void writeCurrentlyStreaming(Writer w, PlayList playList) throws IOException {
		Map currentlyStreaming = new HashMap();
		if (musicDB != null) {
			PlayListEntry entry = playList.getCurrentlyPlaying();
			if (entry != null) {
				long seconds = TimeUnit.MICROSECONDS.toSeconds(musicPlayer.getCurrentPosition());
				long minutes = 0;
				if (seconds > 59) {
					minutes = seconds / 60;
					seconds = seconds % 60;
				}
				Map song = musicDB.getSongDataForSongIndex(entry.getSongIndex());
				currentlyStreaming.put("currentState", new Integer(1));
				currentlyStreaming.put("currentlyPlaying", song.get("author") + " : "+song.get("title"));
				currentlyStreaming.put("currentPosition", minutes + " mins "+ seconds + " secs");
				currentlyStreaming.put("length", entry.getSongIndex().get("length"));
				currentlyStreaming.put("offset", entry.getSongIndex().get("offset"));
			}
			else {
				currentlyStreaming.put("currentState", new Integer(0));
				currentlyStreaming.put("currentlyPlaying", "Nothing");
				currentlyStreaming.put("currentPosition", "0 Secs");
			}
			currentlyStreaming.put("currentPlayIndex", new Integer(playList.getCurrentIndex()+1));
		} else {
			currentlyStreaming.put("currentState", new Integer(3));
		}
		JSONSerializer.serialize(w, currentlyStreaming, true);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void writePlayList(Writer w, Iterator<PlayListEntry> itr) throws IOException{
		Map playListStore = new HashMap();
		playListStore.put("identifier", "id");
		playListStore.put("label", "title");
		List items = new ArrayList();
		playListStore.put("items", items);
		List<Long> list = new ArrayList<Long>();
		while (itr.hasNext()) {
			PlayListEntry entry = itr.next();
			Map songIndex = entry.getSongIndex(); 
			Map song = entry.getSongData();
			Map songData = new HashMap();
			Long offset = (Long)songIndex.get("offset");
			Long length = (Long)songIndex.get("length");
			Long id = offset + length;
			list.add(id);
			songData.put("id", id);
			songData.put("title", song.get("title"));
			songData.put("artist", song.get("author"));
			songData.put("album", song.get("album"));
			songData.put("type", song.get("type"));
			songData.put("offset", songIndex.get("offset"));
			songData.put("length", songIndex.get("length"));
			items.add(songData);
		}
		JSONSerializer.serialize(w, playListStore, true);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addAlbumToPlayList(Writer w, String albumName, String artist, PlayList playList) throws IOException {
		List<Map<String, Number>> songIndexes = musicDB.getSongsForAlbum(albumName, artist);
		Map songs = new TreeMap();
		int i = 0;
		for (Map<String, Number> songIndex : songIndexes) {
			Map songData = musicDB.getSongDataForSongIndex(songIndex);
			Number track = (Number)songData.get("mp3.id3tag.track");
			int trackNumber = ++i;
			if (track != null) {
				trackNumber = track.intValue();
			}
			Map song = new HashMap();
			song.put("offset", songIndex.get("offset"));
			song.put("length", songIndex.get("length"));
			songs.put(new Integer(trackNumber), song);
		}
		for (Iterator itr = songs.values().iterator(); itr.hasNext();) {
			Map songData = (Map)itr.next();
			Map songIndex = new HashMap();
			songIndex.put("offset", songData.get("offset"));
			songIndex.put("length", songData.get("length"));
			playList.add(songIndex);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void createRandomPlayList(Writer w, PlayList playList, String artist) throws IOException {
		List<String> albums = null;
		int numberOfSongs = 50;
		if (artist != null) {
			albums = musicDB.getAlbumsForArtist(artist);
			numberOfSongs = 20;
		} else {
			albums = musicDB.getAllAlbums();
		}
		for (int i = 0; i < numberOfSongs; i++) {
			int randomAlbumIndex = random.nextInt(albums.size());
			String randomAlbum = albums.get(randomAlbumIndex);
			List<Map<String,Number>> randomSongs = musicDB.getSongsForAlbum(randomAlbum, null);
			for (int J = 0; J < 5; J++) {
				if (playList.add(getRandomSong(randomSongs))) {
					break;
				}
			}
		}
		Map songs = new TreeMap();
		int i = 0;
		for (Iterator<PlayListEntry> itr = musicPlayer.getPlayList().getPlayList(); itr.hasNext();) {
			PlayListEntry entry = itr.next();
			Map songIndex = entry.getSongIndex(); 
			Map song = new HashMap();
			Integer id = new Integer(++i);
			song.put("offset", songIndex.get("offset"));
			song.put("length", songIndex.get("length"));
			songs.put(id, song);
		}
		for (Iterator itr = songs.values().iterator(); itr.hasNext();) {
			Map songData = (Map)itr.next();
			Map songIndex = new HashMap();
			songIndex.put("offset", songData.get("offset"));
			songIndex.put("length", songData.get("length"));
		}
	}
	
	private Map<String,Number> getRandomSong(List<Map<String,Number>> randomSongs) {
		int randomSongIndex = random.nextInt(randomSongs.size());
		Map<String,Number> randomSong = randomSongs.get(randomSongIndex);
		return randomSong;
	}

	private static String[] getSegments(String pathInfo) {
        String[] segments = null;

        StringTokenizer st = new StringTokenizer(pathInfo, "/"); //$NON-NLS-1$
        segments = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            segments[i++] = st.nextToken();
        }
        return segments;
    }
	
	private synchronized PlayList getSessionPlayList(HttpSession session) {
		PlayList playList = null;
		playList = (PlayList)session.getAttribute("StreamPlayList");
		if (playList == null) {
			playList = new PlayList(musicDB);
			session.setAttribute("StreamPlayList", playList);
		}
		
		return playList;
	}
	
	private void initializeDB(boolean writeConfig) {
		logger.logp(Level.INFO, getClass().getName(), "initializeDB", "rootDir = ["+rootDir+"] storageDir = ["+storageDir+"]");
        musicDB = new MusicDB(new File(rootDir), new File(storageDir), skipScan);
        getServletContext().setAttribute("musicDB", musicDB);
        if (musicDB.start()) {
            musicPlayer = new MusicPlayer(musicDB);
            getServletContext().setAttribute("musicPlayer", musicPlayer);
            if (writeConfig) {
        		File configFile = new File(tempDir, "config.properties");
        		Properties configProps = new Properties();
        		configProps.setProperty("rootDir", rootDir);
        		configProps.setProperty("storageDir", storageDir);
        		OutputStream os = null;
        		try {
        			os = new FileOutputStream(configFile);
        			configProps.store(os, "");
        			logger.logp(Level.INFO, getClass().getName(), "initializeDB", "Config written to ["+configFile+"]");
        		} catch (IOException e) {
        			e.printStackTrace();
        		} finally {
    				if (os != null) { try { os.close(); } catch (IOException e) {}}
        		}
            }
        } else {
        	musicDB = null;
        }
	}
}
