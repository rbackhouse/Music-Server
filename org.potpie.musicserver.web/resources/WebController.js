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
(function() {
dojo.provide("org.potpie.musicserver.web.WebController");

dojo.require("dijit.Toolbar");
dojo.require("dijit.form.Button");
dojo.require("dijit.Tooltip");
dojo.require("dijit.Menu");
dojo.require("dijit.layout.StackController");
dojo.require("dijit.layout.StackContainer");
dojo.require("dijit.layout.ContentPane");
dojo.require("dijit.layout.BorderContainer");
dojo.require("dojox.grid.DataGrid");
dojo.require("dojo.data.ItemFileReadStore");
dojo.require("dojo.data.ItemFileWriteStore");
dojo.require("org.potpie.musicserver.web.ServiceHandler");
dojo.require("dojox.xml.parser");
dojo.require("dijit.form.TextBox");
dojo.require("dijit.form.HorizontalSlider");
dojo.require("dijit.form.Slider");

var STATE = {"STOPPED" : 1, "PLAYING" : 2, "PAUSED" : 3};

dojo.declare("org.potpie.musicserver.web.WebController", null, {	
	
	constructor: function(artistsStore, albumsStore, songsStore, playListStore, currentState) {
		this.artistsStore = artistsStore;
		this.albumsStore = albumsStore;
		this.songsStore = songsStore;
		this.playListStore = playListStore;
		this.currentState = currentState;
		
		if (this.currentState == STATE.PLAYING) {
			dijit.byId("musicserver.playPauseButton").attr("iconClass", "pauseIcon");
		}
		
		this.artists = dijit.byId("artists");
		this.albums = dijit.byId("albums");
		this.songs = dijit.byId("songs");
		this.playList = dijit.byId("playList");
		
		var self = this;
		var artistClicked = function(e) {
			var artists = dijit.byId("artists");
			self.currentArtist = artists.getItem(e.rowIndex);
			var dfd = org.potpie.musicserver.web.ServiceHandler.getAlbumsForArtist(self.currentArtist.artist);
			dfd.addCallbacks(dojo.hitch(self, "loadAlbums"), dojo.hitch(self, "requestFailed"));
			var stackContainer = dijit.byId("stackContainer");
			var albumsContainer = dijit.byId("albumsContainer");
			stackContainer.selectChild(albumsContainer);
		}
		this.artists.connect(this.artists, "onRowClick", artistClicked);
		
		var albumClicked = function(e) {
			var albums = dijit.byId("albums");
			self.currentAlbum = albums.getItem(e.rowIndex);
			if (self.currentArtist.artist !== undefined) {
				var dfd = org.potpie.musicserver.web.ServiceHandler.getSongsForAlbum(self.currentAlbum.album, self.currentArtist.artist);
			} else {
				var dfd = org.potpie.musicserver.web.ServiceHandler.getSongsForAlbum(self.currentAlbum.album);
			}
			dfd.addCallbacks(dojo.hitch(self, "loadSongs"), dojo.hitch(self, "requestFailed"));

			var stackContainer = dijit.byId("stackContainer");
			var songsContainer = dijit.byId("songsContainer");
			stackContainer.selectChild(songsContainer);
		}
		this.albums.connect(this.albums, "onRowClick", albumClicked);
		
		dojo.connect(dijit.byId("musicserver.rewindButton"), "onClick", this, "onRewind");
		dojo.connect(dijit.byId("musicserver.playPauseButton"), "onClick", this, "onPlayPause");
		dojo.connect(dijit.byId("musicserver.stopButton"), "onClick", this, "onStop");
		dojo.connect(dijit.byId("musicserver.fastForwardButton"), "onClick", this, "onFastForward");
		
		dojo.connect(dijit.byId("randomPlaylist"), "onClick", this, "onRandomPlaylist");
		dojo.connect(dijit.byId("randomForArtist"), "onClick", this, "onRandomForArtist");
		dojo.connect(dijit.byId("resetAlbums"), "onClick", this, "onResetAlbums");
		dojo.connect(dijit.byId("clearPlaylist"), "onClick", this, "clearPlayList");
		dojo.connect(dijit.byId("addAllSongs"), "onClick", this, "addAlbumSongsToPlayList");
		dojo.connect(dijit.byId("addSelectedSongs"), "onClick", this, "addSelectedSongsToPlayList");
		dojo.connect(dijit.byId("removeSelectedSongs"), "onClick", this, "removeSelectedSongsFromPlayList");
		
		var volumeSlider = dijit.byId("volume");
		dojo.connect(volumeSlider, "onChange", function(value) {
			var volume = parseFloat(value) / 10;
			var dfd = org.potpie.musicserver.web.ServiceHandler.setVolume(volume);
			dfd.addCallbacks(dojo.hitch(self, "requestSuccessful"), dojo.hitch(self, "requestFailed"));
        });
		
		setInterval(function() {
			var dfd = org.potpie.musicserver.web.ServiceHandler.currentlyPlaying();
			dfd.addCallbacks(dojo.hitch(self, "requestSuccessful"), dojo.hitch(self, "requestFailedNoOp"));
        }, 5000);
        
	},

	loadSongs: function(response, args) {
		var songs = dijit.byId("songs");
		this.songsStore = new dojo.data.ItemFileWriteStore({data: response});
		songs.setStore(this.songsStore, null, null);
	},
	
	loadAlbums: function(response, args) {
		var albums = dijit.byId("albums");
		this.albumsStore = new dojo.data.ItemFileWriteStore({data: response});
		albums.setStore(this.albumsStore, null, null);
	},
	
	playSongs: function(response, args) {
	},
	
	requestSuccessful: function(response) {
		if (response !== null && response.currentlyPlaying !== undefined) {
			var currentlyPlaying = dojo.byId("currentlyPlaying");
			dojox.xml.parser.removeChildren(currentlyPlaying);
			currentlyPlaying.appendChild(document.createTextNode("Currently Playing ["+response.currentlyPlaying+"] ["+response.currentPosition+"]"));
		}
	},
	
	requestFailed: function(errorObj) {
		alert("Request failed ["+errorObj.status+"]["+errorObj.statusText+"]");
	},
	
	requestFailedNoOp: function(errorObj) {
	},
	
	addAlbumSongsToPlayList: function() {
		if (this.currentAlbum) {
			if (this.currentArtist !== undefined) {
				var dfd = org.potpie.musicserver.web.ServiceHandler.addAlbumToPlayList(this.currentAlbum.album, this.currentArtist.artist);
			} else { 	
				var dfd = org.potpie.musicserver.web.ServiceHandler.addAlbumToPlayList(this.currentAlbum.album);
			}
			dfd.addCallbacks(dojo.hitch(this, "albumToPlayListResult"), dojo.hitch(this, "requestFailed"));
		}
	},
	
	albumToPlayListResult: function(response) {
		var url = _contextRoot+"/service/playList";
		url += dojo.isIE ? "?preventCache"+new Date().valueOf() : "";
		this.playListStore = new dojo.data.ItemFileWriteStore({url: url});
		this.playList.setStore(this.playListStore);
		var stackContainer = dijit.byId("stackContainer");
		var playListContainer = dijit.byId("playListContainer");
		stackContainer.selectChild(playListContainer);
	},
	
	clearPlayList: function() {
		var dfd = org.potpie.musicserver.web.ServiceHandler.clearPlayList();
		dfd.addCallbacks(dojo.hitch(this, "clearPlayListResult"), dojo.hitch(this, "requestFailed"));
	},
	
	clearPlayListResult: function(response) {
		var url = _contextRoot+"/service/playList";
		url += dojo.isIE ? "?preventCache"+new Date().valueOf() : "";
		this.playListStore = new dojo.data.ItemFileWriteStore({url: url});
		this.playList.setStore(this.playListStore);
		this.requestSuccessful(response);
	},
	
	addSelectedSongsToPlayList: function() {
		var self = this;
		
		var onComplete = function(items, request){
			var playListSongs = [];
			for (var i = 0; i < items.length; i++) {
				var song = {offset : items[i].offset[0], length : items[i].length[0]};
				playListSongs.push(song);
			}
			var dfd = org.potpie.musicserver.web.ServiceHandler.addToPlayList(playListSongs);
			dfd.addCallbacks(dojo.hitch(self, "updatePlayList"), dojo.hitch(self, "requestFailed"));
			var stackContainer = dijit.byId("stackContainer");
			var playListContainer = dijit.byId("playListContainer");
			stackContainer.selectChild(playListContainer);
		};
		var onError = function(error, request){
			console.debug("error : "+ error);
		};
		this.songsStore.fetch({query:{select:true}, onComplete: onComplete, onError: onError});
	},
	
	removeSelectedSongsFromPlayList: function() {
		var self = this;
		
		var onComplete = function(items, request){
			var playListSongs = [];
			for (var i = 0; i < items.length; i++) {
				var song = {offset : items[i].offset[0], length : items[i].length[0]};
				playListSongs.push(song);
			}
			var dfd = org.potpie.musicserver.web.ServiceHandler.removeFromPlayList(playListSongs);
			dfd.addCallbacks(dojo.hitch(self, "updatePlayList"), dojo.hitch(self, "requestFailed"));
		};
		var onError = function(error, request){
			console.debug("error : "+ error);
		};
		this.playListStore.fetch({query:{select:true}, onComplete: onComplete, onError: onError});
	},
	
	updatePlayList: function() {
		this.playListStore = new dojo.data.ItemFileWriteStore({url: _contextRoot+"/service/playList"});
		this.playList.setStore(this.playListStore);
	},
	
	onRewind: function(e) {
		dojo.stopEvent(e);
		var dfd = org.potpie.musicserver.web.ServiceHandler.previous();
		dfd.addCallbacks(dojo.hitch(this, "requestSuccessful"), dojo.hitch(this, "requestFailed"));
		this.currentState = STATE.PLAYING;
		dijit.byId("musicserver.playPauseButton").attr("iconClass", "pauseIcon");
	},
	
	onPlayPause: function(e) {
		dojo.stopEvent(e);
		
		var buttonClass = "playIcon";
		switch (this.currentState) {
			case STATE.STOPPED: {
				var dfd = org.potpie.musicserver.web.ServiceHandler.play();
				dfd.addCallbacks(dojo.hitch(this, "requestSuccessful"), dojo.hitch(this, "requestFailed"));
				this.currentState = STATE.PLAYING;
				buttonClass = "pauseIcon";
				break;
			}
			case STATE.PLAYING: {
				var dfd = org.potpie.musicserver.web.ServiceHandler.pause();
				dfd.addCallbacks(dojo.hitch(this, "requestSuccessful"), dojo.hitch(this, "requestFailed"));
				this.currentState = STATE.PAUSED;
				break;
			}
			case STATE.PAUSED: {
				var dfd = org.potpie.musicserver.web.ServiceHandler.pause();
				dfd.addCallbacks(dojo.hitch(this, "requestSuccessful"), dojo.hitch(this, "requestFailed"));
				this.currentState = STATE.PLAYING;
				buttonClass = "pauseIcon";
				break;
			}
		}
		dijit.byId("musicserver.playPauseButton").attr("iconClass", buttonClass);
	},
	
	onStop: function(e) {
		dojo.stopEvent(e);
		var dfd = org.potpie.musicserver.web.ServiceHandler.stop();
		dfd.addCallbacks(dojo.hitch(this, "requestSuccessful"), dojo.hitch(this, "requestFailed"));
		this.currentState = STATE.STOPPED;
		dijit.byId("musicserver.playPauseButton").attr("iconClass", "playIcon");
	},
	
	onFastForward: function(e) {
		dojo.stopEvent(e);
		var dfd = org.potpie.musicserver.web.ServiceHandler.next();
		dfd.addCallbacks(dojo.hitch(this, "requestSuccessful"), dojo.hitch(this, "requestFailed"));
		this.currentState = STATE.PLAYING;
		dijit.byId("musicserver.playPauseButton").attr("iconClass", "pauseIcon");
	},
	
	onRandomPlaylist: function(e) {
		this.clearPlayList();
		dojo.stopEvent(e);
		var dfd = org.potpie.musicserver.web.ServiceHandler.randomPlaylist();
		dfd.addCallbacks(dojo.hitch(this, "albumToPlayListResult"), dojo.hitch(this, "requestFailed"));
	},
	
	onRandomForArtist: function(e) {
		this.clearPlayList();
		dojo.stopEvent(e);
		var dfd = org.potpie.musicserver.web.ServiceHandler.randomPlaylist(this.currentArtist.artist);
		dfd.addCallbacks(dojo.hitch(this, "albumToPlayListResult"), dojo.hitch(this, "requestFailed"));
	},
	
	onResetAlbums: function(e) {
		dojo.stopEvent(e);
		var albums = dijit.byId("albums");
		var url = _contextRoot+"/service/album";
		url += dojo.isIE ? "?preventCache"+new Date().valueOf() : "";
		this.albumsStore = new dojo.data.ItemFileReadStore({url: url});
		albums.setStore(this.albumsStore, null, null);
	}
});
})();