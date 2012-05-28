define(['dijit/registry',
    	'dojo/parser',
        './servicehandler',
        'dojo/_base/connect',
        'dojo/dom-class',
        'dojo/dom',
        'dgrid/Selection',
        'dgrid/Keyboard',
        'dgrid/OnDemandGrid',
        'dgrid/editor',
        'dojo/store/Memory',
        'dijit/Dialog',
        'dojo/_base/declare',
		'dojo/_base/array',
        'dijit/layout/BorderContainer',
        'dijit/layout/ContentPane',
        'dijit/layout/TabContainer',
        'dijit/form/TextBox',
        'dijit/form/Button',
        'dijit/Toolbar',
        'dijit/form/HorizontalSlider',
        'dijit/form/VerticalSlider',
        'dijit/layout/AccordionContainer',
        'dijit/ToolbarSeparator',
        'dojo/domReady!'
], function (registry, 
		parser, 
		servicehandler, 
		connector, 
		domClass, 
		dom, 
		Selection, 
		Keyboard, 
		Grid, 
		editor, 
		MemoryStore, 
		Dialog, 
		declare, 
		arrayUtil) {
	parser.parse();

	var STATE = {"STOPPED" : 0, "PLAYING" : 1, "PAUSED" : 2, "UNINITIALIZED" : 3};
	
	var artistsStore;
	var songsStore;
	var albumsStore;
	var playListStore;
	
	var CustomGrid = declare([Grid, Selection, Keyboard]);
	
	var albums = new CustomGrid({selectionMode: "single", columns: {album: {label: "Album", field: "album"}, artist: {label: "Artist", field: "artist"}}}, "albums");
	
	var artists = new CustomGrid({selectionMode: "single", columns: {artist: {label: "Artist", field: "artist"}}}, "artists");
	
	var playList = new CustomGrid({
		selectionMode: "single",
		columns: {
			select: editor({
				label: " ",
				autoSave: true,
				sortable: false
			}, "checkbox"),
			title: {label: "Title", field: "title"},
			artist: {label: "Artist", field: "artist"},
			album: {label: "Album", field: "album"},
			type: {label: "Type", field: "type"}
		}
	}, "playList");
	
	var songs = new CustomGrid({
		selectionMode: "single",
		columns: {
			select: editor({
				label: " ",
				autoSave: true,
				sortable: false
			}, "checkbox"),
			track: {label: "Track", field: "mp3.id3tag.track"},
			type: {label: "Type", field: "type"},
			title: {label: "Title", field: "title"}
		}
	}, "songs");

	var tabContainer = registry.byId("tabContainer");
	var accordion = registry.byId("accordion");
	var volumeSlider = registry.byId("volume");
	var configDialog = registry.byId("configDialog");
	var okButton = registry.byId("okButton");

	var currentArtist;
	var currentAlbum;
	var currentVolume;
	var currentState;
	var storesInitialized = false;
	
	var requestSuccessful = function(response) {
		if (response !== null && response.currentState !== undefined) {
			currentState = response.currentState;
			if (currentState !== STATE.UNINITIALIZED) {
				currentVolume = response.currentVolume;
				volumeSlider.attr("value", currentVolume); 
				var currentlyPlaying = dom.byId("title");
				while (currentlyPlaying.hasChildNodes()) {
					currentlyPlaying.removeChild(currentlyPlaying.firstChild);
				}
				currentlyPlaying.appendChild(document.createTextNode("Music Server - Currently Playing ["+response.currentlyPlaying+"] ["+response.currentPosition+"]"));
				if (!storesInitialized) {
					storesInitialized = true;
					updatePlayList();
					var dfd = servicehandler.getArtists();
					dfd.addCallbacks(loadArtists, requestFailed);
					var dfd = servicehandler.getAllAlbums();
					dfd.addCallbacks(loadAlbums, requestFailed);
				}
				setPlayPauseIcon();
				setTimeout(function(){ monitor(); }, 5000);
			} else {
				configDialog.show();
			}
		}
	};
	
	var requestFailed = function(errorObj) {
		alert("Request failed ["+errorObj.status+"]["+errorObj.statusText+"]");
	};
	
	accordion.watch("selectedChildWidget", function(name, oldv, newv){
		switch (newv.id) {
			case "artistsLeft":
				tabContainer.selectChild(registry.byId("artistsContainer"));
				break;
			case "albumsLeft":
				tabContainer.selectChild(registry.byId("albumsContainer"));
				break;
			case "songsLeft":
				tabContainer.selectChild(registry.byId("songsContainer"));
				break;
			case "playListLeft":
				tabContainer.selectChild(registry.byId("playListContainer"));
				break;
		}
	});
	
	tabContainer.watch("selectedChildWidget", function(name, oldv, newv){
		switch (newv.id) {
			case "artistsContainer":
				accordion.selectChild(registry.byId("artistsLeft"));
				break;
			case "albumsContainer":
				accordion.selectChild(registry.byId("albumsLeft"));
				break;
			case "songsContainer":
				accordion.selectChild(registry.byId("songsLeft"));
				break;
			case "playListContainer":
				accordion.selectChild(registry.byId("playListLeft"));
				break;
		}
	});
	
	var albumFilter = registry.byId("albumFilter");
	connector.connect(albumFilter, "onChange", function() {
		var value = albumFilter.attr("value");
		if (value !== "") {
			albums.query = function(obj) {
				if (obj.album.indexOf(value) === 0) {
					return true;
				} else {
					return false;
				}
			};
		} else {
			albums.query = {};
		}
		albums.refresh();
    });
	
	var artistFilter = registry.byId("artistFilter");
	connector.connect(artistFilter, "onChange", function() {
		var value = artistFilter.attr("value");
		if (value !== "") {
			artists.query = function(obj){
				if (obj.artist.indexOf(value) === 0) {
					return true; 
				} else {
					return false;
				}
			};
		} else {
			artists.query = {};
		}
		artists.refresh();
    });
	
	var songFilter = registry.byId("songFilter");
	connector.connect(songFilter, "onChange", function() {
		var value = songFilter.attr("value");
		if (value !== "") {
			songs.query = function(obj) {
				if (obj.title.indexOf(value) === 0) {
					return true;
				} else {
					return false;
				}
			};
		} else {
			songs.query = {};
		}
		songs.refresh();
    });
	
	var loadSongs = function(response, args) {
		songsStore = new MemoryStore({data: response.items});
		songs.set("store", songsStore);
		songs.refresh();
	};
	
	var loadAlbums = function(response, args) {
		albumsStore = new MemoryStore({idProperty:"album", data: response.items});
		albums.set("store", albumsStore);
		albums.refresh();
	};
	
	var loadArtists = function(response, args) {
		artistsStore = new MemoryStore({idProperty:"artist", data: response.items});
		artists.set("store", artistsStore);
		artists.refresh();
	};
	
	var playListResult = function(response) {
		updatePlayList();
		tabContainer.selectChild(registry.byId("playListContainer"));
	};
	
	var addAlbumSongsToPlayList = function() {
		if (currentAlbum) {
			if (currentArtist) {
				var dfd = servicehandler.addAlbumToPlayList(currentAlbum.album, currentArtist.artist);
			} else { 	
				var dfd = servicehandler.addAlbumToPlayList(currentAlbum.album);
			}
			dfd.addCallbacks(playListResult, requestFailed);
		}
	};
	
	var clearPlaylist = function() {
		var dfd = servicehandler.clearPlayList();
		dfd.addCallbacks(playListResult, requestFailed);
	};
	
	var randomPlaylist = function(e) {
		dojo.stopEvent(e);
		clearPlaylist();
		var dfd = servicehandler.randomPlaylist();
		dfd.addCallbacks(playListResult, requestFailed);
	};
	
	var randomForArtist = function(e) {
		dojo.stopEvent(e);
		if (currentArtist) {
			clearPlaylist();
			var dfd = servicehandler.randomPlaylist(currentArtist.artist);
			dfd.addCallbacks(playListResult, requestFailed);
		}
	};
	
	var onRewind = function(e) {
		dojo.stopEvent(e);
		var dfd = servicehandler.previous();
		dfd.addCallbacks(requestSuccessful, requestFailed);
	};
	
	var onPlayPause = function(e) {
		dojo.stopEvent(e);
		switch (currentState) {
			case STATE.STOPPED: {
				var dfd = servicehandler.play();
				dfd.addCallbacks(requestSuccessful, requestFailed);
				break;
			}
			case STATE.PLAYING: {
				var dfd = servicehandler.pause();
				dfd.addCallbacks(requestSuccessful, requestFailed);
				break;
			}
			case STATE.PAUSED: {
				var dfd = servicehandler.pause();
				dfd.addCallbacks(requestSuccessful, requestFailed);
				break;
			}
		}
	};
	
	var setPlayPauseIcon = function() {
		var buttonClass;
		switch (currentState) {
			case STATE.STOPPED:
			case STATE.PAUSED: {
				buttonClass = "playIcon";
				break;
			}
			case STATE.PLAYING: {
				buttonClass = "pauseIcon";
				break;
			}
		}
		registry.byId("musicserver.playPauseButton").attr("iconClass", buttonClass);
	};
	
	var onStop = function(e) {
		dojo.stopEvent(e);
		var dfd = servicehandler.stop();
		dfd.addCallbacks(requestSuccessful, requestFailed);
	};
	
	var onFastForward = function(e) {
		dojo.stopEvent(e);
		var dfd = servicehandler.next();
		dfd.addCallbacks(requestSuccessful, requestFailed);
	};
	
	var resetAlbums = function(e) {
		dojo.stopEvent(e);
		currentArtist = undefined;
		var dfd = servicehandler.getAllAlbums();
		dfd.addCallbacks(loadAlbums, requestFailed);
	};
	
	var updatePlayList = function() {
		var dfd = servicehandler.getPlaylist();
		dfd.addCallbacks(function(response){
			playListStore = new MemoryStore({data: response.items});
			playList.set("store", playListStore);
		}, requestFailed);
	};
	
	var addSelectedSongsToPlayList = function() {
		var items = songsStore.query({ select: true });
		var playListSongs = [];
		for (var i = 0; i < items.length; i++) {
			var song = {offset : items[i].offset, length : items[i].length};
			playListSongs.push(song);
		}
		var dfd = servicehandler.addToPlayList(playListSongs);
		dfd.addCallbacks(updatePlayList, requestFailed);
		tabContainer.selectChild(registry.byId("playListContainer"));
	};
	
	var removeSelectedSongsFromPlayList = function() {
		var items = playListStore.query({ select: true });
		var playListSongs = [];
		for (var i = 0; i < items.length; i++) {
			var song = {offset : items[i].offset, length : items[i].length};
			playListSongs.push(song);
		}
		var dfd = servicehandler.removeFromPlayList(playListSongs);
		dfd.addCallbacks(updatePlayList, requestFailed);
	};
	
	artists.on("dgrid-select", function(e) {
		currentArtist = e.rows[0].data;
		var dfd = servicehandler.getAlbumsForArtist(currentArtist.artist);
		dfd.addCallbacks(loadAlbums, requestFailed);
		tabContainer.selectChild(registry.byId("albumsContainer"));
	});
	
	albums.on("dgrid-select", function(e) {
		currentAlbum = e.rows[0].data;
		if (currentArtist && currentArtist.artist) {
			var dfd = servicehandler.getSongsForAlbum(currentAlbum.album, currentArtist.artist);
		} else {
			var dfd = servicehandler.getSongsForAlbum(currentAlbum.album);
		}
		dfd.addCallbacks(loadSongs, requestFailed);
		tabContainer.selectChild(registry.byId("songsContainer"));
	});
	
	connector.connect(registry.byId("addAllSongs"), "onClick", addAlbumSongsToPlayList);
	connector.connect(registry.byId("randomPlaylist"), "onClick", randomPlaylist);
	connector.connect(registry.byId("randomForArtist"), "onClick", randomForArtist);
	connector.connect(registry.byId("clearPlaylist"), "onClick", clearPlaylist);
	connector.connect(registry.byId("resetAlbums"), "onClick", resetAlbums);
	connector.connect(registry.byId("addSelectedSongs"), "onClick", addSelectedSongsToPlayList);
	connector.connect(registry.byId("removeSelectedSongs"), "onClick", removeSelectedSongsFromPlayList);
	
	connector.connect(registry.byId("musicserver.rewindButton"), "onClick", onRewind);
	connector.connect(registry.byId("musicserver.playPauseButton"), "onClick", onPlayPause);
	connector.connect(registry.byId("musicserver.stopButton"), "onClick", onStop);
	connector.connect(registry.byId("musicserver.fastForwardButton"), "onClick", onFastForward);
	
	connector.connect(volumeSlider, "onChange", function(value) {
		var volume = parseFloat(value) / 10;
		var dfd = servicehandler.setVolume(volume);
		dfd.addCallbacks(requestSuccessful, requestFailed);
    });

	connector.connect(okButton, "onClick", function(e) {
		var rootDir = registry.byId("rootDir").attr("value");
		var storageDir = registry.byId("storageDir").attr("value");
		console.log("rootDir = "+ rootDir + " storageDir = "+storageDir);
		var dfd = servicehandler.initialize(rootDir, storageDir);
		dfd.addCallbacks(requestSuccessful, requestFailed);
	});
	
	function monitor() {
		var dfd = servicehandler.currentlyPlaying();
		dfd.addCallbacks(requestSuccessful, requestFailed);
    };
	monitor();
	
	return {};
});