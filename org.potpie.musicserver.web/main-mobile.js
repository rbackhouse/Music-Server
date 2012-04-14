var zazlConfig = {
	directInject: true,
	packages: [{name: 'dojo'},{name: 'dijit'},{name: 'dojox'}]
};

require(["app/mobile"], 
function(musicserver) {
    console.log("musicserver loaded");
});
