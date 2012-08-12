define(['dijit/registry',
        'dojox/mobile/parser',
        './servicehandler',
        'dojox/mobile/ListItem',
        'dojo/_base/connect',
        'dojo/dom-class',
        'dojo/dom',
        'dojox/mobile/TabBarButton',
        'dojox/mobile/ScrollableView',
        'dojox/mobile/RoundRectList',
        'dojox/mobile', 
        'dojox/mobile/Button', 
        'dojox/mobile/Heading',
        'dojox/mobile/IconContainer',
        'dojox/mobile/ToolBarButton',
        'dojox/mobile/TabBar',
        'dojox/mobile/RoundRect',
        'dojox/mobile/Slider',
        'dojox/mobile/compat',
        'dojo/domReady!'
], function (registry, parser, servicehandler, ListItem, connector, domClass, dom, TabBarButton, ScrollableView, RoundRectList) {
	parser.parse();

	var STATE = {"STOPPED" : 0, "PLAYING" : 1, "PAUSED" : 2, "UNINITIALIZED" : 3};
	var currentArtist;
	var currentVolume;
	var currentState;
	var initialized = false;
	
	var volumeSlider = registry.byId("volume");

	var requestSuccessful = function(response) {
		if (response !== null && response.currentState !== undefined) {
			currentState = response.currentState;
			if (currentState !== STATE.UNINITIALIZED) {
				currentVolume = response.currentVolume;
				volumeSlider.set('value', currentVolume); 
				var currentlyPlaying = dom.byId("currentlyPlaying");
				while (currentlyPlaying.hasChildNodes()) {
					currentlyPlaying.removeChild(currentlyPlaying.firstChild);
				}
				currentlyPlaying.appendChild(document.createTextNode("["+response.currentlyPlaying+"]"));
				currentlyPlaying.appendChild(document.createElement("br"));
				currentlyPlaying.appendChild(document.createTextNode("["+response.currentPosition+"]"));
				setPlayPauseIcon();
				if (!initialized) {
					initialized = true;
					var dfd = servicehandler.getArtistsTabDetails();
					dfd.addCallbacks(function(tabs) {
						var artistsContainer = registry.byId("artists");
						var artistTabContainer = registry.byId("artistTabContainer");
						var tabButton, view, list;
						
						for (var i = 0; i < tabs.length; i++) {
							list = registry.byId("artistList"+(i+1));
							for (var j = 0; j < tabs[i].artistList.length; j++) {
								var artistItem = new ListItem({
									label: tabs[i].artistList[j].label,
									moveTo: "albums",
									variableHeight: true
								});
								list.addChild(artistItem);
								artistItem.connect(artistItem, "onClick", artistClicked);
							}
							tabButton = new TabBarButton({
								label: tabs[i].label, 
								moveTo: "artistView"+(i+1), 
								selected: i < 1 ? true : false, 
								style: "width:40px" 
							});
							tabButton.domNode.setAttribute("listId", "artistList"+(i+1));
							tabButton.domNode.setAttribute("startIndex", ""+tabs[i].startIndex);
							tabButton.domNode.setAttribute("endIndex", ""+tabs[i].endIndex);
							tabButton.connect(tabButton, "onClick", getArtistsForTab);
							artistTabContainer.addChild(tabButton);
						}
					}, requestFailed);
				}
			}
		}
	};
	
	var requestFailed = function(errorObj) {
		alert("Request failed ["+errorObj.status+"]["+errorObj.statusText+"]");
	};

	var requestFailedNoOp = function(errorObj) {};
	
	var addAllSongs = function(e) {
		dojo.stopEvent(e);
		var songList = registry.byId("songList");
		var albumName = songList.domNode.getAttribute("currentAlbum");
		if (currentArtist !== undefined) {
			var dfd = servicehandler.addAlbumToPlayList(albumName, currentArtist);
		} else {
			var dfd = servicehandler.addAlbumToPlayList(albumName);
		}
		dfd.addCallbacks(getPlayList, requestFailed);
		var view = registry.byId("songs");
		view.performTransition("playing", -1);
	};
	
	var randomForArtist = function(e) {
		clearPlayList();
		dojo.stopEvent(e);
		var dfd = servicehandler.randomPlaylist(currentArtist);
		dfd.addCallbacks(getPlayList, requestFailed);
		var view = registry.byId("albums");
		view.performTransition("playing", -1);
	};
	
	var randomPlaylist = function(e) {
		clearPlayList();
		dojo.stopEvent(e);
		var dfd = servicehandler.randomPlaylist();
		dfd.addCallbacks(getPlayList, requestFailed);
	};
	
	var loadPlayList = function(response, args) {
		var playList = registry.byId("playList");
		playList.destroyDescendants(false);
		for (var i = 0; i < response.items.length; i++) {
			var playListItem = new dojox.mobile.ListItem({
				label: response.items[i].artist + " : " + response.items[i].title
			});
			domClass.add(playListItem.domNode, "mblVariableHeight");
			playList.addChild(playListItem);
		}
	};
	
	var clearPlayList = function() {
		var dfd = servicehandler.clearPlayList();
		dfd.addCallbacks(requestSuccessful, requestFailed);
		var playList = registry.byId("playList");
		playList.destroyDescendants(false);
	};
	
	var getPlayList = function() {
		var dfd = servicehandler.getPlaylist();
		dfd.addCallbacks(loadPlayList, requestFailed);
	};
	
	var volumeUp = function(e) {
		if (currentVolume < 10) {
			currentVolume++;
			var dfd = servicehandler.setVolume(currentVolume/10);
			dfd.addCallbacks(requestSuccessful, requestFailed);
		}
	};
	
	var volumeDown = function(e) {
		if (currentVolume > 0) {
			currentVolume--;
			var dfd = servicehandler.setVolume(currentVolume/10);
			dfd.addCallbacks(requestSuccessful, requestFailed);
		}
	};
	
	var previous = function(e) {
		dojo.stopEvent(e);
		var dfd = servicehandler.previous();
		dfd.addCallbacks(requestSuccessful, requestFailed);
	};
	
	var playPause = function(e) {
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
		registry.byId("playPause").domNode.className = "mblButton " + buttonClass;
	};
	
	var stop = function(e) {
		dojo.stopEvent(e);
		var dfd = servicehandler.stop();
		dfd.addCallbacks(requestSuccessful, requestFailed);
	};
	
	var next = function(e) {
		dojo.stopEvent(e);
		var dfd = servicehandler.next();
		dfd.addCallbacks(requestSuccessful, requestFailed);
	};
	
	var songClicked = function(e) {
		var songItemNode = e.target.parentNode;
		var songOffset = songItemNode.attributes.getNamedItem("songOffset");
		var songLength = songItemNode.attributes.getNamedItem("songLength");
		var dfd = servicehandler.addToPlayList([{offset : parseInt(songOffset.value), length : parseInt(songLength.value)}]);
		dfd.addCallbacks(getPlayList, requestFailed);
	};
	
	var albumClicked = function(e) {
		var dfd = servicehandler.getSongsForAlbum(e.target.textContent, currentArtist);
		dfd.addCallbacks(
		function(response) {
			var songList = registry.byId("songList");
			songList.domNode.setAttribute("currentAlbum", response.items[0].album + " ("+response.items[0].type+")");
			songList.destroyDescendants(false);
			for (var i = 0; i < response.items.length; i++) {
				var songItem = new ListItem({
					label: response.items[i].title,
					moveTo: "playing",
					variableHeight: true
				});
				songItem.domNode.setAttribute("songOffset", ""+response.items[i].offset);
				songItem.domNode.setAttribute("songLength", ""+response.items[i].length);
				songItem.connect(songItem, "onClick", songClicked);
				songList.addChild(songItem);
			}
		}, requestFailed);
	};
	
	var artistClicked = function(e) {
		currentArtist = e.target.textContent;
		var dfd = servicehandler.getAlbumsForArtist(currentArtist);
		dfd.addCallbacks(function(response) {
			var albumList = registry.byId("albumList");
			albumList.destroyDescendants(false);
			for (var i = 0; i < response.items.length; i++) {
				var albumItem = new ListItem({
					label: response.items[i].album,
					moveTo: "songs",
					variableHeight: true
				});
				albumItem.connect(albumItem, "onClick", albumClicked);
				albumList.addChild(albumItem);
			}
		}, requestFailed);
	};
	
	var getArtistsForTab = function(e) {
		var tab = e.target.offsetParent;
		var listId = tab.attributes.getNamedItem("listId").value;
		var startIndex = parseInt(tab.attributes.getNamedItem("startIndex").value);
		var	endIndex = parseInt(tab.attributes.getNamedItem("endIndex").value);
		var dfd = servicehandler.getArtistsByIndexRange(listId, startIndex, endIndex);
		dfd.addCallbacks(function(response) {
			var list = registry.byId(response.listId);
			list.destroyDescendants(false);
			for (var i = 0; i < response.items.length; i++) {
				var artistItem = new ListItem({
					label: response.items[i].artist,
					moveTo: "albums",
					variableHeight: true
				});
				list.addChild(artistItem);
				artistItem.connect(artistItem, "onClick", artistClicked);
			}
		}, requestFailed);
	};
	
	connector.connect(registry.byId("toPlayList1"), "onClick", getPlayList);
	connector.connect(registry.byId("toPlayList2"), "onClick", getPlayList);
	connector.connect(registry.byId("toPlayList3"), "onClick", getPlayList);
	connector.connect(registry.byId("addAllButton"), "onClick", addAllSongs);
	connector.connect(registry.byId("randomButton"), "onClick", randomPlaylist);
	connector.connect(registry.byId("randomForArtistButton"), "onClick", randomForArtist);
	connector.connect(registry.byId("clearButton"), "onClick", clearPlayList);
	connector.connect(registry.byId("previous"), "onClick", previous);
	connector.connect(registry.byId("playPause"), "onClick", playPause);
	connector.connect(registry.byId("stop"), "onClick", stop);
	connector.connect(registry.byId("next"), "onClick", next);
	connector.connect(registry.byId("up"), "onClick", volumeUp);
	connector.connect(registry.byId("down"), "onClick", volumeDown);
	
	connector.connect(volumeSlider, "onChange", function(value) {
		var volume = parseFloat(value) / 10;
		var dfd = servicehandler.setVolume(volume);
		dfd.addCallbacks(requestSuccessful, requestFailed);
    });
	
	function monitor() {
		var dfd = servicehandler.currentlyPlaying();
		dfd.addCallbacks(requestSuccessful, requestFailed);
		setTimeout(function(){ monitor(); }, 5000);
    };
	monitor();
	
	return {};
});