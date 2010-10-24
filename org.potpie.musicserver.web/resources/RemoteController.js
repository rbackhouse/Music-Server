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
dojo.provide("org.potpie.musicserver.web.RemoteController");

dojo.require("dojox.mobile.parser");
dojo.require("dojox.mobile");
dojo.requireIf(!dojo.isWebKit, "dojox.mobile.compat");

dojo.require("org.potpie.musicserver.web.ServiceHandler");

var STATE = {"STOPPED" : 1, "PLAYING" : 2, "PAUSED" : 3};

dojo.declare("org.potpie.musicserver.web.RemoteController", null, {	
	
	constructor: function(currentState) {
		this.currentVolume = _currentVolume;
		this.currentState = currentState;
		if (this.currentState == STATE.PLAYING) {
			dijit.byId("playPause").domNode.className = "mblButton pauseIcon";
		}
		
		var self = this;
		var artistClicked = function(e) {
			self.currentArtist = e.target.textContent;
			var dfd = org.potpie.musicserver.web.ServiceHandler.getAlbumsForArtist(e.target.textContent);
			dfd.addCallbacks(dojo.hitch(self, "loadAlbums"), dojo.hitch(self, "requestFailed"));
		}
		dojo.query("li[id*=\"artistItem\"]").forEach(function(node){
			var w = dijit.byNode(node); 
			if (w.id.indexOf("artistItem") >= 0) {
				w.connect(w, "onClick", artistClicked);
			}
		});
		
		dojo.connect(dijit.byId("artistTabContainer"), "onTabClick", function(e) {
			var tab = e.currentTarget;
			var list = tab.pane.containerNode.childNodes[1];
			//console.debug(list.id+":"+list.childElementCount+":"+list.attributes.getNamedItem("startIndex").value+":"+list.attributes.getNamedItem("endIndex").value);
			if (list.childElementCount == 0) {
				var dfd = org.potpie.musicserver.web.ServiceHandler.getArtistsByIndexRange(list.id, 
						list.attributes.getNamedItem("startIndex").value,
						list.attributes.getNamedItem("endIndex").value);
				dfd.addCallbacks(dojo.hitch(self, "loadArtists"), dojo.hitch(self, "requestFailed"));
			}
		});
		
		var dfd1 = org.potpie.musicserver.web.ServiceHandler.getPlaylist();
		dfd1.addCallbacks(dojo.hitch(this, "loadPlayList"), dojo.hitch(this, "requestFailed"));
		
		var dfd2 = org.potpie.musicserver.web.ServiceHandler.currentlyPlaying();
		dfd2.addCallbacks(dojo.hitch(this, "requestSuccessful"), dojo.hitch(this, "requestFailed"));
		
		dojo.connect(dijit.byId("randomButton"), "onClick", this, "randomPlaylist");
		dojo.connect(dijit.byId("clearButton"), "onClick", this, "clearPlayList");
		dojo.connect(dijit.byId("randomForArtistButton"), "onClick", this, "randomForArtist");
		dojo.connect(dijit.byId("addAllButton"), "onClick", this, "addAllSongs");
		dojo.connect(dijit.byId("playPause"), "onClick", this, "onPlayPause");
		dojo.connect(dijit.byId("stop"), "onClick", this, "onStop");
		dojo.connect(dijit.byId("next"), "onClick", this, "onFastForward");
		dojo.connect(dijit.byId("previous"), "onClick", this, "onRewind");
		dojo.connect(dijit.byId("gotoAlbums"), "onClick", this, "gotoAlbums");
		dojo.connect(dijit.byId("gotoSongs"), "onClick", this, "gotoSongs");
		dojo.connect(dijit.byId("gotoPlayList"), "onClick", this, "gotoPlayList");
		dojo.connect(dijit.byId("gotoPlayList1"), "onClick", this, "gotoPlayList1");
		dojo.connect(dijit.byId("gotoPlayList2"), "onClick", this, "gotoPlayList2");
		dojo.connect(dijit.byId("plus"), "onClick", this, "volumeUp");
		dojo.connect(dijit.byId("minus"), "onClick", this, "volumeDown");
		setInterval(function() {
			var dfd = org.potpie.musicserver.web.ServiceHandler.currentlyPlaying();
			dfd.addCallbacks(dojo.hitch(self, "requestSuccessful"), dojo.hitch(self, "requestFailed"));
        }, 5000);
	},
	
	volumeUp: function(e) {
		if (this.currentVolume < 10) {
			this.currentVolume++;
			var dfd = org.potpie.musicserver.web.ServiceHandler.setVolume(this.currentVolume/10);
			dfd.addCallbacks(dojo.hitch(this, "requestSuccessful"), dojo.hitch(this, "requestFailed"));
			var volumeLabel = dojo.byId("volume");
			while (volumeLabel.hasChildNodes()) {
				volumeLabel.removeChild(volumeLabel.firstChild);
			}
			volumeLabel.appendChild(document.createTextNode("Volume ["+this.currentVolume+"]"));
		}
	},
	
	volumeDown: function(e) {
		if (this.currentVolume > 0) {
			this.currentVolume--;
			var dfd = org.potpie.musicserver.web.ServiceHandler.setVolume(this.currentVolume/10);
			dfd.addCallbacks(dojo.hitch(this, "requestSuccessful"), dojo.hitch(this, "requestFailed"));
			var volumeLabel = dojo.byId("volume");
			while (volumeLabel.hasChildNodes()) {
				volumeLabel.removeChild(volumeLabel.firstChild);
			}
			volumeLabel.appendChild(document.createTextNode("Volume ["+this.currentVolume+"]"));
		}
	},
	
	gotoAlbums: function(e) {
		dojo.stopEvent(e);
		var view = dijit.byId("artists");
		view.performTransition("albums", -1);
	},
	
	gotoSongs: function(e) {
		dojo.stopEvent(e);
		var view = dijit.byId("artists");
		view.performTransition("songs", -1);
	},
	
	gotoPlayList: function(e) {
		dojo.stopEvent(e);
		var view = dijit.byId("artists");
		view.performTransition("playing", -1);
	},
	
	gotoPlayList1: function(e) {
		dojo.stopEvent(e);
		var view = dijit.byId("albums");
		view.performTransition("playing", -1);
	},
	
	gotoPlayList2: function(e) {
		dojo.stopEvent(e);
		var view = dijit.byId("songs");
		view.performTransition("playing", -1);
	},
	
	addAllSongs: function(e) {
		dojo.stopEvent(e);
		var songList = dijit.byId("songList");
		var albumName = songList.domNode.getAttribute("currentAlbum");
		if (self.currentArtist !== undefined) {
			var dfd = org.potpie.musicserver.web.ServiceHandler.addAlbumToPlayList(albumName, self.currentArtist);
		} else {
			var dfd = org.potpie.musicserver.web.ServiceHandler.addAlbumToPlayList(albumName);
		}
		dfd.addCallbacks(dojo.hitch(this, "getPlayList"), dojo.hitch(this, "requestFailed"));
		var view = dijit.byId("songs");
		view.performTransition("playing", -1);
	},
	
	randomForArtist: function(e) {
		this.clearPlayList();
		dojo.stopEvent(e);
		var dfd = org.potpie.musicserver.web.ServiceHandler.randomPlaylist(this.currentArtist);
		dfd.addCallbacks(dojo.hitch(this, "getPlayList"), dojo.hitch(this, "requestFailed"));
		var view = dijit.byId("albums");
		view.performTransition("playing", -1);
	},
	
	randomPlaylist: function(e) {
		this.clearPlayList();
		dojo.stopEvent(e);
		var dfd = org.potpie.musicserver.web.ServiceHandler.randomPlaylist();
		dfd.addCallbacks(dojo.hitch(this, "getPlayList"), dojo.hitch(this, "requestFailed"));
	},
	
	getPlayList: function(response, args) {
		var dfd = org.potpie.musicserver.web.ServiceHandler.getPlaylist();
		dfd.addCallbacks(dojo.hitch(this, "loadPlayList"), dojo.hitch(this, "requestFailed"));
	},
	
	loadPlayList: function(response, args) {
		var playList = dijit.byId("playList");
		playList.destroyDescendants(false);
		for (var i = 0; i < response.items.length; i++) {
			var playListItem = new dojox.mobile.ListItem({
				label: response.items[i].artist + " : " + response.items[i].title
			});
			dojo.addClass(playListItem.domNode, "mblVariableHeight");
			playList.addChild(playListItem);
		}
	},
	
	clearPlayList: function() {
		var dfd = org.potpie.musicserver.web.ServiceHandler.clearPlayList();
		dfd.addCallbacks(dojo.hitch(this, "requestSuccessful"), dojo.hitch(this, "requestFailed"));
		var playList = dijit.byId("playList");
		playList.destroyDescendants(false);
	},
	
	loadArtists: function(response, args) {
		var self = this;
		var artistClicked = function(e) {
			self.currentArtist = e.target.textContent;
			var dfd = org.potpie.musicserver.web.ServiceHandler.getAlbumsForArtist(e.target.textContent);
			dfd.addCallbacks(dojo.hitch(self, "loadAlbums"), dojo.hitch(self, "requestFailed"));
		}
		var artistList = dijit.byId(response.listId);
		artistList.destroyDescendants(false);
		for (var i = 0; i < response.items.length; i++) {
			var artistItem = new dojox.mobile.ListItem({
				label: response.items[i].artist,
				moveTo: "albums"
			});
			dojo.addClass(artistItem.domNode, "mblVariableHeight");
			artistItem.connect(artistItem, "onClick", artistClicked);
			artistList.addChild(artistItem);
		}
	},
	
	loadAlbums: function(response, args) {
		var self = this;
		var albumClicked = function(e) {
			if (self.currentArtist !== undefined) {
				var dfd = org.potpie.musicserver.web.ServiceHandler.getSongsForAlbum(e.target.textContent, self.currentArtist);
			} else {
				var dfd = org.potpie.musicserver.web.ServiceHandler.getSongsForAlbum(e.target.textContent);
			}
			dfd.addCallbacks(dojo.hitch(self, "loadSongs"), dojo.hitch(self, "requestFailed"));
		}
		var albumList = dijit.byId("albumList");
		albumList.destroyDescendants(false);
		for (var i = 0; i < response.items.length; i++) {
			var albumItem = new dojox.mobile.ListItem({
				label: response.items[i].album,
				moveTo: "songs"
			});
			dojo.addClass(albumItem.domNode, "mblVariableHeight");
			albumItem.connect(albumItem, "onClick", albumClicked);
			albumList.addChild(albumItem);
		}
	},
	
	loadSongs: function(response, args) {
		var self = this;
		var songClicked = function(e) {
			var songItemNode = e.currentTarget.parentNode;
			var songOffset = songItemNode.attributes.getNamedItem("songOffset");
			var songLength = songItemNode.attributes.getNamedItem("songLength");
			var dfd = org.potpie.musicserver.web.ServiceHandler.addToPlayList([{offset : parseInt(songOffset.value), length : parseInt(songLength.value)}]);
			dfd.addCallbacks(dojo.hitch(self, "getPlayList"), dojo.hitch(self, "requestFailed"));
		}
		var songList = dijit.byId("songList");
		songList.domNode.setAttribute("currentAlbum", response.items[0].album + " ("+response.items[0].type+")");

		songList.destroyDescendants(false);
		for (var i = 0; i < response.items.length; i++) {
			var songItem = new dojox.mobile.ListItem({
				label: response.items[i].title,
				moveTo: "playing"
			});
			songItem.domNode.setAttribute("songOffset", ""+response.items[i].offset);
			songItem.domNode.setAttribute("songLength", ""+response.items[i].length);
			dojo.addClass(songItem.domNode, "mblVariableHeight");
			songItem.connect(songItem, "onClick", songClicked);
			songList.addChild(songItem);
		}
	},
	
	onRewind: function(e) {
		dojo.stopEvent(e);
		var dfd = org.potpie.musicserver.web.ServiceHandler.previous();
		dfd.addCallbacks(dojo.hitch(this, "requestSuccessful"), dojo.hitch(this, "requestFailed"));
		this.currentState = STATE.PLAYING;
		dijit.byId("playPause").domNode.className = "mblButton pauseIcon";
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
		dijit.byId("playPause").domNode.className = "mblButton " + buttonClass;
	},
	
	onStop: function(e) {
		dojo.stopEvent(e);
		var dfd = org.potpie.musicserver.web.ServiceHandler.stop();
		dfd.addCallbacks(dojo.hitch(this, "requestSuccessful"), dojo.hitch(this, "requestFailed"));
		this.currentState = STATE.STOPPED;
		dijit.byId("playPause").domNode.className = "mblButton playIcon";
	},
	
	onFastForward: function(e) {
		dojo.stopEvent(e);
		var dfd = org.potpie.musicserver.web.ServiceHandler.next();
		dfd.addCallbacks(dojo.hitch(this, "requestSuccessful"), dojo.hitch(this, "requestFailed"));
		this.currentState = STATE.PLAYING;
		dijit.byId("playPause").domNode.className = "mblButton pauseIcon";
	},
	
	requestSuccessful: function(response) {
		if (response.currentlyPlaying != undefined) {
			var currentlyPlaying = dojo.byId("currentlyPlaying");
			while (currentlyPlaying.hasChildNodes()) {
				currentlyPlaying.removeChild(currentlyPlaying.firstChild);
			}
			currentlyPlaying.appendChild(document.createTextNode("["+response.currentlyPlaying+"]"));
			currentlyPlaying.appendChild(document.createElement("br"));
			currentlyPlaying.appendChild(document.createTextNode("["+response.currentPosition+"]"));
		}
	},
	
	requestFailed: function(errorObj) {
		alert("Request failed ["+errorObj.status+"]["+errorObj.statusText+"]");
	},

	requestFailedNoOp: function(errorObj) {
	}
});
})();