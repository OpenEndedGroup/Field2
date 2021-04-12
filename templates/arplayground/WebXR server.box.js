
// this box represents our connection to web-browser(s)
// connected to Field on port 8090
var RemoteServerExecution = Java.type('trace.graphics.remote.RemoteServerExecution')
_.setClass(RemoteServerExecution.class)

// we'll serve up a group of webpages that are stored inside the Field2.app
// we'll have to change this depending on whether we are executing inside a 
// desktop (without AR), iOS (with our custom app), or Android (inside Canary)

var Main = Java.type("fieldagent.Main")

// this points our webserver at the files for mobile-XR
// this is built into Field
_.addDynamicRoot("AR", () => Main.app+"/lib/ar/ios_aframe_vr/")



// this also lets you serve files from your Desktop
// assuming you are on a mac and called Marc (you might have to edit this)
_.addDynamicRoot("desktop", () => "/Users/marc/Desktop/")


// automatically execute this when we start up
_.auto=1