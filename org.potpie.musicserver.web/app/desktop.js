define(['dijit/registry',
    	'dojo/parser',
        './servicehandler',
        'dojo/_base/connect',
        'dojo/dom-class',
        'dojo/dom',
        'dojo/data/ItemFileReadStore',
        'dojo/data/ItemFileWriteStore',
        'dojox/grid/DataGrid',
        'dijit/Dialog',
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
], function (registry, parser, servicehandler, connector, domClass, dom, ItemFileReadStore, ItemFileWriteStore, DataGrid, Dialog) {
	parser.parse();

	var STATE = {"STOPPED" : 0, "PLAYING" : 1, "PAUSED" : 2, "UNINITIALIZED" : 3};
	var albums = registry.byId("albums");
	var artists = registry.byId("artists");
	var playList = registry.byId("playList");
	var tabContainer = registry.byId("tabContainer");
	var accordion = registry.byId("accordion");
	var songs = registry.byId("songs");
	var volumeSlider = registry.byId("volume");
	var configDialog = registry.byId("configDialog");
	var okButton = registry.byId("okButton");

	var artistsStore;
	var songsStore;
	var albumsStore;
	var playListStore;
	
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
					playListStore = new ItemFileWriteStore({url: "./service/playList"});
					playList.setStore(playListStore);
					artistsStore = new ItemFileWriteStore({url: "./service/artist"});
					artists.setStore(artistsStore);
					albumsStore = new ItemFileWriteStore({url: "./service/album"});
					albums.setStore(albumsStore);
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
		albums.filter({album: albumFilter.attr("value")+"*"});
    });
	var artistFilter = registry.byId("artistFilter");
	connector.connect(artistFilter, "onChange", function() {
		artists.filter({artist: artistFilter.attr("value")+"*"});
    });
	var songFilter = registry.byId("songFilter");
	connector.connect(songFilter, "onChange", function() {
		songs.filter({title: songFilter.attr("value")+"*"});
    });
	
	var loadSongs = function(response, args) {
		songsStore = new ItemFileWriteStore({data: response});
		songs.setStore(songsStore, null, null);
	};
	
	var loadAlbums = function(response, args) {
		albumsStore = new ItemFileWriteStore({data: response});
		albums.setStore(albumsStore, null, null);
	};
	
	var playListResult = function(response) {
		playListStore = new ItemFileWriteStore({url: "./service/playList"});
		playList.setStore(playListStore);
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
		albumsStore = new dojo.data.ItemFileReadStore({url: "./service/album"});
		albums.setStore(albumsStore, null, null);
	};
	
	var updatePlayList = function() {
		playListStore = new ItemFileWriteStore({url: "./service/playList"});
		playList.setStore(playListStore);
	};
	
	var addSelectedSongsToPlayList = function() {
		var onComplete = function(items, request){
			var playListSongs = [];
			for (var i = 0; i < items.length; i++) {
				var song = {offset : items[i].offset[0], length : items[i].length[0]};
				playListSongs.push(song);
			}
			var dfd = servicehandler.addToPlayList(playListSongs);
			dfd.addCallbacks(updatePlayList, requestFailed);
			tabContainer.selectChild(registry.byId("playListContainer"));
		};
		var onError = function(error, request){
			console.debug("error : "+ error);
		};
		songsStore.fetch({query:{select:true}, onComplete: onComplete, onError: onError});
	};
	
	var removeSelectedSongsFromPlayList = function() {
		var onComplete = function(items, request){
			var playListSongs = [];
			for (var i = 0; i < items.length; i++) {
				var song = {offset : items[i].offset[0], length : items[i].length[0]};
				playListSongs.push(song);
			}
			var dfd = servicehandler.removeFromPlayList(playListSongs);
			dfd.addCallbacks(updatePlayList, requestFailed);
		};
		var onError = function(error, request){
			console.debug("error : "+ error);
		};
		playListStore.fetch({query:{select:true}, onComplete: onComplete, onError: onError});
	};
	
	var artistClicked = function(e) {
		currentArtist = artists.getItem(e.rowIndex);
		var dfd = servicehandler.getAlbumsForArtist(currentArtist.artist);
		dfd.addCallbacks(loadAlbums, requestFailed);
		tabContainer.selectChild(registry.byId("albumsContainer"));
	};
	artists.connect(artists, "onRowClick", artistClicked);
	
	var albumClicked = function(e) {
		currentAlbum = albums.getItem(e.rowIndex);
		if (currentArtist && currentArtist.artist) {
			var dfd = servicehandler.getSongsForAlbum(currentAlbum.album, currentArtist.artist);
		} else {
			var dfd = servicehandler.getSongsForAlbum(currentAlbum.album);
		}
		dfd.addCallbacks(loadSongs, requestFailed);
		tabContainer.selectChild(registry.byId("songsContainer"));
	};
	albums.connect(albums, "onRowClick", albumClicked);
	
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