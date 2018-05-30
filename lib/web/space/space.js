// nagivate to:
// http://localhost:63342/space/main.html
var audioContext = new AudioContext();

// Create a (1st-order Ambisonic) Songbird scene.

var songbirdOptions = {
    ambisonicOrder: 3
}

var songbird = new ResonanceAudio(audioContext, songbirdOptions);

// Send songbird's binaural output to stereo out.
songbird.output.connect(audioContext.destination);

// Set room acoustics properties.
var dimensions = {
    width: 3,
    height: 3,
    depth: 3
};

var material = 'plywood-panel'
var material2 = 'plywood-panel'

var materials = {
    left: material,
    right: material,
    front: material,
    back: material2,
    down: material,
    up: material
};

songbird.setRoomProperties(dimensions, materials);


channels = []
channelMap = {}

makeSource = function(name, url) {
    if (channelMap[name]) return channelMap[name]
    console.log(" stereo source "+url)

    channel = {}
    var audioElement = document.createElement('audio');
    audioElement.src = url;
    audioElement.load()

    var audioElementSource = audioContext.createMediaElementSource(audioElement);

    var source = songbird.createSource();
    channel["Lsource"] = source
    audioElementSource.connect(source.input)

    channel["LaudioElement"] = audioElement
    channel["LaudioElementSource"] = audioElementSource

    channel["sourceURL"] = url

    channels.push(channel)
    channelMap[name] = channel

}

_play = function (time) {

    return function(s) {
        if (s!=undefined)
        {
            channels[s].LaudioElement.currentTime = time
            channels[s].LaudioElement.play();
        }
        else {
            channels.forEach(function (x) {
                x.LaudioElement.currentTime = time
                x.LaudioElement.play();

            })
        }
    }
}

_toAll = function(name)
{
    return function(s) {
        if (s!=undefined)
        {
            channels[s].LaudioElement[name]()
       }
        else {
            channels.forEach(function (x) {
                x.LaudioElement[name]()
            })
        }
    }
}

pause = _toAll("pause")
