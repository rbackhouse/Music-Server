amdlite({
    packages: [
        {
            name: 'dojo',
            location: 'dojo',
            main:'main'
        },
        {
            name: 'dijit',
            location: 'dijit',
            main:'main'
        },
        {
            name: 'dojox',
            location: 'dojox',
            main:'main'
        }
    ]
}, 
["app/stream"], 
function(musicserver) {
    console.log("musicstreamer loaded");
});
