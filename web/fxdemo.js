
var hostname = "sgdemo1.solace.com"
// ,sgdemo2.solace.com"

// Secured port options
var path = '/wss'
var tls = true;
var port = "8443"
// Non-secured port options
//var path = '/ws'
//var tls = false;
//var port = "8000"

var user = "default"
var pass = "default"
var topic = "fxrates/#"

var qos = 1;
var retain = false;
var timeout = 60;
var keepAlive = 120;
var cleanSession = true;

var clientId = "" + (Math.random() + 1).toString(36).substring(2, 10);  // 8 "random" [0-9a-z]
var connected = false;

var fxtbl = null;

// Entry
$(document).ready( function () {
    fxtbl = $('#fxrate-table').DataTable({
        "paging":   false,
        "info":     false,
        "searching":   false
    });
    
    // Initiate Solace connection
    connect();
} );




// called when the client connects
function onConnect(context) {
  // Once a connection has been made, make a subscription and send a message.
  var connectionString = context.invocationContext.host + ":" + context.invocationContext.port + context.invocationContext.path;
  logMessage("INFO", "Connection Success ", "[URI: ", connectionString, ", ID: ", context.invocationContext.clientId, "]");
  var statusSpan = document.getElementById("connectionStatus");
  statusSpan.innerHTML = "Connected to Solace PubSub+: " + connectionString + " with Client ID: " + context.invocationContext.clientId;
  connected = true;

  client.subscribe(topic);
  logMessage("INFO", "Subscribed to " + topic);
}


function onConnected(reconnect, uri) {
  // Once a connection has been made, make a subscription and send a message.
  logMessage("INFO", "Client Has now connected: [Reconnected: ", reconnect, ", URI: ", uri, "]");
  connected = true;
}

function onFail(context) {
  logMessage("ERROR", "Failed to connect. [Error Message: ", context.errorMessage, "]");
  var statusSpan = document.getElementById("connectionStatus");
  statusSpan.innerHTML = "Failed to connect: " + context.errorMessage;
  connected = false;
}

// called when the client loses its connection
function onConnectionLost(responseObject) {
  if (responseObject.errorCode !== 0) {
    logMessage("INFO", "Connection Lost. [Error Message: ", responseObject.errorMessage, "]");
  }
  connected = false;
}

// called when a message arrives
function onMessageArrived(message) {

    var payload = JSON.parse(message.payloadString);
    logMessage("INFO", "TOPIC='" + message.destinationName + "',&nbsp;&nbsp;&nbsp;PAYLOAD='" + message.payloadString + "'");

    var found = false;
    var inSym = payload.symbol;
    var mySym;
    var color;

    fxtbl.rows().every( function() {
        mySym = this.data()[0];
        // logMessage("DEBUG", d);
        // logMessage("DEBUG", inSym + "-" + mySym);
        if (inSym === mySym) {
            // logMessage("DEBUG", "MATCHED");
            // update
            var newData = [ mySym, payload.buying, payload.selling];
            this.data( newData ).draw();
            // color red for + green for -
            color = (payload.direction === "+") ? "red" : "green";
            $(this.node()).find('td:eq(1)').css('color', color);
            $(this.node()).find('td:eq(2)').css('color', color);
            found = true;
            //break
            return false;
        }
    });

    if (!found) {
        // add this new row
        logMessage("DEBUG", "Adding new symbol: " + payload.symbol);
        fxtbl.row.add(
            [ inSym, payload.buying, payload.selling ]
        ).draw( false );
    }

}

function logMessage(type, ...content) {
  if (type === "INFO") {
    console.info(...content);
  } else if (type === "ERROR") {
    console.error(...content);
  } else {
    console.log(...content);
  }
}

function connect() {

  client = new Paho.MQTT.Client(hostname, Number(port), path, clientId);
  logMessage("INFO", "Connecting to Server: [Host: ", hostname, ", Port: ", port, ", Path: ", client.path, ", ID: ", clientId, "]");

  // set callback handlers
  client.onConnectionLost = onConnectionLost;
  client.onMessageArrived = onMessageArrived;
  client.onConnected = onConnected;

  var options = {
    invocationContext: { host: hostname, port: port, path: client.path, clientId: clientId },
    timeout: timeout,
    keepAliveInterval: keepAlive,
    cleanSession: cleanSession,
    useSSL: tls,
    onSuccess: onConnect,
    onFailure: onFail
  };

  if (user.length > 0) {
    options.userName = user;
  }

  if (pass.length > 0) {
    options.password = pass;
  }

  // connect the client
  client.connect(options);
  var statusSpan = document.getElementById("connectionStatus");
  statusSpan.innerHTML = "Connecting...";
}

function disconnect() {
  logMessage("INFO", "Disconnecting from Server.");
  client.disconnect();
  var statusSpan = document.getElementById("connectionStatus");
  statusSpan.innerHTML = "Connection - Disconnected.";
  connected = false;

}
