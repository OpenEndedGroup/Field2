
var ll = _.space.withFile("c:/Users/marc/Desktop/mario.wav")
ll.play(0)
ll.loop(false)
ll.position(vec(-1,0.7,-2))

_.space.r.execute("x")

// and let's directly talk to the Resonance Audio API running in the 
// browser
_.space.r.execute(`
	// Set room acoustics properties.
	var dimensions = {
		width: 3,
		height: 3,
		depth: 3
	};

	var material = 'marble'
	var material2 = 'marble'
	var material = 'acoustic-ceiling-tiles'
	var material2 = 'acoustic-ceiling-tiles'

	var materials = {
		left: material,
		right: material,
		front: material,
		back: material2,
		down: material,
		up: material
	};

	window.songbird.setRoomProperties(dimensions, materials);
	
	window.songbird.setSpeedOfSound(10)
`)