# Created by Luke Aschenbrenner on 9/17/21
# Please see README.md for more information (that is, once I actually make one)
# TODO: make readme file
# Simple instructions: add API ID and key into environment variables, use ngrok url to redirect post requests from twilio's console to port 5000, and set twilio phone number to yours in the app!

from os import environ
import basest
import htmlmin
from bs4 import BeautifulSoup, Comment, Doctype
import pyquery
from basest.encoders import Encoder
from basest.exceptions import ImproperUsageError
import brotli
import base64
import requests
import lxml
import csv
import json
import math
import time
from requests_html import HTMLSession, AsyncHTMLSession
import asyncio
import json
import signal

from twilio.twiml.messaging_response import MessagingResponse, Message
from twilio.rest import Client
import urllib

#from flask import Flask, request, Response
from quart import Quart, request, Response
#import plivo << alternate service

#import numpy
gsm = ("@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞ\x1bÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?"
       "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà")
ext = ("````````````````````^```````````````````{}`````\\````````````[~]`"
       "|````````````````````````````````````€``````````````````````````")
SYMBOL_TABLE = [
"@","£","$","¥","è","é","ù","ì","ò","Ç","\n","Ø","ø","Å","å","_","Æ","æ","ß","É","!","\"","#","¤","%","&","'","(",")","*","+",",","-",".","/","0","1","2","3","4","5","6","7","8","9",":",";","<","=",">","?","¡","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","Ä","Ö","Ñ","Ü","\u00A7","¿","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","ä","ö","ñ","ü","à"
]
#Removed symbols for working prototype (US carriers are deleting greek symbols when using SMS API services):
#"@","£","$","¥","è","é","ù","ì","ò","Ç","\n","Ø","ø","Å","å","_", "Æ","æ","ß","É","!","\"","#","¤","%","&","'","(",")","*","+",",","-",".","/","0","1","2","3","4","5","6","7","8","9",":",";","<","=",">","?","¡","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","Ä","Ö","Ñ","Ü","`","¿","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","ä","ö","ñ","ü","à"
#    ]

tasksList = {}
messagesForNumber = {}

multiPartMessages = {}
decodedUrlByNumber = {}


def gsm_encode(plaintext):
    res = ""
    for c in plaintext:
        idx = gsm.find(c)
        if idx != -1:
            res += chr(idx)
            continue
        idx = ext.find(c)
        if idx != -1:
            res += chr(27) + chr(idx)
    #return binascii.b2a_hex(res.encode('utf-8'))
    return res.encode('utf-8')



client = Client(environ.get("TWILIO_API_ID"), environ.get("TWILIO_API_KEY"))
app = Quart(__name__)


async def sendWebsite(number, url):
    print("sendWebsite called.....")
    data = await sendMessages(url)
    await outbound_sms(number, "{} Process starting".format(len(data)))
    await asyncio.sleep(1)
    messagesForNumber[number] = list()
    for chunk in data:
        message = await outbound_sms(number, chunk)
        messagesForNumber[number].append(message.sid)

    try:
        tasksList.pop(number)
    except KeyError:
        pass

    await asyncio.sleep(240)
    try:
        messagesForNumber.pop(number)
    except KeyError:
        pass


class smsEncoder(Encoder):
    input_base = 256
    output_base = 114
 #   output_base = 114
  #  input_ratio = 135
    input_ratio = 134
    output_ratio = 158

   # input_ratio = 6
   # output_ratio = 7
   # these attributes are only required if using decode() and encode()
    input_symbol_table = [chr(c) for c in range(115)]
    output_symbol_table = [
"@","£","$","¥","è","é","ù","ì","ò","Ç","\n","Ø","ø","Å","å","Δ","_","Φ","Γ","Λ","Ω","Π","Ψ","Σ","Θ","Ξ","Æ","æ","ß","É","!","\"","#","¤","%","&","'","(",")","*","+",",","-",".","/","0","1","2","3","4","5","6","7","8","9",":",";","<","=",">","?","¡","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","Ä","Ö","Ñ","Ü","`","¿","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","ä","ö","ñ","ü","à"
    ]
    padding_symbol = '='

######
    

class smsDecoder(Encoder):
    input_base = 114
   # output_base = 126
    output_base = 256
  
   # input_ratio = 7
   # output_ratio = 6
    input_ratio = 158
    output_ratio = 134
   
   # these attributes are only required if using decode() and encode()
    input_symbol_table = [chr(c) for c in range(125)]
    output_symbol_table = [
"@","£","$","¥","è","é","ù","ì","ò","Ç","\n","Ø","ø","Å","å","Δ","_","Φ","Γ","Λ","Ω","Π","Ψ","Σ","Θ","Ξ","Æ","æ","ß","É","!","\"","#","¤","%","&","'","(",")","*","+",",","-",".","/","0","1","2","3","4","5","6","7","8","9",":",";","<","=",">","?","¡","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","Ä","Ö","Ñ","Ü","`","¿","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","ä","ö","ñ","ü","à"
    ]
    padding_symbol = '='

#########
#basest.core.best_ratio(input_base=256, output_bases=[94], chunk_sizes=range(1, 256))
#r = requests.get('https://www.almanac.com/content/how-tie-knots')

headers = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36',
}
async def get_website(url):
    session = AsyncHTMLSession()
    session.headers = headers
    r = await session.request("GET", url, params=None, headers=headers)
    await r.html.arender()
    await session.close()
    return r

async def sendMessages(url):

    #TODO: REQUESTS library auto parse url eg. www
   # r = requests.get(url, headers=headers)
    r = None
    data = None
    try:
        tlds = [".com", ".net", ".org", ".gov", ".au", ".ru", ".nz", ".co", ".me", ".de", ".se", ".uk", ".ca", ".network", ".life", ".shop", ".il", ".gr", ".eu", ".kr", ".kp", ".in", ".info", ".ai", ".xyz", ".io", ".top", ".pro", ".pw", ".club", ".cc", ".tech", ".tv", ".biz", ".online", ".tk", ".gg", ".store", ".work", ".it", ".cloud", ".es", ".live"]
        protocols = ["http", "https"]
        containsTld = False 
        containsProto = False      
        for tld in tlds:
            if url.find(tld) != -1:
                containsTld = True
                break
        for protocol in protocols:
            if url.find(protocol) != -1:
                containsProto = True
                break
        print(containsTld)
        print(containsProto)
        if not containsTld:
            url = "http://frogfind.com/?q=" + url
        elif not containsProto:
            url = "http://" + url
        r = await get_website(url)
        data = r.html.html
    except Exception:
        data = "<p>Error: The page you have requested is not available.</p>"
        
    #await r.html.arender()
    #.encode(encoding='UTF-8')


    soup = sanitize_html(data)
    final = htmlmin.minify(str(soup), True, True, True, True, True, False, True, False).encode(encoding='UTF-8')
    # ^ of type bytes

    ######## TESTING BELOW ###########

    ##### USE THE BELOW FOR TESTING
    with open('soupHtml.html', 'w', encoding='utf-8') as f:
        f.write(final.decode())
    #print(final.decode())
    #print('\n')
    #print(data)
    #print('\n')

    compressed = brotli.compress(mode=1, data=final)
    #n = int(compressed.encode('hex'), 16)

    #print(list(bytearray(compressed)))

    #print(n)
    #print(compressed.decode())
    #hexrep = (binascii.hexlify(compressed))

    #print(type(hexrep))

    #print(int.from_bytes(compressed, byteorder='little'))
    encoder = smsEncoder()
    encodedsms = encoder.encode_raw(compressed)
    #print(encodedsms)

    #print(encodedsms)
    #print('\n')
    #print(encodedsms)
    #print('\n')

    output = ""
    #strings are immutable
    for singleval in encodedsms:
        output = output + SYMBOL_TABLE[singleval]


    NUM_CHARS_PER_SMS = 158 # Leave 2 characters for positional number (115 chars * 115 = 13225 total text (arbitrary limit))
    #smsQueue = [output[i:i+NUM_CHARS_PER_SMS] for i in range(0, len(output), NUM_CHARS_PER_SMS)]
    smsQueueWithoutIndicators = [output[i:i+160] for i in range(0, len(output), 160)]

    smsQueue =  []
    for i in range(0, len(output), NUM_CHARS_PER_SMS):
        smsQueue.append(output[i:i+NUM_CHARS_PER_SMS])

    i = 0
    for chunk in smsQueue:
        string = ''.join(v2r(i, SYMBOL_TABLE))
        #print(string)
        chunk = string + chunk
        #print(chunk)
        #print("\n")
        #print("{}{}".format(i,chunk))
        smsQueue[i] = chunk
        i += 1

    howManyTextsToExpect = math.ceil(len(smsQueue))

    print(howManyTextsToExpect, "Process starting...")

   # print("number of texts with indicators: {}".format(math.ceil(len(smsQueue))))

    return smsQueue



#print(output)
#print('\n')
#print(len(output))

#n = int(compressed.encode()
#print(gsm_encode("Hello World"))

#print(len(base64.b64encode(compressed))) #.decode()
#print(len(base64.b85encode(compressed)))

#print('\n')
#print(base64.b85encode(compressed).decode())

#extraChars = 0


#VALID_TAGS = ['head', 'base', 'a', 'abbr', 'address', 'article', 'aside', 'audio', 'b', 'bdi', 'bdo', 'blockquote', 'body', 'br', 'button', 'caption', 'center', 'cite', 'code', 'col', 'colgroup', 'dd', 'del', 'details', 'dfn', 'dialog', 'div', 'dl', 'em', 'fieldset', 'figure', 'footer', 'form', 'font', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'header', 'hgroup', 'hr', 'html', 'i', 'input', 'ins', 'keygen', 'legend', 'li', 'link', 'main', 'mark', 'menu', 'menuitem', 'meter', 'nav', 'noscript', 'object', 'ol', 'optgroup', 'option', 'output', 'p', 'param', 'pre', 'progress', 'q', 'rb', 'rp', 'rt', 'rtc', 'ruby', 's', 'samp', 'section', 'select', 'small', 'source', 'span', 'strong', 'sub', 'summary', 'sup', 'table', 'tbody', 'td', 'template', 'textarea', 'tfoot', 'th', 'thead', 'time', 'title', 'tr', 'track', 'u', 'ul', 'wbr']
#TODO: BRING BACK HEAD TAG, (maybe div?) it has the website title!

VALID_TAGS = ['base', 'a', 'abbr', 'address', 'article', 'aside', 'audio', 'b', 'bdi', 'bdo', 'blockquote', 'body', 'br', 'button', 'caption', 'center', 'cite', 'code', 'col', 'colgroup', 'dd', 'del', 'details', 'dfn', 'dialog', 'dl', 'em', 'fieldset', 'figure', 'footer', 'form', 'font', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'header', 'hgroup', 'hr', 'html', 'i', 'input', 'ins', 'keygen', 'legend', 'li', 'link', 'main', 'mark', 'menu', 'menuitem', 'meter', 'nav', 'noscript', 'object', 'ol', 'optgroup', 'option', 'output', 'p', 'param', 'pre', 'progress', 'q', 'rb', 'rp', 'rt', 'rtc', 'ruby', 's', 'samp', 'section', 'select', 'small', 'source', 'span', 'strong', 'sub', 'summary', 'sup', 'table', 'tbody', 'td', 'template', 'textarea', 'tfoot', 'th', 'thead', 'time', 'title', 'tr', 'track', 'u', 'ul', 'wbr']
#REMOVE_TAGS = ['script', 'head', 'img', 'iframe', 'form', 'style', 'template', 'div', 'noscript']

VALID_ATTRIBUTES = []
REMOVE_ATTRIBUTES = ['style', 'id', '*', 'class', 'target', 'rel'] 
#REMOVE_ATTRIBUTES = ['style', 'id', 'name', 'src', '*', 'class'] 

def sanitize_html(data):
    ## cool url sanitizing: https://github.com/acejr1337/smsdialup/blob/main/Server/src/main/java/chace/smsdialupbackend/SmsReceiver.java
    ## consider using https://pypi.org/project/html-sanitizer/
    soup = BeautifulSoup(data, 'lxml')

    for tag in soup.findAll(True):
        if tag.name not in VALID_TAGS and tag.name != 'div':
            #tag.hidden = True
            tag.decompose()
            #tag.attrs = [(attr, tag.attr.value) for attr in tag.attrs if attr in VALID_ATTRIBUTES]
        elif tag.name == 'div':
            tag.unwrap()

    #return soup.renderContents()

    for tag in soup.recursiveChildGenerator():

        try:
            newAttrs = {}
            for item in tag.attrs.items():
                itemList = list(item)
                #if(item[0] in VALID_ATTRIBUTES):
                if(item[0] not in REMOVE_ATTRIBUTES):
                    if(item[0] == 'src' or item[0] == 'action'):
                        if(str(item[1])[:4] != 'http' and str(item[1])[:3] != 'www' and str(item[1])[:1] != "/"):
                            itemList[1] = "./" + item[1]
                        elif(str(item[1])[:4] != 'http' and str(item[1])[:3] != 'www'):
                            itemList[1] = "." + item[1]
                        item = tuple(itemList)
                    newAttrs[item[0]] = str(item[1])
            tag.attrs = newAttrs

            
        except AttributeError: 
            # 'NavigableString' object has no attribute 'attrs'
            pass


    for element in soup.contents:
        if(isinstance(element, Comment) or isinstance(element, Doctype)):
            element.extract()

    
    return soup

#    #for s in soup.select('script'):
#    #    s.extract()
#    
#
#    for s in soup(REMOVE_TAGS):
#        s.decompose()
#
#
#
#
#    #for attr_del in REMOVE_ATTRIBUTES: 
#    #    [soup.attrs.pop(attr_del) for s in soup.find_all() if attr_del in soup.attrs]
#
#
#    for s in soup.select('div'):
#        s.unwrap()
#        
#    for s in soup.select('noscript'):
#        s.unwrap()





def v2r(num, alphabet):
    """Convert base 10 number into a string of a custom base (alphabet)."""
    tempNum = num
    alphabet_length = len(alphabet)
    result = []
    if(num == 0):
        result = [alphabet[0]] + [alphabet[0]]
    while num > 0:
        result = [alphabet[num % alphabet_length]] + result
        num  = num // alphabet_length
    if(tempNum < alphabet_length and tempNum != 0):
        result = [alphabet[0]] + result
    return result


def r2v(data, alphabet):
    """Convert string of a custom base (alphabet) back into base 10 number."""
    alphabet_length = len(alphabet)
    num = 0
    for char in data:
        num = alphabet_length * num + alphabet.index(char)
    return num

#print(v2r(15126, SYMBOL_TABLE)) # Works up to 15,124
#print(r2v(['£', 'à'], SYMBOL_TABLE))

#i=1
#preappend = ''
#if(range(splitOutput < 125)):
#    preappend += SYMBOL_TABLE[0]
#for bigChunk in range(splitOutput)//125:
#    preappend = preappend + SYMBOL_TABLE[bigChunk]
#for(chunk in 
#    
#for chunk in splitOutput:
#    print(pre.encode((i).to_bytes(2, byteorder='big')))
#    #print(pre.encode([r]))
#    i+=10
    


@app.route('/send_sms', methods=['POST'])
async def outbound_sms(to_, body_):
    response = client.messages.create(
      from_='+18884842216',
      to=to_,
      body=body_
    )
    return response


async def background_task(from_, body):
    if(body[:2] == "@@" or (body[:2] == "àà" and multiPartMessages.get(from_) != None and len(multiPartMessages[from_]) > 0)):
        multiPartMessages[from_] = []

    try:
        try:
            multiPartMessages[from_].append(body)
        except KeyError:
            multiPartMessages[from_] = [body]
        #print("MPM:")
        #print(multiPartMessages)

        reassembled = [None] * len(multiPartMessages[from_])
        
        for str in multiPartMessages[from_]:
            if(body[:2] == "àà"):
                #print(multiPartMessages[from_])
                #print(len(multiPartMessages[from_]))
                textOrder = len(multiPartMessages[from_])-1
            else:
                textOrder = r2v(str[:2], SYMBOL_TABLE)
            text = ("{}".format(str[2:]))
            if(len(text) == 157):
                text =  text + "\n"
            reassembled[textOrder] = text


        stringReassembled = (''.join(reassembled))

       # print("STRING REASSEMBLED:")
       # print(stringReassembled)

        nums = []

        for chr in stringReassembled:
            nums.append(SYMBOL_TABLE.index(chr))

       # print("NUUUUUMMMMS:")
       # print(nums)

        garbageData = 0
        p = len(nums) - 1
        while nums[p] == 114:
            garbageData += 1
            p -= 1
       # print("NUM GARBAGE DATA:")
       # print(garbageData)
        decode = smsDecoder()

        urlWithGarbage = decode.encode_raw(input_data=nums)
        

        urlEncoded = urlWithGarbage[:len(urlWithGarbage) - garbageData]
        
        decodedUrlString = bytes(urlEncoded).decode("UTF-8")
        try:
            decodedUrlByNumber[from_] += decodedUrlString
        except KeyError:
            decodedUrlByNumber[from_] = decodedUrlString

        if(body[:2] == "àà"):
            finalDecString = decodedUrlByNumber[from_]
            asyncio.create_task(sendWebsite(from_, finalDecString))
            decodedUrlByNumber[from_] = ""


    except ImproperUsageError:
        if not multiPartMessages[from_] == None and len(multiPartMessages[from_]) > 1500:
            multiPartMessages[from_] = [] ##cache is getting too long
        return
        #this just means we don't have the full message yet! need to implement splitting up message every 160 chars and v2r/r2v in android




@app.route('/receive_sms', methods=['POST'])
async def inbound_sms():
    print("inbound_sms recieved/called....")
    from_ = (await request.values).get('From')
    to = (await request.values).get('To')
    body = str((await request.values).get('Body', None))
    print('Message received - From: %s, To: %s, Text: %s' %(from_, to, body))

    if body.find("Website Cancel") != -1: #stop sending messages!
        try:
            poppedTask = tasksList.pop(from_)
            if poppedTask != None:
                print("CANCELLING POPPED TASK!")
                poppedTask.cancel()
        except Exception:
            print("Task not present or not able to cancel.")
            pass
        
        try:
            for sid in messagesForNumber[from_]:
                try:
                    print(client.queues(sid).delete())
                    print("Deletion of message successful.")
                except Exception:
                    print("can't cancel message! for sid ", sid)
                    pass
        except Exception:
            print("Message SID does not exist. Cancelling early perhaps?")

        try:
            messagesForNumber.pop(from_)
        except KeyError:
            print("ERROR: Could not remove messages queue from memory for {}".format(from_))
            pass

    else:
        app.add_background_task(background_task, from_, body)    
    #await sendWebsite(from_, body)
    #return 'Message Received'
    return 'OK'


def decodeIt(smsQueue):
    #out2 = output.encode(encoding='UTF-8')
    #print(out2)

    howManyTextsToExpect = math.ceil(len(smsQueue))
    reassembled = [None] * howManyTextsToExpect
    #print(out2)
    for str in smsQueue:
        textOrder = r2v(str[:2], SYMBOL_TABLE)
        text = ("{}".format(str[2:]))
        if(len(text) == 157):
            text =  text + "\n"
        reassembled[textOrder] = text

    #print(reassembled)

    stringReassembled = (''.join(reassembled))
    nums = []

    #for chr in output:
    for chr in stringReassembled:
        #print(chr)
        nums.append(SYMBOL_TABLE.index(chr))

    #print(nums)
    #print(nums)
    #print(nums)
    #print(nums)
    #print(len(nums))
    decode = smsDecoder()
    print(len(nums))
    nuevo = decode.encode_raw(input_data=nums)

    #print(nuevo)
    #print(nums)
    print(nuevo)

    binstring = bytes(nuevo)
    #for oneval in nuevo:
    #    binstring = binstring + (oneval).decode()

    #print(binstring)

    newDecompressed = brotli.decompress(nuevo)


    #print(newDecompressed.decode())

    with open('decoded.html', 'w', encoding='UTF-8') as f:
        f.write(newDecompressed.decode())

    return newDecompressed.decode('UTF-8')
    #print(nums)
    #print(out2)

def handler(signum, frame):
    res = input("Ctrl-c was pressed. Do you really want to exit? y/n ")
    if res == 'y':
        #HTMLSession.close()
        exit(1)

if __name__ == '__main__':
    signal.signal(signal.SIGINT, handler)
    #print(environ.get("TWILIO_API_KEY"))
    app.run(host='0.0.0.0', debug=True)
    
    #smsQueue = ["@@@ìßGAV&ä¿qùOK3uÜ9ì.Æh9+ÉùßñFT_änåNÆ=+éO%3¤ÜèH*¿MÖG9O£7Ag=p_4=òOüIå&l/#WdG-OR*üh¤l9pc\"Ñ*8\'Vk¿E%C\'#øòåèreM2m4ñTnt>¡ß*k_ù=\'j>yÄC:U¥èHf-=1L#\nyÜÜÉzL;¿dòbCLSäM¤.VN\n", "@£@8-S$G2ì:c\"(xMn!ICZRtÜ4:UüjjrWe\"suUÄXEàààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààà"]
    #print(decodeIt(smsQueue))

