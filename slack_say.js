

var CHANNEL_NAME = 'xxxx';
var TOKEN = 'xxxx-99999999999-xxxxxxxxxxxxxxxxxxxxxxxx' ;

if(process.argv.length < 3) {
  console.log('arg err.');
  process.exit(1);
}

var Botkit = require('./lib/Botkit.js');
var os = require('os');
var controller = Botkit.slackbot({ debug: false });
var bot = controller.spawn({ token: TOKEN }).startRTM();
controller.on('rtm_open',function(bot,message) {
    bot.api.channels.list({}, function (err, response) {
        var channel_id = '';
        var message = process.argv[2];
        if(process.argv.length >= 4) {
            CHANNEL_NAME = process.argv[3];
        }
        if (response.hasOwnProperty('channels') && response.ok) {
            var total = response.channels.length;
            for (var i = 0; i < total; i++) {
                var channel = response.channels[i];
                if(channel.name == CHANNEL_NAME){
                    channel_id  = channel.id;
                    break;
                }
            }
        }
        bot.say( { text: message, channel: channel_id   });  
        setTimeout(function(){ process.exit(0); }, 1 * 1000);
    });
}); 

