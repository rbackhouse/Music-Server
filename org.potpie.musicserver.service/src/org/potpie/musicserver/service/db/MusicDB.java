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
package org.potpie.musicserver.service.db;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

import org.dojotoolkit.json.JSONParser;
import org.dojotoolkit.json.JSONSerializer;
import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.VorbisComment;
import org.tritonus.share.sampled.file.TAudioFileFormat;


public class MusicDB {
	private static Logger logger = Logger.getLogger("org.potpie.musicserver.service.db");
	
	private File root = null;
	private File storageDir = null;
	private RandomAccessFile songs = null;
	private MpegAudioFileReader mpegAudioFileReader = null;
	private Map<String, List<String>> artistsToAlbums = null;
	private Map<String, List<String>> artistIdsToArtists = null;
	private Map<String, List<Map<String, Number>>> artistsToSongs = null;
	private Map<String, List<Map<String, Number>>> albumsToSongs = null;
	private Map<String, Object> dirinfos = null;
	private List<String> invalid = null;
	private long offset = 0;
	private boolean saveFiles = false;
	private boolean skipScan = false;
	
	public MusicDB(File root, File storageDir, boolean skipScan) {
		this.root = root;
		this.storageDir = storageDir;
		this.skipScan = skipScan;
		mpegAudioFileReader = new MpegAudioFileReader();
		artistsToAlbums = new HashMap<String, List<String>>();
		artistIdsToArtists = new HashMap<String, List<String>>();
		artistsToSongs = new HashMap<String, List<Map<String, Number>>>();
		albumsToSongs = new HashMap<String, List<Map<String, Number>>>();
		dirinfos = new HashMap<String, Object>();
		invalid = new ArrayList<String>();
	}
	
	public void start() {
		if (!root.exists()) {
			logger.logp(Level.SEVERE, getClass().getName(), "start", "Music directory ["+root.getPath()+"] does not exist");
			return;
		}
		File songsFile = new File(storageDir, "songs.json");
		if (songsFile.exists()) {
			Reader r = null;
			try {
				songs = new RandomAccessFile(songsFile, "rwd");
				offset = songs.length();
				songs.seek(offset);
				r = new BufferedReader(new FileReader(new File(storageDir, "dirinfos.json")));
				dirinfos = (Map<String, Object>)JSONParser.parse(r);
				
				r = new BufferedReader(new FileReader(new File(storageDir, "artistsToSongs.json")));
				artistsToSongs = (Map<String, List<Map<String, Number>>>)JSONParser.parse(r);
				r.close();
				
				r = new BufferedReader(new FileReader(new File(storageDir, "artistIdsToArtists.json")));
				artistIdsToArtists = (Map<String, List<String>>)JSONParser.parse(r);
				r.close();
				
				r = new BufferedReader(new FileReader(new File(storageDir, "artistsToAlbums.json")));
				artistsToAlbums = (Map<String, List<String>>)JSONParser.parse(r);
				r.close();
				
				r = new BufferedReader(new FileReader(new File(storageDir, "albumsToSongs.json")));
				albumsToSongs = (Map<String, List<Map<String, Number>>>)JSONParser.parse(r);
				if (!skipScan) {
					logger.logp(Level.INFO, getClass().getName(), "start", "Started scanning ["+root.getPath()+"]");
					scan(root, dirinfos);
					logger.logp(Level.INFO, getClass().getName(), "start", "Completed scanning ["+root.getPath()+"]");
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {if (r != null)r.close();}catch(IOException e){}
			}
		}
		else {
			try {
				storageDir.mkdirs();
				songs = new RandomAccessFile(new File(storageDir, "songs.json"), "rwd");
				dirinfos.put("name", root.getPath());
				dirinfos.put("files", new ArrayList<String>());
				dirinfos.put("dirs", new ArrayList<Map<String, Object>>());
				logger.logp(Level.INFO, getClass().getName(), "start", "Started scanning ["+root.getPath()+"]");
				scan(root, dirinfos);
				logger.logp(Level.INFO, getClass().getName(), "start", "Completed scanning ["+root.getPath()+"]");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (saveFiles) {
			logger.logp(Level.CONFIG, getClass().getName(), "start", "Files need saving");
			Writer w = null;
			try {
				w = new BufferedWriter(new FileWriter(new File(storageDir, "dirinfos.json")));
				JSONSerializer.serialize(w, dirinfos, true);
				w = new BufferedWriter(new FileWriter(new File(storageDir, "artistsToSongs.json")));
				JSONSerializer.serialize(w, artistsToSongs, true);
				w.close();
				w = new BufferedWriter(new FileWriter(new File(storageDir, "artistIdsToArtists.json")));
				JSONSerializer.serialize(w, artistIdsToArtists, true);
				w.close();
				w = new BufferedWriter(new FileWriter(new File(storageDir, "artistsToAlbums.json")));
				JSONSerializer.serialize(w, artistsToAlbums, true);
				w.close();
				w = new BufferedWriter(new FileWriter(new File(storageDir, "albumsToSongs.json")));
				JSONSerializer.serialize(w, albumsToSongs, true);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {if (w != null)w.close();}catch(IOException e){}
			}
		}
		logger.logp(Level.INFO, getClass().getName(), "start", "Total Artists = "+artistsToSongs.keySet().size());
		logger.logp(Level.INFO, getClass().getName(), "start", "Total Albums = "+albumsToSongs.keySet().size());
		for (String invalidFile : invalid) {
			logger.logp(Level.SEVERE, getClass().getName(), "start", "Invalid album or artist : "+invalidFile);
		}
	}
	
	public void stop() {
		if (songs != null) {
			try {
				songs.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public synchronized List<String> getAllArtists() {
		Set<String> set = new TreeSet<String>();
		for (String artistId : artistIdsToArtists.keySet()) {
			set.add(getBestFitArtistName(artistId));
		}
		return new ArrayList<String>(set);
	}
	
	public synchronized List<String> getAllAlbums() {
		return new ArrayList<String>(new TreeSet<String>(albumsToSongs.keySet()));
	}

	public synchronized List<String> getAlbumsForArtist(String artist) {
		String artistId = getArtistIdForArtist(artist);
		return artistsToAlbums.get(artistId);
	}
	
	public synchronized String getArtistForAlbum(String album) {
		for (String artist : artistsToAlbums.keySet()) {
			List<String> albums = artistsToAlbums.get(artist);
			if (albums.contains(album)) {
				return getBestFitArtistName(artist);
			}
		}
		return null;
	}
	
	public synchronized List<Map<String, Number>> getSongsForAlbum(String album, String artist) {
		List<Map<String, Number>> albumSongs = albumsToSongs.get(album);
		Map<Long, Map<String, Number>> songMap = new TreeMap<Long, Map<String, Number>>();
		long defaultTrack = 0;
		for (Map<String, Number> song : albumSongs) {
			Map songData = getSongDataForSongIndex(song);
			if (artist != null) {
				String author = (String)songData.get("author");
				if (author == null) {
					continue;
				}
				String authorId = getArtistIdForArtist(author);
				if (authorId == null) {
					continue;
				}
				String artistId = getArtistIdForArtist(artist);
				if (artistId == null) {
					continue;
				}
				if (!authorId.equals(artistId)) {
					continue;
				}
			}
			Long track = (Long)songData.get("mp3.id3tag.track");
			if (track == null) {
				track = ++defaultTrack;
			}
			songMap.put(track, song);
		}
		return new ArrayList<Map<String, Number>>(songMap.values());
	}
	
	public synchronized List<Map<String, Number>> getSongsForArtist(String artist) {
		String artistId = getArtistIdForArtist(artist);
		return artistsToSongs.get(artistId);
	}
	
	public synchronized Map getSongDataForSongIndex(Map<String, Number> songIndex) {
		long offset = songIndex.get("offset").longValue();
		int length = songIndex.get("length").intValue();
		Map songData = null;
		try {
			songs.seek(offset);
			byte[] bytes = new byte[length];
			songs.read(bytes);
			String songDetails = new String(bytes);
			songData = (Map)JSONParser.parse(new StringReader(songDetails));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return songData;
	}
	
	private String getArtistIdForArtist(String artist) {
		String id = null;
		
		for (String artistId : artistIdsToArtists.keySet()) {
			List<String> artistNames = artistIdsToArtists.get(artistId);
			for (String artistName: artistNames) {
				if (artistName.equals(artist)) {
					id = artistId;
					break;
				}
			}
			if (id != null) {
				break;
			}
		}
		return id;
	}
	
	private String getBestFitArtistName(String artistId) {
		List<String> artistNames = artistIdsToArtists.get(artistId);
		
		if (artistNames.size() == 1) {
			return artistNames.get(0);
		}
		String bestFit = null;
		for (String artistName: artistNames) {
			bestFit = artistName;
			if (Character.isLowerCase(artistName.charAt(0))) {
				bestFit = null;
			}
			if (artistName.toLowerCase().startsWith("the ")) {
				bestFit = null;
			}
			if (artistName.contains("&")) {
				bestFit = null;
			}
			if (artistName.toLowerCase().endsWith(", the")) {
				bestFit = null;
			}
			
			if (bestFit != null) {
				break;
			}
		}
		if (bestFit == null) {
			bestFit = artistNames.get(0);
		}
		return bestFit;
	}
	
	private void scan(File dir, Map<String, Object> parentDirinfo) {
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				List<Map<String, Object>> dirinfos = (List<Map<String, Object>>)parentDirinfo.get("dirs");
				Map<String, Object> dirinfo = null;
				for (Map<String, Object> m : dirinfos) {
					if (m.get("name").equals(file.getPath())) {
						dirinfo = m;
						break;
					}
				}
				if (dirinfo == null) {
					dirinfo = new HashMap<String, Object>();
					dirinfo.put("name", file.getPath());
					dirinfo.put("files", new ArrayList<String>());
					dirinfo.put("dirs", new ArrayList<Map<String, Object>>());
					dirinfos.add(dirinfo);
				}
				scan(file, dirinfo);
			}
			else {
				List<String> fileinfos = (List<String>)parentDirinfo.get("files");
				if (!fileinfos.contains(file.getPath())) {
					try {
						AudioFileFormat aff = mpegAudioFileReader.getAudioFileFormat(file);
				        String type = aff.getType().toString();
				        if (type.equalsIgnoreCase("mp3")) {
				            if (aff instanceof TAudioFileFormat) {
				                Map props = ((TAudioFileFormat) aff).properties();
				                Map entry = new HashMap();
				                String artist = null;
				                String album = null;
				                String title = null;
				                
				                if (props.containsKey("mp3.channels")) entry.put("mp3.channels", ((Integer) props.get("mp3.channels")).intValue());
				                if (props.containsKey("mp3.frequency.hz")) entry.put("mp3.frequency.hz", ((Integer) props.get("mp3.frequency.hz")).intValue());
				                if (props.containsKey("mp3.bitrate.nominal.bps")) entry.put("mp3.bitrate.nominal.bps", ((Integer) props.get("mp3.bitrate.nominal.bps")).intValue());
				                if (props.containsKey("mp3.version.layer")) entry.put("mp3.version.layer", "Layer " + props.get("mp3.version.layer"));
				                if (props.containsKey("mp3.version.mpeg")) {
				                	String version = (String) props.get("mp3.version.mpeg");
				                    if (version.equals("1")) version = "MPEG1";
				                    else if (version.equals("2")) version = "MPEG2-LSF";
				                    else if (version.equals("2.5")) version = "MPEG2.5-LSF";
				                    entry.put("mp3.version.mpeg", version);
				                }
				                if (props.containsKey("mp3.mode")) {
					                String channelsMode = null;
				                    int mode = ((Integer) props.get("mp3.mode")).intValue();
				                    if (mode == 0) channelsMode = "Stereo";
				                    else if (mode == 1) channelsMode = "Joint Stereo";
				                    else if (mode == 2) channelsMode = "Dual Channel";
				                    else if (mode == 3) channelsMode = "Single Channel";
				                    entry.put("mp3.mode", channelsMode);
				                }
				                if (props.containsKey("mp3.crc")) entry.put("mp3.crc", ((Boolean) props.get("mp3.crc")).booleanValue());
				                if (props.containsKey("mp3.vbr")) entry.put("mp3.vbr", ((Boolean) props.get("mp3.vbr")).booleanValue());
				                if (props.containsKey("mp3.copyright")) entry.put("mp3.copyright",((Boolean) props.get("mp3.copyright")).booleanValue());
				                if (props.containsKey("mp3.original")) entry.put("mp3.original" ,((Boolean) props.get("mp3.original")).booleanValue());
				                if (props.containsKey("title")) {
				                	title = (String) props.get("title");
				                	entry.put("title", title);
				                }
				                if (props.containsKey("author")) {
				                	artist = (String) props.get("author");
				                	artist = artist.trim();
				                	entry.put("author", artist);
				                }
				                if (props.containsKey("album")) {
				                	album = (String) props.get("album");
				                	album = (album == null) ? "" : album;
				                	album = album.trim();
				                	entry.put("album", album);
				                }
				                if (props.containsKey("date")) entry.put("date",(String) props.get("date"));
				                if (props.containsKey("duration")) entry.put("duration", (long) Math.round((((Long) props.get("duration")).longValue()) / 1000000));
				                if (props.containsKey("mp3.id3tag.genre")) entry.put("mp3.id3tag.genre", (String) props.get("mp3.id3tag.genre"));
				                if (props.containsKey("mp3.id3tag.track")) {
				                    try {
				                    	entry.put("mp3.id3tag.track", Integer.parseInt((String) props.get("mp3.id3tag.track")));
				                    }
				                    catch (NumberFormatException e1) {}
				                }
				                entry.put("path", file.getPath());
				                entry.put("type", "mp3");
				                writeInfo(entry, artist, album);
								saveFiles = true;
								fileinfos.add(file.getPath());
				            }
				        }
					} catch (UnsupportedAudioFileException e) {
						if (e.getMessage().equals("FLAC stream found")) {
							InputStream is = null;
							try {
								is = new FileInputStream(file);
								FLACDecoder decoder = new FLACDecoder(is);
								Metadata[] metadatas = decoder.readMetadata();
								for (Metadata metadata : metadatas) {
					            	if (metadata instanceof VorbisComment) {
					            		VorbisComment comment = (VorbisComment)metadata;
					            		String[] title = comment.getCommentByName("TITLE");
					            		String[] artist = comment.getCommentByName("ARTIST");
					            		String[] album = comment.getCommentByName("ALBUM");
					            		String[] track = comment.getCommentByName("TRACKNUMBER");
						                Map entry = new HashMap();
						                if (artist != null && artist.length > 0) {
						                	entry.put("author", artist[0]);
						                }
						                if (album != null && album.length > 0) {
						                	entry.put("album", album[0]);
						                }
						                if (title != null && title.length > 0) {
						                	entry.put("title", title[0]);
						                }
						                if (track != null && track.length > 0) {
						                    try {
						                    	entry.put("mp3.id3tag.track", Integer.parseInt((String) track[0]));
						                    }
						                    catch (NumberFormatException e1) {}
						                	
						                }
						                entry.put("path", file.getPath());
						                entry.put("type", "flac");
						                writeInfo(entry, artist[0], album[0]);
										saveFiles = true;
										fileinfos.add(file.getPath());
					            	}
								}
							} catch (IOException e1) {
							}finally {
								if (is != null){try{is.close();}catch(IOException ex){}}
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private void writeInfo(Map entry, String artist, String album) throws IOException {
        StringWriter sw = new StringWriter();
        JSONSerializer.serialize(sw, entry, true);
        if (artist == null || artist.equals("")) {
        	invalid.add((String)entry.get("path"));
        	return;
        }
        if (album == null || album.equals("")) {
        	invalid.add((String)entry.get("path"));
        	return;
        }
        songs.writeBytes(sw.toString());
        int length = sw.toString().getBytes().length;
        Map<String, Number> songInfo = new HashMap<String, Number>();
        songInfo.put("length", new Long(length));
        songInfo.put("offset", new Long(offset));
        offset += length;
        
        if (artist.indexOf('/') != -1) {
        	artist = artist.replace('/', '-');
        }
        
        if (album.indexOf('/') != -1) {
        	album = album.replace('/', '-');
        }
        
        String artistId = artist.toLowerCase();
        if (artistId.startsWith("the ") && !artistId.equals("the the")) {
        	artistId = artistId.substring(4);
        }
        if (artistId.indexOf('&') != -1) {
        	artistId = artistId.replace("&", "and");
        }
        if (artistId.endsWith(", the")) {
        	artistId = artistId.substring(0, artistId.indexOf(", the"));
        }
        List<String> artistNames = artistIdsToArtists.get(artistId);
        if (artistNames == null) {
        	artistNames = new ArrayList<String>();
        	artistIdsToArtists.put(artistId, artistNames);
    		logger.logp(Level.FINE, getClass().getName(), "writeInfo", "Artist Id ["+artistId+"]");
        }
        if (!artistNames.contains(artist)) {
        	artistNames.add(artist);
    		logger.logp(Level.FINE, getClass().getName(), "writeInfo", "Artist ["+artist+"]");
        }
        
        String albumName = album + " ("+entry.get("type") +")";
        
        List<Map<String, Number>> artistSongList = artistsToSongs.get(artistId);
        if (artistSongList == null) {
        	artistSongList = new ArrayList<Map<String, Number>>();
        	artistsToSongs.put(artistId, artistSongList);
        }
        artistSongList.add(songInfo);
        
        List<String> albumList = artistsToAlbums.get(artistId);
        if (albumList == null) {
        	albumList = new ArrayList<String>();
        	artistsToAlbums.put(artistId, albumList);
        }
        if (!albumList.contains(albumName)) {
        	albumList.add(albumName);
        }
        List<Map<String, Number>> albumSongList = albumsToSongs.get(albumName);
    	if (albumSongList == null) {
    		logger.logp(Level.FINE, getClass().getName(), "writeInfo", "Album ["+albumName+"]");
    		albumSongList = new ArrayList<Map<String, Number>>();
    		albumsToSongs.put(albumName, albumSongList);
    	}
    	albumSongList.add(songInfo);
	}
}
