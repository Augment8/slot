(function(global){
  'use strict';

  var ua = window.navigator.userAgent;
  var isAndroid = false;
  if (/Android/.test(ua)) {
    isAndroid = true;
  }

  var url = location.toString().replace("http","ws") + 'ws/';
  var connection = null;
  var open = function() {
      if (connection === null || connection.readyState !== WebSocket.OPEN) {
        connection = new WebSocket(url,[]);
        global.slot = connection;
      }
  };
  open();
  setInterval(open, 5000);

  document.documentElement.addEventListener('touchstart', function (e) {
    if (e.target.nodeName !== "INPUT") {
      e.preventDefault();
    }
  }, true);

  document.addEventListener("DOMContentLoaded", function(e) {
    var emitChangeName = function (name) {
      var obj = {
        type: 'ChangeName',
        value: {'name': name}
      };
      connection.send(JSON.stringify(obj));
    };

    // 名前の反映
    var name_text = document.getElementById('name_text');
    var cookie = getCookies();
    name_text.value = cookie.name || '';

    connection.addEventListener('open', function(){
      name_text.disabled = false;
      emitChangeName(name_text.value);

      name_text.addEventListener('keyup', function(e){
        var name = e.srcElement.value;
        emitChangeName(name);
        console.log(name);
        setCookie('name', name);
      });
    });

    connection.addEventListener('open', function(){
      var button = document.getElementById('a_button');
      button.addEventListener('touchstart', function(){
        var obj = {
          type: 'PressButton',
          value: {'value': 'a'}
        };
        connection.send(JSON.stringify(obj));
      }, true);
    });

    connection.addEventListener('open', function() {
      var button = document.getElementById('b_button');
      button.addEventListener('touchstart', function(){
        var obj = {
          type: 'PressButton',
          value: {'value': 'b'}
        };
        connection.send(JSON.stringify(obj));
      }, true);
    });

    connection.addEventListener('open', function() {
      var button = document.getElementById('c_button');
      button.addEventListener('touchstart', function(){
        var obj = {
          type: 'PressButton',
          value: {'value': 'c'}
        };
        connection.send(JSON.stringify(obj));
      }, true);
    });

    connection.addEventListener('open', function() {
      var button = document.getElementById('up_button');
      button.addEventListener('touchstart', function(){
        var obj = {
          type: 'PressButton',
          value: {'value': 'up'}
        };
        connection.send(JSON.stringify(obj));
      }, true);
      button.addEventListener('touchend', function(){
        var obj = {
          type: 'ReleaseButton',
          value: {'value': 'up'}
        };
        connection.send(JSON.stringify(obj));
      }, true);
    });

    connection.addEventListener('open', function() {
      var button = document.getElementById('down_button');
      button.addEventListener('touchstart', function(e){
        var obj = {
          type: 'PressButton',
          value: {'value': 'down'}
        };
        connection.send(JSON.stringify(obj));
        e.preventDefault();
      }, true);
      button.addEventListener('touchend', function(){
        var obj = {
          type: 'ReleaseButton',
          value: {'value': 'down'}
        };
        connection.send(JSON.stringify(obj));
        e.preventDefault();
      }, true);
    });

    connection.addEventListener('open', function() {
      var button = document.getElementById('left_button');
      button.addEventListener('touchstart', function(){
        var obj = {
          type: 'PressButton',
          value: {'value': 'left'}
        };
        connection.send(JSON.stringify(obj));
      }, true);
      button.addEventListener('touchend', function(){
        var obj = {
          type: 'ReleaseButton',
          value: {'value': 'left'}
        };
        connection.send(JSON.stringify(obj));
      }, true);
    });

    connection.addEventListener('open', function() {
      var button = document.getElementById('right_button');
      button.addEventListener('touchstart', function(){
        var obj = {
          type: 'PressButton',
          value: {'value': 'right'}
        };
        connection.send(JSON.stringify(obj));
      }, true);
      button.addEventListener('touchend', function(){
        var obj = {
          type: 'ReleaseButton',
          value: {'value': 'right'}
        };
        connection.send(JSON.stringify(obj));
      }, true);
    });

  });

  connection.addEventListener('open', function(e){
    console.log('WebSocket open');
    console.log(e);
    var state = WebSocket.OPEN;
  });

  connection.addEventListener('error', function(e){
    console.log('WebSocket Error :' + e);
    setTimeout(function(){
      open();
    },1000);
  });

  connection.addEventListener('close', function(e){
    console.log('WebSocket Error :');
    console.log(e);
    setTimeout(function(){
      open();
    },1000);
  });

  connection.addEventListener('open', function() {
    document.addEventListener('click', function(e){
      var obj = {
        type: 'test',
        value: {'message': 'test'}
      };
      // connection.send(JSON.stringify(obj));
    });
  });

  // 傾きセンサー
  connection.addEventListener('open', function() {
    window.addEventListener("devicemotion", mutator(1000/30)(function(e){
      var obj = {
        type: 'Gravity',
        value: {
          x: e.accelerationIncludingGravity.x * (isAndroid ? -1 :1),
          y: e.accelerationIncludingGravity.y,
          z: e.accelerationIncludingGravity.z * (isAndroid ? -1 :1)
        }
      };
      connection.send(JSON.stringify(obj));
    }), true);
  });
  /*
  connection.onopen = function () {
    console.log('open');

    /*
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
     //
  };
   */
// Log errors

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

  var setCookie = function(key, value) {
    document.cookie = [key, encodeURIComponent(value)].join('=') + ";";
  };

  function getCookies()
  {
    var cookie = document.cookie;
    if( cookie == '' ) {
      return {};
    }
    var values = cookie.split( '; ' );
    return values.reduce(function(acc, x){
      var value = x.split( '=' );
      acc[value[0]] = decodeURIComponent(value[1]);
      return acc;
    }, {});
  }

  var session = {};
  var func = {
    session_id: function(message) {
      setCookie('SESSION_ID', message.sessionId);
    }
  };

// Log messages from the server
  connection.addEventListener('message', function(e){
    console.log('Server: ' + e.data);
    var message = JSON.parse(e.data);
    func[message.type](message);
  });
})(window);
