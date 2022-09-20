# TxtNet Browser
### Browse the Web over SMS, no WiFi or Mobile Data required!
<p align="center"><img src="https://github.com/lukeaschenbrenner/TxtNet-Browser/raw/master/app/src/main/ic_launcher-playstore.png" alt="App Icon" width="200"/></p>

TextNet Browser is an Android app that allows anyone around the world to browse the web without a mobile data connection! It uses SMS as a medium of transmitting HTTP requests to a server where a pre-parsed HTML response is compressed using Google's [Brotli](https://github.com/google/brotli) compression algorithm and encoded using a custom Base-114 encoding format (based on [Basest](https://github.com/saxbophone/basest-python)).  

> ⚠️**Please note**: All web traffic  should be considered unencrypted, as all requests are made over SMS and received in plaintext by the server!

## Download
### See the **[releases page](https://github.com/lukeaschenbrenner/TxtNet-Browser/releases)** for an APK download of the TxtNet Browser client. A Google Play release is coming soon.
NOTE: This browser is in alpha stage, and as such I cannot guarantee that it will function in the future! The server may go down at any time.

## How it works

This app utilizes a permission present in Android since KitKat (4.4) which allows any app to read incoming SMS messages and send SMS messages without being your default SMS app. While this is a security concern, the code for this app is open source and does not use any internet permissions (because that's the whole point!) 
The app communicates with a "server phone number", which is a phone number controlled by a messaging API (in this case Twilio) that communicates over REST to the Python server sript. Each URL request is sent, encoded in a custom base 114, to the server. Usually, this only requires 1 SMS, but just in case, each message is prepended with an order specifier. When the server recieves a request, it uses Pyppeteer to request the website in a Chromium instance running on the server. This allows any Javascript that may exist to parse all HTML required. Once the page is loaded, only the HTML is transferred back to the recipient device. The HTML is stripped of unnecessary tags and attributes, compressed into raw bytes, and then encoded. Once encoded, the messages are split into 160 character numbered segments (maximizing the [GSM-7 standard](https://en.wikipedia.org/wiki/GSM_03.38) SMS size) and sent to the app to parse using the Twilio API.

Side note: Compression savings have been estimated to be an average of 20% using Brotli, but oftentimes it can save much more! For example, the website `example.com` in stripped HTML is 285 characters, but only requires 2 SMS messages (189 characters) to receive. Even includeing the 225% overhead in data transmission, it is still more efficient!

##### Why encode the HTML in the first place?
SMS was created in 1984, and was created to utilize the extra bytes from the data channels in phone signalling. It was originally concieved to only support 128 characters in a 7-bit alphabet. When further characters were required to support a subset of the UTF-8 character set, a new standard called UCS-2 was created. Still limited by the 160 bytes available, UCS-2 supports more characters (many of which show up in HTML documents) but limits SMS sizes to 70 characters per SMS. By encoding all data in GSM-7, more data can be sent per SMS message than sending the raw HTML over SMS. It is possible that it may be even more efficient to create an encoding system using all the characters available in UCS-2, but this limits compatibility and is out of the scope of the project.

## Server Installation (WIP)

The current source code is pointed at my own server, using a Twilio API with credits I have purchased. If you would like to run your own server, follow the instructions below:
1. Register for an account at [Twilio](https://twilio.com/), purchase a toll-free number with SMS capability, and purchase credits. (This project will not work with Twilio free accounts)
2. Create a Twilio application for the number.
3. Sign up for an [ngrok](http://ngrok.com/) account and download the ngrok application
4. Open the ngrok directory and run this command: `./ngrok tcp 5000`
5. Visit the [active numbers](https://console.twilio.com/US1/develop/phone-numbers/manage/incoming) page and add the ngrok url to the "A Message Comes In" section after selecting "webhook". For example: "https://xyz.ngrok.io/receive_sms"
6.  Download the TxtNet Browser [server script](https://github.com/lukeaschenbrenner/TxtNet-Browser/blob/master/SMS_Server_Twilio.py) and install all the required modules using "pip install x"
7.  Add your Twilio API ID and Key into your environment variables, and run the script! `python3 ./SMS_Server_Twilio.py`
8. In the TxtNet Browser app, press the three dots and press "Change Server Phone Number". Enter in the phone number you purchased from Twilio and press OK!

## FAQ/Troubleshooting

Bugs:
- It is almost impossible to stop a page load when it begins. The server migitates this by adding a restriction so that no more than 100 SMS messages are sent, but the client also tries to inform the server to stop sending messages when the button is pressed. Do not spam this button!
- Many carriers are unnecessarily rate limiting incoming text messages, so a page may look as though it "stalled" while loading on large pages. As of now the only way to fix this is to wait!
- The main search provider (FrogFind) appears to be having its own bugs with certain queries returning 0 results. Please use Google instead for those!
- In Android 12 (or possibly a new version of Google Messages?), there is a new and "improved" messages blocking feature. This results in no SMS messages getting through when a number is blocked, which makes the blocking feature of TxtNet Browser break the app! Instead of blocking messages, to get around this "feature", you can silent message notifications from the server phone number.
<img src="https://github.com/lukeaschenbrenner/TxtNet-Browser/raw/master/media/silentMessages.png" alt="Silence Number" width="200"/>

<img src="https://github.com/lukeaschenbrenner/TxtNet-Browser/raw/master/media/Messages_Migrating_Popup.png" alt="Contacts Popup" width="200"/>

<img src="https://github.com/lukeaschenbrenner/TxtNet-Browser/raw/master/media/MigratingBlockedContacts.png" alt="Migrating Contacts" width="200"/>
 - The app might randomly crash when attempting to relaunch it after some time. Please submit a PR if you know how to fix this!

## Screenshots / Demo

<table>
  <tr>
    <td> <img src="https://github.com/lukeaschenbrenner/TxtNet-Browser/raw/master/media/screenshot1.png"  alt="1" height = 640px ></td>
    <td><img src="https://github.com/lukeaschenbrenner/TxtNet-Browser/raw/master/media/screenshot2.png" alt="2" height = 640px></td>
   </tr> 
   <tr>
      <td><img src="https://github.com/lukeaschenbrenner/TxtNet-Browser/raw/master/media/screenshot3.png" alt="3" height = 640px></td>
      <td><img src="https://github.com/lukeaschenbrenner/TxtNet-Browser/raw/master/media/screenshot4.png" align="right" alt="4" height = 640px>
  </td>
  </tr>
</table>

##### Demo

https://user-images.githubusercontent.com/5207700/191133921-ee39c87a-c817-4dde-b522-cb52e7bf793b.mp4

> Demo video shown above


## Development

Feel free to submit pull requests! I am a second-year CS student with basic knowledge of Android Development and Server Development, and greatly appreciate help and support from the community.

## Future Impact
My long-term goal with this project is to eventually reach communities where such a service would be practically useful, which may include:
- Those in countries with a low median income and prohibitively expensive data plans
- Those who live under opressive governments, with near impenetrable internet censorship

If you think you might be able to help funding a local country code phone number or server, or have any other ideas, please get in contact with the email in my profile description!

## License

GPLv3 - See LICENSE.md

## Credits

Thank you to everyone who has contributed to the libraries used by this app, especially Brotli and Basest. Special thanks goes to [Coldsauce](https://github.com/ColdSauce), whose original project [Cosmos Browser](https://github.com/ColdSauce/CosmosBrowserAndroid) was the original inspiration for this project!
My original reply to his Hacker News comment is [here](https://news.ycombinator.com/item?id=30685223#30687202).
