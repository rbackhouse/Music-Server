var zazlConfig = {
	directInject: true,
	excludes: ['put-selector/node-html'],
	packages: [{name: 'dojo'},{name: 'dijit'},{name: 'dojox'}]
};

require(["app/desktop-dgrid", 'dojo/dom', 'dojo/dom-style'],
function(musicserver, dom, domStyle) {
	domStyle.set(dom.byId("borderContainer"), "visibility", "visible");
    console.log("musicserver loaded");
});
