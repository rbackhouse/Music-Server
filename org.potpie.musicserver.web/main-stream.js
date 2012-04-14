var zazlConfig = {
	directInject: true,
	packages: [{name: 'dojo'},{name: 'dijit'},{name: 'dojox'}]
};

require(["app/stream"], 
function(musicserver) {
    console.log("musicstreamer loaded");
});
