zazl({
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
["app/mobile"], 
function(musicserver) {
    console.log("musicserver loaded");
});
