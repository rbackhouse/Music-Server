define(['dijit/registry',
        'dojox/mobile/parser',
        './servicehandler',
        'dojox/mobile/ListItem',
        'dojo/_base/connect',
        'dojo/dom-class',
        'dojo/dom',
        'dojo/dom-construct',
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
], function (registry, parser, servicehandler, ListItem, connector, domClass, dom, construct, TabBarButton, ScrollableView, RoundRectList) {
	parser.parse();

	var STATE = {"STOPPED" : 0, "PLAYING" : 1, "PAUSED" : 2, "UNINITIALIZED" : 3};
	var currentArtist;
	var currentState = STATE.STOPPED;
	var currentTrack;
	var currentPlayIndex = 1;
	var initialized = false;
	
	var requestSuccessful = function(response) {
		if (response !== null && response.currentState !== undefined) {
			if (response.currentState !== STATE.UNINITIALIZED) {
				currentTrack = response.currentlyPlaying;
				currentPlayIndex = response.currentPlayIndex;
				var currentlyPlaying = dom.byId("currentlyPlaying");
				while (currentlyPlaying.hasChildNodes()) {
					currentlyPlaying.removeChild(currentlyPlaying.firstChild);
				}
				currentlyPlaying.appendChild(document.createTextNode("["+response.currentlyPlaying+"]"));
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
			var dfd = servicehandler.addAlbumToStreamPlayList(albumName, currentArtist);
		} else {
			var dfd = servicehandler.addAlbumToStreamPlayList(albumName);
		}
		dfd.addCallbacks(getPlayList, requestFailed);
		var view = registry.byId("songs");
		view.performTransition("playing", -1);
	};
	
	var randomForArtist = function(e) {
		clearPlayList();
		dojo.stopEvent(e);
		var dfd = servicehandler.randomStreamPlaylist(currentArtist);
		dfd.addCallbacks(getPlayList, requestFailed);
		var view = registry.byId("albums");
		view.performTransition("playing", -1);
	};
	
	var randomPlaylist = function(e) {
		clearPlayList();
		dojo.stopEvent(e);
		var dfd = servicehandler.randomStreamPlaylist();
		dfd.addCallbacks(getPlayList, requestFailed);
		registry.byId("playPause").domNode.className = "mblButton playIcon";
	};
	
	var loadPlayList = function(response, args) {
		var playList = registry.byId("playList");
		playList.destroyDescendants(false);
		for (var i = 0; i < response.items.length; i++) {
			var playListItem = new dojox.mobile.ListItem({
				label: response.items[i].artist + " : " + response.items[i].title
			});
			playListItem.domNode.setAttribute("songOffset", ""+response.items[i].offset);
			playListItem.domNode.setAttribute("songLength", ""+response.items[i].length);
			domClass.add(playListItem.domNode, "mblVariableHeight");
			playList.addChild(playListItem);
		}
	};
	
	var clearPlayList = function() {
		currentPlayIndex = 1;
		var dfd = servicehandler.clearStreamPlayList();
		dfd.addCallbacks(requestSuccessful, requestFailed);
		var playList = registry.byId("playList");
		playList.destroyDescendants(false);
		registry.byId("playPause").domNode.className = "mblButton playIcon";
	};
	
	var getPlayList = function() {
		var dfd = servicehandler.getStreamPlaylist();
		dfd.addCallbacks(loadPlayList, requestFailed);
	};
	
	var previous = function(e) {
		dojo.stopEvent(e);
		if (currentPlayIndex > 1) {
			var dfd = servicehandler.streamPrevious();
			dfd.addCallbacks(requestSuccessful, requestFailed);
			startStreaming(--currentPlayIndex);
			currentState = STATE.PLAYING;
		}
		registry.byId("playPause").domNode.className = "mblButton pauseIcon";
	};
	
	var playPause = function(e) {
		if (e) {
			dojo.stopEvent(e);
		}
		var buttonClass = "playIcon";
		var streamer = dom.byId("streamer");
		switch (currentState) {
			case STATE.STOPPED: {
				var dfd = servicehandler.streamPlay();
				dfd.addCallbacks(requestSuccessful, requestFailed);
				startStreaming(currentPlayIndex);
				currentState = STATE.PLAYING;
				buttonClass = "pauseIcon";
				break;
			}
			case STATE.PLAYING: {
				if (streamer !== null && !streamer.paused) {
					streamer.pause();
				}
				currentState = STATE.PAUSED;
				break;
			}
			case STATE.PAUSED: {
				if (streamer !== null && streamer.paused) {
					streamer.play();
				}
				currentState = STATE.PLAYING;
				buttonClass = "pauseIcon";
				break;
			}
		}
		registry.byId("playPause").domNode.className = "mblButton " + buttonClass;
	};
	
	var stop = function(e) {
		dojo.stopEvent(e);
		var streamer = dom.byId("streamer");
		if (streamer !== null) {
			streamer.pause();
			construct.destroy(streamer);
		}
		currentState = STATE.STOPPED;
		registry.byId("playPause").domNode.className = "mblButton playIcon";
	};
	
	var next = function(e) {
		dojo.stopEvent(e);
		var playList = dijit.byId("playList");
		if (currentPlayIndex < (playList.containerNode.childNodes.length -1)) {
			var dfd = servicehandler.streamNext();
			dfd.addCallbacks(requestSuccessful, requestFailed);
			startStreaming(++currentPlayIndex);
			currentState = STATE.PLAYING;
		}
		registry.byId("playPause").domNode.className = "mblButton pauseIcon";
	};
	
	var startStreaming = function(index) {
		var playList = dijit.byId("playList");
		var playListItemNode = playList.containerNode.childNodes[index];
		var songOffset = playListItemNode.attributes.getNamedItem("songOffset");
		var songLength = playListItemNode.attributes.getNamedItem("songLength");
		
		var srcUrl = "./service/stream?length="+songLength.value+"&offset="+songOffset.value;
		var streamerContainer = dom.byId("streamerContainer");
		var streamer = dom.byId("streamer");
		if (streamer !== null) {
			construct.destroy(streamer);	
		}
		
		streamer = construct.create("audio", {id: "streamer", src: srcUrl, autoplay: "true"}, streamerContainer);

		var streamEnded = function(e) {
			console.log("ended");
			var playList = dijit.byId("playList");
			if (currentPlayIndex < (playList.containerNode.childNodes.length -1)) {
				var dfd = servicehandler.streamNext();
				dfd.addCallbacks(requestSuccessful, requestFailed);
				++currentPlayIndex;
				currentState = STATE.STOPPED;
				playPause();
			}
		}
		streamer.addEventListener('ended', streamEnded, false);

		var update = function(e) {
			var currentlyPlaying = dom.byId("currentlyPlaying");
			while (currentlyPlaying.hasChildNodes()) {
				currentlyPlaying.removeChild(currentlyPlaying.firstChild);
			}
			var msg = "["+currentTrack+"]";
			currentlyPlaying.appendChild(document.createTextNode(msg));
			var streamer = dom.byId("streamer");
			if (streamer !== null) {
				var seconds = streamer.currentTime.toFixed(0);
				var minutes = 0;
				if (seconds > 59) {
					minutes = seconds / 60;
					seconds = seconds % 60;
					msg = "["+minutes.toFixed(0)+" mins, "+seconds+" secs]";
				} else {
					msg = "["+seconds+" secs]";
				}
				currentlyPlaying.appendChild(document.createElement("br"));
				currentlyPlaying.appendChild(document.createTextNode(msg));
			}
		}
		streamer.addEventListener('timeupdate', update, false);

		streamer.addEventListener('canplaythrough', function() {
			streamer.play();
		}, false);

		/*
		var eventListener = function(e) {
			console.log("media event: "+e.type);
		}
		streamer.addEventListener('canplay', eventListener, false);
		streamer.addEventListener('durationchange', eventListener, false);
		streamer.addEventListener('emptied', eventListener, false);
		streamer.addEventListener('error', eventListener, false);
		streamer.addEventListener('loadeddata', eventListener, false);
		streamer.addEventListener('loadedmetadata', eventListener, false);
		streamer.addEventListener('loadstart', eventListener, false);
		streamer.addEventListener('pause', eventListener, false);
		streamer.addEventListener('playing', eventListener, false);
		streamer.addEventListener('progress', eventListener, false);
		streamer.addEventListener('ratechange', eventListener, false);
		streamer.addEventListener('readystatechange', eventListener, false);
		streamer.addEventListener('seeked', eventListener, false);
		streamer.addEventListener('seeking', eventListener, false);
		streamer.addEventListener('stalled', eventListener, false);
		streamer.addEventListener('suspend', eventListener, false);
		streamer.addEventListener('volumechange', eventListener, false);
		streamer.addEventListener('waiting', eventListener, false);
		*/
		streamer.load();
	};
	
	var songClicked = function(e) {
		var songItemNode = e.currentTarget.parentNode;
		var songOffset = songItemNode.attributes.getNamedItem("songOffset");
		var songLength = songItemNode.attributes.getNamedItem("songLength");
		var dfd = servicehandler.addToStreamPlayList([{offset : parseInt(songOffset.value), length : parseInt(songLength.value)}]);
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
		var tab = e.currentTarget.offsetParent;
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
	
	var dfd = servicehandler.currentlyStreaming();
	dfd.addCallbacks(requestSuccessful, requestFailed);
	
	return {};
});