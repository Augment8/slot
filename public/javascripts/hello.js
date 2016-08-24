'use strict';

var url = location.toString().replace("http","ws") + 'ws/';
var connection = new WebSocket(url,[]);

connection.onopen = function () {
  console.log('open');
  var name_text = document.getElementById('name_text');
  name_text.disabled = false;
  name_text.addEventListener('keydown', function(e){
    var obj = {
      type: 'ChangeName',
      value: {'name': e.srcElement.value}
    };
    connection.send(JSON.stringify(obj));
  });
  var a_button = document.getElementById('a_button');
  a_button.addEventListener('touchstart', function(){
    var obj = {
      type: 'PressButton',
      value: {'value': 'a'}
    };
    connection.send(JSON.stringify(obj));
  });

  var b_button = document.getElementById('b_button');
  b_button.addEventListener('touchstart', function(){
    var obj = {
      type: 'PressButton',
      value: {'value': 'b'}
    };
    connection.send(JSON.stringify(obj));
  });

  document.addEventListener('click', function(e){
    var obj = {
      type: 'test',
      value: {'message': 'test'}
    };
    // connection.send(JSON.stringify(obj));
  });
  document.addEventListener('touchstart', function(e){
    var touch = e.changedTouches[0];
    var obj = {
      type: 'touchstart',
      x: touch.clientX,
      y: touch.clientY
    };
    connection.send(JSON.stringify(obj));
  });
  document.addEventListener('touchmove', function(e){
    var touch = e.changedTouches[0];
    var obj = {
      type: 'touchmove',
      x: touch.clientX,
      y: touch.clientY
    };
    connection.send(JSON.stringify(obj));
  });
  document.addEventListener('touchend', function(e){
    var touch = e.changedTouches[0];
    var obj = {
      type: 'touchend',
      x: touch.clientX,
      y: touch.clientY
    };
    connection.send(JSON.stringify(obj));
  });
  window.addEventListener("devicemotion", mutator(200)(function(e){
    var obj = {
      type: 'gravity',
      x: e.accelerationIncludingGravity.x,
      y: e.accelerationIncludingGravity.y,
      z: e.accelerationIncludingGravity.z
    };
    connection.send(JSON.stringify(obj));
  }), true);
};

// Log errors
connection.onerror = function (error) {
  console.log('WebSocket Error ' + error);
};

var mutator = function (n) {
  var time = Date.now();
  return function(f) {
    return function(e) {
      var tmp = Date.now();
      if (time + n < tmp) {
        time = tmp;
        return f(e);
      }
      return null;
    };
  };
};

var session = {};
var func = {
  session_id: function(message) {
    document.cookie = "SESSION_ID=" + message.sessionId + ";";
    session.user = prompt("ユーザー名を入力してください", '');
    connection.send(JSON.stringify({type: "session", session: session}));
  },
  session: function(message) {
    var session = message.session;
    session.user = prompt("ユーザー名を入力してください。", '');
    connection.send(JSON.stringify({type: "session", session: session}));
  }
};

// Log messages from the server
connection.onmessage = function (e) {
  console.log('Server: ' + e.data);
  var message = JSON.parse(e.data);
  func[message.type](message);
};
