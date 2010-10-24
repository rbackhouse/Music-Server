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
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
	private String root = null;
	private String storageDir = null;
    
    public MusicServerServlet() {}

    public MusicServerServlet(String root, String storageDir) {
    	this.root = root;
    	this.storageDir = storageDir;
    }
    
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		if (root == null) {
			root = config.getServletContext().getInitParameter("root");
		}
		if (storageDir == null) {
			storageDir = config.getServletContext().getInitParameter("storageDir");
		}
		if (storageDir != null && root != null) {
			logger.logp(Level.INFO, getClass().getName(), "init", "root = ["+root+"] storageDir = ["+storageDir+"]");
			boolean skipScan = false;
			String strSkipScan = config.getServletContext().getInitParameter("skipScan");
			if (strSkipScan != null) {
				skipScan = Boolean.valueOf(strSkipScan);
			}
	        musicDB = new MusicDB(new File(root), new File(storageDir), skipScan);
	        getServletContext().setAttribute("musicDB", musicDB);
	        musicDB.start();
	        musicPlayer = new MusicPlayer(musicDB);
	        getServletContext().setAttribute("musicPlayer", musicPlayer);
		}
	}

	public void destroy() {
		super.destroy();
		musicDB.stop();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (musicDB == null) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server failed to initialize correctly. Check the logs");
			return;
		}
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
					}
					else {
						for (String artist: artists) {
							Map artistData = new HashMap();
							artistData.put("artist", artist);
							items.add(artistData);
						}
					}
					JSONSerializer.serialize(w, artistDataStore, true);
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
		if (musicDB == null) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server failed to initialize correctly. Check the logs");
			return;
		}
		resp.setContentType("test/json");
		String[] segments = getSegments(req.getPathInfo());
		if (segments.length > 0) {
			if (segments[0].equals("addToPlayList")) {
				List<Map<String, Number>> songs = (List<Map<String, Number>>)JSONParser.parse(req.getReader());
				for (Map<String, Number> song : songs) {
					musicPlayer.getPlayList().add(song);
				}
			}
			if (segments[0].equals("addToStreamPlayList")) {
				List<Map<String, Number>> songs = (List<Map<String, Number>>)JSONParser.parse(req.getReader());
				for (Map<String, Number> song : songs) {
					PlayList playList = getSessionPlayList(req.getSession());
					playList.add(song);
				}
			}
			else if (segments[0].equals("removeFromPlayList")) {
				List<Map<String, Number>> songs = (List<Map<String, Number>>)JSONParser.parse(req.getReader());
				for (Map<String, Number> song : songs) {
					musicPlayer.getPlayList().remove(song);
				}
			}
			else if (segments[0].equals("removeFromStreamPlayList")) {
				List<Map<String, Number>> songs = (List<Map<String, Number>>)JSONParser.parse(req.getReader());
				for (Map<String, Number> song : songs) {
					PlayList playList = getSessionPlayList(req.getSession());
					playList.remove(song);
				}
			}
			else if (segments[0].equals("stream")) {
				PlayList playList = getSessionPlayList(req.getSession());
				MusicStreamer.stream(playList, req, resp);
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
		JSONSerializer.serialize(w, currentlyPlaying, true);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void writeCurrentlyStreaming(Writer w, PlayList playList) throws IOException {
		Map currentlyStreaming = new HashMap();
		PlayListEntry entry = playList.getCurrentlyPlaying();
		if (entry != null) {
			long seconds = TimeUnit.MICROSECONDS.toSeconds(musicPlayer.getCurrentPosition());
			long minutes = 0;
			if (seconds > 59) {
				minutes = seconds / 60;
				seconds = seconds % 60;
			}
			Map song = musicDB.getSongDataForSongIndex(entry.getSongIndex());
			currentlyStreaming.put("currentlyPlaying", song.get("author") + " : "+song.get("title"));
			currentlyStreaming.put("currentPosition", minutes + " mins "+ seconds + " secs");
			currentlyStreaming.put("length", entry.getSongIndex().get("length"));
			currentlyStreaming.put("offset", entry.getSongIndex().get("offset"));
		}
		else {
			currentlyStreaming.put("currentlyPlaying", "Nothing");
			currentlyStreaming.put("currentPosition", "0 Secs");
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
}
