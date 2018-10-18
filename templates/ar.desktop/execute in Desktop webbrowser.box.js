
// this box represents our connection to web-browser(s)
// connected to Field on port 8090
var RemoteServerExecution = Java.type('trace.graphics.remote.RemoteServerExecution')
_.setClass(RemoteServerExecution.class)

// we'll serve up a group of webpages that are stored inside the Field2.app
// we'll have to change this depending on whether we are executing inside a 
// desktop (without AR), iOS (with our custom app), or Android (inside Canary)

var Main = Java.type("fieldagent.Main")

// this points our webserver at the files for iOS
//_.addDynamicRoot("AR", () => Main.app+"lib/ar/sketch1_ios/")

// this uses the desktop version
_.addDynamicRoot("AR", () => Main.app+"lib/ar/three.js/")


// automatically execute this when we start up
_.auto=1