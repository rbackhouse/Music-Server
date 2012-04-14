var zazlConfig = {
	directInject: true,
	packages: [{name: 'dojo'},{name: 'dijit'},{name: 'dojox'}]
};

require(["app/desktop", 'dojo/dom', 'dojo/dom-style'],
function(musicserver, dom, domStyle) {
	domStyle.set(dom.byId("borderContainer"), "visibility", "visible");
    console.log("musicserver loaded");
});
