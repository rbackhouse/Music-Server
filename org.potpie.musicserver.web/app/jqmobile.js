define(['jquery',
        './jqservicehandler'
], function ($, servicehandler) {

	var STATE = {"STOPPED" : 0, "PLAYING" : 1, "PAUSED" : 2, "UNINITIALIZED" : 3};
	var currentArtist;
	var currentAlbum;
	var currentState;
	var initialized = false;
	
	var requestSuccessful = function(response) {
		if (response !== undefined && response.currentState !== undefined) {
			currentState = response.currentState;
			if (currentState !== STATE.UNINITIALIZED) {
				//$("#volume").val(response.currentVolume);
				setPlayPauseIcon();
				if (!initialized) {
					initialized = true;
					var dfd = servicehandler.getArtists();
					dfd.addCallbacks(function(artists) {
						for (var i = 0; i < artists.items.length; i++) {
							$("#artistList").append("<li><a href='#albums'>"+artists.items[i].artist+"</a></li>");
						}
						$("#artistList").listview("refresh");
						$('#artistList').delegate('li', 'click', function () {
							artistClicked(this.textContent);
						});
						
					}, requestFailed);
					getPlayList();
				}
			}
		}
	};
	
	var artistClicked = function(artistName) {
		currentArtist = artistName;
		var dfd = servicehandler.getAlbumsForArtist(artistName);
		dfd.addCallbacks(function(albums) {
			$("#albumList li").remove();
			for (var i = 0; i < albums.items.length; i++) {
				$("#albumList").append("<li><a href='#songs'>"+albums.items[i].album+"</a></li>");
			}
			$("#albumList").listview("refresh");
			$('#albumList').delegate('li', 'click', function () {
				albumClicked(this.textContent);
			});
		}, requestFailed);
	};
	
	var albumClicked = function(albumName) {
		currentAlbum = albumName;
		var dfd = servicehandler.getSongsForAlbum(albumName, currentArtist);
		dfd.addCallbacks(function(songs) {
			$("#songList li").remove();
			for (var i = 0; i < songs.items.length; i++) {
				$("#songList").append("<li data-songOffset='"+songs.items[i].offset+"' data-songLength='"+songs.items[i].length+"'><a href='#playlist'>"+songs.items[i].title+"</a></li>");
			}
			$("#songList").listview("refresh");
			$('#songList').delegate('li', 'click', function () {
				var songOffset = this.getAttribute("data-songOffset");
				var songLength = this.getAttribute("data-songLength");
				var dfd = servicehandler.addToPlayList([{offset : parseInt(songOffset), length : parseInt(songLength)}]);
				dfd.addCallbacks(getPlayList, requestFailed);
			});
		}, requestFailed);

	};
	
	var getPlayList = function() {
		var dfd = servicehandler.getPlaylist();
		dfd.addCallbacks(loadPlayList, requestFailed);
	};
	
	var loadPlayList = function(playlist, args) {
		console.log("loadPlayList");
		$("#playingList li").remove();
		for (var i = 0; i < playlist.items.length; i++) {
			//$("#playingList").append("<li><a href='#playlist'>"+playlist.items[i].artist + " : " + playlist.items[i].title+"</a></li>");
			$("#playingList").append("<li>"+playlist.items[i].artist + " : " + playlist.items[i].title+"</li>");
		}
		try {
			$("#playingList").listview("refresh");
		} catch (e) {
			$("#playingList ol").listview("refresh");
		}
	};
	
	var clearPlayList = function() {
		var dfd = servicehandler.clearPlayList();
		dfd.addCallbacks(requestSuccessful, requestFailed);
		$("#playingList li").remove();
	};
	
	var setPlayPauseIcon = function() {
		switch (currentState) {
			case STATE.STOPPED:
			case STATE.PAUSED: {
				$("#playPause").removeClass("pauseIcon");
				$("#playPause").addClass("playIcon");
				break;
			}
			case STATE.PLAYING: {
				$("#playPause").removeClass("playIcon");
				$("#playPause").addClass("pauseIcon");
				break;
			}
		}
	};
	
	var requestFailed = function(errorObj) {
		alert("Request failed ["+errorObj.status+"]["+errorObj.statusText+"]");
	};

	var requestFailedNoOp = function(errorObj) {};

	function monitor() {
		var dfd = servicehandler.currentlyPlaying();
		dfd.addCallbacks(requestSuccessful, requestFailed);
		setTimeout(function(){ monitor(); }, 5000);
    };
    $(document).ready(function() {
    	console.log("ready");
		monitor();
		$("#randomButton").click(function() {
			clearPlayList();
			var dfd = servicehandler.randomPlaylist();
			dfd.addCallbacks(getPlayList, requestFailed);
		});
		$("#clearButton").click(function() {
	        clearPlayList();
		});
		$("#addAllButton").click(function() {
	        if (currentAlbum !== undefined) {
				if (currentArtist !== undefined) {
					var dfd = servicehandler.addAlbumToPlayList(currentAlbum, currentArtist);
				} else {
					var dfd = servicehandler.addAlbumToPlayList(currentAlbum);
				}
				dfd.addCallbacks(getPlayList, requestFailed);
				$.mobile.changePage("#playlist");
	        }
		});
		$("#randomForArtistButton").click(function() {
			clearPlayList();
			var dfd = servicehandler.randomPlaylist(currentArtist);
			dfd.addCallbacks(getPlayList, requestFailed);
			$.mobile.changePage("#playlist");
		});
		$("#previous").click(function() {
	        console.log("previous pressed");
			var dfd = servicehandler.previous();
			dfd.addCallbacks(requestSuccessful, requestFailed);
		});
		$("#playPause").click(function() {
	        console.log("playPause pressed");
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
	        
		});
		$("#stop").click(function() {
	        console.log("stop pressed");
			var dfd = servicehandler.stop();
			dfd.addCallbacks(requestSuccessful, requestFailed);
		});
		$("#next").click(function() {
	        console.log("next pressed");
			var dfd = servicehandler.next();
			dfd.addCallbacks(requestSuccessful, requestFailed);
		});
		$("#volume").bind( "change", function(event, ui) {
			var volume = parseFloat(event.target.value) / 10;
			var dfd = servicehandler.setVolume(volume);
			dfd.addCallbacks(requestSuccessful, requestFailed);
		});		
    });    
	return {};
});


 

 