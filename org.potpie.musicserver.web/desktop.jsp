<%@ page import="org.dojotoolkit.optimizer.JSOptimizer" %>
<%@ page import="org.dojotoolkit.optimizer.JSAnalysisData" %>
<!DOCTYPE html>
<html>
    <head>
    <title>Music Server</title>
	<style type="text/css">
		@import "dojo/resources/dojo.css";
		@import "dijit/themes/claro/claro.css";
        @import 'dojox/grid/resources/Grid.css';
        @import 'dojox/grid/resources/claroGrid.css';
	</style>
    <link rel="stylesheet" href="css/web.css"/>
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
		JSAnalysisData analysisData = jsOptimizer.getAnalysisData(new String[] {"app/desktop"});
		String url = request.getContextPath() +"/_javascript?modules=app/desktop&version="+analysisData.getChecksum()+"&locale="+request.getLocale();
	%>
	<script type="text/javascript" src="<%=url%>"></script>
	<script type="text/javascript" src="main-desktop.js"></script>
    </head>
    <body class="claro">
		<div data-dojo-type="dojo.data.ItemFileWriteStore" data-dojo-id="albumsStore" data-dojo-props="data: {identifier: 'title',label: 'title',items: []}"></div>		    	
		<div data-dojo-type="dojo.data.ItemFileWriteStore" data-dojo-id="artistsStore" data-dojo-props="data: {identifier: 'title',label: 'title',items: []}"></div>		    	
		<div data-dojo-type="dojo.data.ItemFileWriteStore" data-dojo-id="songsStore" data-dojo-props="data: {identifier: 'title',label: 'title',items: []}"></div>		    	
		<div data-dojo-type="dojo.data.ItemFileWriteStore" data-dojo-id="playListStore" data-dojo-props="data: {identifier: 'title',label: 'title',items: []}"></div>		    	
		<div dojoType="dijit.layout.BorderContainer" gutters="true" id="borderContainer" liveSplitters="false">
    		<div dojoType="dijit.layout.ContentPane" region="top" splitter="false">
    			<div style="font-weight: bold;" id="title">Music Server - Currently Playing [] [0]</div>
    		</div>	
		    <div dojoType="dijit.layout.AccordionContainer" minSize="20" style="width: 300px;" id="accordion" region="leading" splitter="true">
		        <div id="artistsLeft" dojoType="dijit.layout.ContentPane" title="Artists"  selected="true">
  					<label for="artistFilter">Filter Artists</label>
  					<input id="artistFilter" dojoType="dijit.form.TextBox" value="" style="width: 10em;" intermediateChanges="true"/>
		        </div>
		        <div id="albumsLeft" dojoType="dijit.layout.ContentPane" title="Albums">
   					<label for="albumFilter">Filter Albums</label>
	    			<input id="albumFilter" dojoType="dijit.form.TextBox" value="" style="width: 10em;" intermediateChanges="true"/>
		        </div>
		        <div id="songsLeft" dojoType="dijit.layout.ContentPane" title="Songs">
   					<label for="songFilter">Filter Songs</label>
	    			<input id="songFilter" dojoType="dijit.form.TextBox" value="" style="width: 10em;" intermediateChanges="true"/>
		        </div>
		        <div id="playListLeft" dojoType="dijit.layout.ContentPane" title="Play List">
		        </div>
		    </div>
		    <div id="tabContainer" dojoType="dijit.layout.TabContainer" region="center" tabStrip="true">
		    	<div id="artistsContainer" dojoType="dijit.layout.ContentPane" title="Artists" selected="true">
	   	   			<table id="artists" jsId="artists" dojoType="dojox.grid.DataGrid" store="artistsStore" clientSort="false" sortInfo="-1">
						<thead>
							<tr>
								<th field="artist" width="400px" sortDesc="true">Artist</th>
							</tr>
						</thead>
					</table>
			    </div>	
		    	<div id="albumsContainer" dojoType="dijit.layout.BorderContainer" title="Albums">
					<div dojoType="dijit.Toolbar" region="top">
						<div id="resetAlbums" dojoType="dijit.form.Button">Reset</div>
						<div id="randomForArtist" dojoType="dijit.form.Button">Mix</div>
					</div>	
	   	   			<table region="center" id="albums" jsId="albums" dojoType="dojox.grid.DataGrid" store="albumsStore" clientSort="false" sortInfo="1">
						<thead>
							<tr>
								<th field="album" width="500px">Album</th>
							</tr>
						</thead>
					</table>
			    </div>	
		    	<div id="songsContainer" dojoType="dijit.layout.BorderContainer" title="Songs">
					<div dojoType="dijit.Toolbar" region="top">
						<div id="addSelectedSongs" dojoType="dijit.form.Button">Add Selected</div>
						<div id="addAllSongs" dojoType="dijit.form.Button">Add All</div>
					</div>	
		    	   	<table region="center" id="songs" dojoType="dojox.grid.DataGrid" store="songsStore">
						<thead>
							<tr>
								<th field="id" width="50px" hidden="true"></th>
								<th field="select" width="50px" cellType="dojox.grid.cells.Bool" editable="true">&nbsp;</th>
								<th field="mp3.id3tag.track" width="50px">Track</th>
								<th field="type" width="50px">Type</th>
								<th field="title" width="300px">Title</th>
							</tr>
						</thead>
					</table>
			    </div>	
		    	<div id="playListContainer" dojoType="dijit.layout.BorderContainer" title="Play List">
					<div dojoType="dijit.Toolbar" region="top">
						<div id="randomPlaylist" dojoType="dijit.form.Button">Random</div>
						<div id="clearPlaylist" dojoType="dijit.form.Button">Clear</div>
						<div id="removeSelectedSongs" dojoType="dijit.form.Button">Remove</div>
					</div>	
		    	   	<table style="height: 80%" id="playList" dojoType="dojox.grid.DataGrid" store="playListStore" region="center">
						<thead>
							<tr>
								<th field="id" width="25px" hidden="true"></th>
								<th field="select" width="50px" cellType="dojox.grid.cells.Bool" editable="true">&nbsp;</th>
								<th field="title" width="200px">Title</th>
								<th field="artist" width="200px">Artist</th>
								<th field="album" width="350px">Album</th>
								<th field="type" width="50px">Type</th>
								<th field="offset" width="50px" hidden="true">Offset</th>
								<th field="length" width="50px" hidden="true">Length</th>
							</tr>
						</thead>
					</table>
				</div>	
		    </div>
			<div dojoType="dijit.Toolbar" region="bottom">
				<div id="musicserver.rewindButton" dojoType="dijit.form.Button" iconClass="rewindIcon rewindIconSelected" showLabel="false"></div>
				<div id="musicserver.stopButton" dojoType="dijit.form.Button" iconClass="stopIcon stopIconSelected" showLabel="false"></div>
				<div id="musicserver.playPauseButton" dojoType="dijit.form.Button" iconClass="playIcon playIconSelected" showLabel="false"></div>
				<div id="musicserver.fastForwardButton" dojoType="dijit.form.Button" iconClass="fastForwardIcon fastForwardIconSelected" showLabel="false"></div>
			</div>
			<div region="right" id="volume" dojoType="dijit.form.VerticalSlider" value="1" minimum="0" maximum="10" discreteValues="11" intermediateChanges="true" showButtons="true"></div>
    	</div>
		<div data-dojo-type="dijit.Dialog" id="configDialog" title="Music Server Configuration"">
		    <table>
		        <tr>
		            <td colspan="2">Enter the following paths to initialize the Music Database</td>
		        </tr>
		        <tr>
		            <td><label for="rootDir">Music Files Directory: </label></td>
		            <td><input data-dojo-type="dijit.form.TextBox" type="text" name="rootDir" id="rootDir"></td>
		        </tr>
		        <tr>
		            <td><label for="storageDir">Database Directory: </label></td>
		            <td><input data-dojo-type="dijit.form.TextBox" type="text" name="storageDir" id="storageDir"></td>
		        </tr>
		        <tr>
		            <td align="center" colspan="2">
		                <button id="okButton" data-dojo-type="dijit.form.Button" type="submit">OK</button>
		            </td>
		        </tr>
		    </table>
		</div>
    </body>
</html>