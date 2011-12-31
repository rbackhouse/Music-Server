<%@ page import="org.dojotoolkit.optimizer.JSOptimizer" %>
<%@ page import="org.dojotoolkit.optimizer.JSAnalysisData" %>
<!DOCTYPE html>
<html>
    <head>
    <title>Music Server</title>
	<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"/>
	<meta name="apple-mobile-web-app-capable" content="yes" />
	<meta name="apple-mobile-web-app-status-bar-style" content="black" />
    <style>
        @import "dojox/mobile/themes/iphone/iphone.css";
        @import "dojox/mobile/themes/iphone/ipad.css";
    </style>
    <link rel="stylesheet" href="css/rc.css"/>
	<script type="text/javascript">
        var dojoConfig = {
            locale : "<%=request.getLocale().toString().toLowerCase().replace('_', '-')%>",
            has:{
                "dojo-1x-base":1
            }
		};
	</script>
	<%
	    JSOptimizer jsOptimizer = (JSOptimizer)pageContext.getServletContext().getAttribute("org.dojotoolkit.optimizer.JSOptimizer");
	    if (jsOptimizer == null) {
	    	throw new JspException("A JSOptimizer  has not been loaded into the servlet context");
	    }
		JSAnalysisData analysisData = jsOptimizer.getAnalysisData(new String[] {"app/mobile"});
		String url = request.getContextPath() +"/_javascript?modules=app/mobile&version="+analysisData.getChecksum()+"&locale="+request.getLocale();
	%>
	<script type="text/javascript" src="<%=url%>"></script>
	<script type="text/javascript" src="main-mobile.js"></script>
    </head>
    <body style="visibility:hidden;">
		<div dojoType="dojox.mobile.View" id="artists" selected="true">
			<div dojoType="dojox.mobile.Heading" label="Artists">
				<div id="toPlayList1" data-dojo-type="dojox.mobile.ToolBarButton" label="Play List" style="float:right;" moveTo="playing"></div>
			</div>
			<div dojoType="dojox.mobile.TabBar" id="artistTabContainer" barType="segmentedControl">
			</div>
			<div dojoType="dojox.mobile.ScrollableView" id="artistView1">
				<ul dojoType="dojox.mobile.RoundRectList" id="artistList1">
				</ul>			
			</div>
			<div dojoType="dojox.mobile.ScrollableView" id="artistView2">
				<ul dojoType="dojox.mobile.RoundRectList" id="artistList2">
				</ul>			
			</div>
			<div dojoType="dojox.mobile.ScrollableView" id="artistView3">
				<ul dojoType="dojox.mobile.RoundRectList" id="artistList3">
				</ul>			
			</div>
			<div dojoType="dojox.mobile.ScrollableView" id="artistView4">
				<ul dojoType="dojox.mobile.RoundRectList" id="artistList4">
				</ul>			
			</div>
			<div dojoType="dojox.mobile.ScrollableView" id="artistView5">
				<ul dojoType="dojox.mobile.RoundRectList" id="artistList5">
				</ul>			
			</div>
		</div>		
		<div dojoType="dojox.mobile.View" id="albums">
			<div dojoType="dojox.mobile.Heading" label="Albums" back="Artists" moveTo="artists">
				<div id="toPlayList2" data-dojo-type="dojox.mobile.ToolBarButton" label="Play List" style="float:right;" moveTo="playing"></div>
			</div>
			<div dojoType="dojox.mobile.RoundRect">
				<button id="randomForArtistButton" dojoType="dojox.mobile.Button" style="width:40px">Mix</button>
			</div>	
			<div dojoType="dojox.mobile.ScrollableView" id="albumsView">
				<ul dojoType="dojox.mobile.RoundRectList" id="albumList">
				</ul>
			</div>	
		</div>		
		<div dojoType="dojox.mobile.View" id="songs">
			<div dojoType="dojox.mobile.Heading" label="Songs" back="Albums" moveTo="albums">
				<div id="toPlayList3" data-dojo-type="dojox.mobile.ToolBarButton" label="Play List" style="float:right;" moveTo="playing"></div>
			</div>
			<div dojoType="dojox.mobile.RoundRect">
				<button id="addAllButton" dojoType="dojox.mobile.Button" style="width:70px">Add All</button>
			</div>	
			<div dojoType="dojox.mobile.ScrollableView" id="songsView">
				<ul dojoType="dojox.mobile.RoundRectList" id="songList">
				</ul>
			</div>	
		</div>		
		<div dojoType="dojox.mobile.View" id="playing">
			<div dojoType="dojox.mobile.Heading" label="What's Playing" back="Artists" moveTo="artists">
			</div>
			<div dojoType="dojox.mobile.TabBar" id="playlistTabContainer" barType="segmentedControl">
				<li dojoType="dojox.mobile.TabBarButton" moveTo="playlistView">Play List</li>
				<li dojoType="dojox.mobile.TabBarButton" moveTo="controlsView">Controls</li>
			</div>
			<div dojoType="dojox.mobile.View" id="playlistView">
				<div dojoType="dojox.mobile.RoundRect">
					<button id="clearButton" dojoType="dojox.mobile.Button" style="width:60px">Clear</button>
					<button id="randomButton" dojoType="dojox.mobile.Button" style="width:70px">Random</button>
				</div>	
				<div dojoType="dojox.mobile.ScrollableView" id="playListView">
					<ul dojoType="dojox.mobile.RoundRectList" id="playList">
					</ul>
				</div>
			</div>		
			<div dojoType="dojox.mobile.View" id="controlsView">
				<div dojoType="dojox.mobile.RoundRect">
					<button id="previous" dojoType="dojox.mobile.Button" class="rewindIcon"></button>
					<button id="playPause" dojoType="dojox.mobile.Button" class="playIcon"></button>
					<button id="stop" dojoType="dojox.mobile.Button" class="stopIcon"></button>
					<button id="next" dojoType="dojox.mobile.Button" class="fastForwardIcon"></button>
				</div>
				<div dojoType="dojox.mobile.RoundRect">
					Volume:
					<input id="volume" type="range" style="width:200px;" data-dojo-type="dojox.mobile.Slider" min="1" max="10" />
				</div>	
				<div dojoType="dojox.mobile.RoundRect">
					<div id="currentlyPlaying">Currently Playing []</div>
				</div>
			</div>
		</div>
		<ul dojoType="dojox.mobile.TabBar" fixed="bottom">
			<li dojoType="dojox.mobile.TabBarButton" moveTo="artists">Artists</li>
			<li dojoType="dojox.mobile.TabBarButton" moveTo="albums">Albums</li>
			<li dojoType="dojox.mobile.TabBarButton" moveTo="songs">Songs</li>
			<li dojoType="dojox.mobile.TabBarButton" moveTo="playing">Playing</li>
		</ul>
    </body>
</html>