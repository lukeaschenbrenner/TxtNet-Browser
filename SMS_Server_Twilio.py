# This is the Python Server script associated with the TxtNet Browser project.
# Please see README.md for more information on how to get started or how to run your own server.

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
from pyppeteer import launch
import asyncio
import json
import signal
import logging
from logging import getLogger
from quart.logging import default_handler

#This implementation uses Twilio, but you could use an alternative service or possibly selfhost an API on your own phone number.
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
ALT_SYMBOL_TABLE = [
"@","£","$","¥","è","é","ù","ì","ò","Ç","\n","Ø","ø","Å","å","_","Δ","Φ","Γ","Λ","Ω","Π","Ψ","Σ","Θ","Ξ","Æ","æ","ß","É","!","\"","#","¤","%","&","'","(",")","*","+",",","-",".","/","0","1","2","3","4","5","6","7","8","9",":",";","<","=",">","?","¡","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","Ä","Ö","Ñ","Ü","\u00A7","¿","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","ä","ö","ñ","ü","à"
] # This new symbol table adds 10 greek characters that originally would not send due to carrier issues parsing GSM-7 messages. Still being worked on with help from Twilio since 4/2022

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
"@","£","$","¥","è","é","ù","ì","ò","Ç","\n","Ø","ø","Å","å","_","Æ","æ","ß","É","!","\"","#","¤","%","&","'","(",")","*","+",",","-",".","/","0","1","2","3","4","5","6","7","8","9",":",";","<","=",">","?","¡","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","Ä","Ö","Ñ","Ü","\u00A7","¿","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","ä","ö","ñ","ü","à"
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
    input_symbol_table = [chr(c) for c in range(115)]
    output_symbol_table = [
"@","£","$","¥","è","é","ù","ì","ò","Ç","\n","Ø","ø","Å","å","_","Æ","æ","ß","É","!","\"","#","¤","%","&","'","(",")","*","+",",","-",".","/","0","1","2","3","4","5","6","7","8","9",":",";","<","=",">","?","¡","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","Ä","Ö","Ñ","Ü","\u00A7","¿","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","ä","ö","ñ","ü","à"
    ]
    padding_symbol = '='

#########
#basest.core.best_ratio(input_base=256, output_bases=[94], chunk_sizes=range(1, 256))
#r = requests.get('https://www.almanac.com/content/how-tie-knots')

headers = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36',
}
async def get_website(url):
    #session = AsyncHTMLSession(browser_args=["--proxy-server=socks5://127.0.0.1:1080", "--host-resolver-rules=\"MAP * ~NOTFOUND , EXCLUDE 127.0.0.1\""])    
    #for no proxy: 
    session = AsyncHTMLSession()
    session.headers = headers
    #session.proxies = {'http': 'http://127.0.0.1:1080'}
    r = await session.request("GET", url, params=None, headers=headers)
    await r.html.arender()
    await session.close()
    return r

async def sendMessages(url, exceptionFlag = False):

    #TODO: REQUESTS library auto parse url eg. www
   # r = requests.get(url, headers=headers)
    r = None
    data = None
    try:
        tlds = ["aaa","aarp","abarth","abb","abbott","abbvie","abc","able","abogado","abudhabi","ac","academy","accenture","accountant","accountants","aco","active","actor","ad","adac","ads","adult","ae","aeg","aero","aetna","af","afamilycompany","afl","africa","ag","agakhan","agency","ai","aig","aigo","airbus","airforce","airtel","akdn","al","alfaromeo","alibaba","alipay","allfinanz","allstate","ally","alsace","alstom","am","americanexpress","americanfamily","amex","amfam","amica","amsterdam","analytics","android","anquan","anz","ao","aol","apartments","app","apple","aq","aquarelle","ar","arab","aramco","archi","army","arpa","art","arte","as","asda","asia","associates","at","athleta","attorney","au","auction","audi","audible","audio","auspost","author","auto","autos","avianca","aw","aws","ax","axa","az","azure","ba","baby","baidu","banamex","bananarepublic","band","bank","bar","barcelona","barclaycard","barclays","barefoot","bargains","baseball","basketball","bauhaus","bayern","bb","bbc","bbt","bbva","bcg","bcn","bd","be","beats","beauty","beer","bentley","berlin","best","bestbuy","bet","bf","bg","bh","bharti","bi","bible","bid","bike","bing","bingo","bio","biz","bj","black","blackfriday","blanco","blockbuster","blog","bloomberg","blue","bm","bms","bmw","bn","bnl","bnpparibas","bo",
        "boats","boehringer","bofa","bom","bond","boo","book","booking","bosch","bostik","boston","bot","boutique","box","br","bradesco","bridgestone","broadway","broker","brother","brussels","bs","bt","budapest","bugatti","build","builders","business","buy","buzz","bv","bw","by","bz","bzh","ca","cab","cafe","cal","call","calvinklein","cam","camera","camp","cancerresearch","canon","capetown","capital","capitalone","car","caravan","cards","care","career","careers","cars","cartier","casa","case","caseih","cash","casino","cat","catering","catholic","cba","cbn","cbre","cbs","cc","cd","ceb","center","ceo","cern","cf","cfa","cfd","cg","ch","chanel","channel","chase","chat","cheap","chintai","christmas","chrome","chrysler","church","ci","cipriani","circle","cisco","citadel","citi","citic","city","cityeats","ck","cl","claims","cleaning","click","clinic","clinique","clothing","cloud","club","clubmed","cm","cn","co","coach","codes","coffee","college","cologne","com","comcast","commbank","community","company","compare","computer","comsec","condos","construction","consulting","contact","contractors","cooking","cookingchannel","cool","coop","corsica","country","coupon","coupons","courses","cr","credit","creditcard","creditunion","cricket","crown","crs","cruise","cruises","csc","cu","cuisinella",
        "cv","cw","cx","cy","cymru","cyou","cz","dabur","dad","dance","data","date","dating","datsun","day","dclk","dds","de","deal","dealer","deals","degree","delivery","dell","deloitte","delta","democrat","dental","dentist","desi","design","dev","dhl","diamonds","diet","digital","direct","directory","discount","discover","dish","diy","dj","dk","dm","dnp","do","docs","doctor","dodge","dog","doha","domains","dot","download","drive","dtv","dubai","duck","dunlop","duns","dupont","durban","dvag","dvr","dz","earth","eat","ec","eco","edeka","edu","education","ee","eg","email","emerck","energy","engineer","engineering","enterprises","epost","epson","equipment","er","ericsson","erni","es","esq","estate","esurance","et","etisalat","eu","eurovision","eus","events","everbank","exchange","expert","exposed","express","extraspace","fage","fail","fairwinds","faith","family","fan","fans","farm","farmers","fashion","fast","fedex","feedback","ferrari","ferrero","fi","fiat","fidelity","fido","film","final","finance","financial","fire","firestone","firmdale","fish","fishing","fit","fitness","fj","fk","flickr","flights","flir","florist","flowers","fly","fm","fo","foo","food","foodnetwork","football","ford","forex","forsale","forum","foundation","fox","fr","free","fresenius","frl","frogans","frontdoor","frontier","ftr","fujitsu","fujixerox","fun","fund","furniture","futbol","fyi","ga","gal","gallery","gallo","gallup","game","games","gap","garden","gb","gbiz","gd","gdn","ge","gea","gent","genting","george","gf","gg","ggee","gh","gi","gift","gifts","gives","giving","gl","glade","glass","gle","global","globo","gm","gmail","gmbh","gmo","gmx","gn","godaddy","gold","goldpoint","golf","goo","goodhands","goodyear","goog","google","gop","got","gov","gp","gq","gr","grainger","graphics","gratis","green","gripe","grocery","group","gs","gt","gu","guardian","gucci","guge","guide","guitars","guru","gw","gy","hair","hamburg","hangout","haus","hbo","hdfc","hdfcbank","health","healthcare","help","helsinki","here","hermes","hgtv","hiphop","hisamitsu","hitachi","hiv","hk","hkt","hm","hn","hockey","holdings","holiday","homedepot","homegoods","homes","homesense","honda","honeywell","horse","hospital","host","hosting","hot","hoteles","hotels","hotmail","house","how","hr","hsbc","ht","hu","hughes","hyatt","hyundai","ibm","icbc","ice","icu","id","ie","ieee","ifm","ikano","il","im","imamat","imdb","immo","immobilien","in","industries","infiniti","info","ing","ink","institute","insurance","insure","int","intel","international","intuit","investments","io","ipiranga","iq","ir","irish","is","iselect","ismaili","ist","istanbul","it","itau","itv","iveco","iwc","jaguar","java","jcb","jcp","je","jeep","jetzt","jewelry","jio","jlc","jll","jm","jmp","jnj","jo","jobs","joburg","jot","joy","jp","jpmorgan","jprs","juegos","juniper","kaufen","kddi","ke","kerryhotels","kerrylogistics","kerryproperties","kfh","kg","kh","ki","kia","kim","kinder","kindle","kitchen","kiwi","km","kn","koeln","komatsu","kosher","kp","kpmg","kpn","kr","krd","kred","kuokgroup","kw","ky","kyoto","kz","la","lacaixa","ladbrokes","lamborghini","lamer","lancaster","lancia","lancome","land","landrover","lanxess","lasalle","lat","latino","latrobe","law","lawyer","lb","lc","lds","lease","leclerc","lefrak","legal","lego","lexus","lgbt","li","liaison","lidl","life","lifeinsurance","lifestyle","lighting","like","lilly","limited","limo","lincoln","linde","link","lipsy","live","living","lixil","lk","llc","loan","loans","locker","locus","loft","lol","london","lotte","lotto","love","lpl","lplfinancial","lr","ls","lt","ltd","ltda","lu","lundbeck","lupin","luxe","luxury","lv","ly","ma","macys","madrid","maif","maison","makeup","man","management","mango","map","market","marketing","markets","marriott","marshalls","maserati","mattel","mba","mc","mckinsey","md","me","med","media","meet","melbourne","meme","memorial","men","menu","meo","merckmsd","metlife","mg","mh","miami","microsoft","mil","mini","mint","mit","mitsubishi","mk","ml","mlb","mls","mm","mma","mn","mo","mobi","mobile","mobily","moda","moe","moi","mom","monash","money","monster","mopar","mormon","mortgage","moscow","moto","motorcycles","mov","movie","movistar","mp","mq","mr","ms","msd","mt","mtn","mtr","mu","museum","mutual","mv","mw","mx","my","mz","na","nab","nadex","nagoya","name","nationwide","natura","navy","nba","nc","ne","nec","net","netbank","netflix","network","neustar","new","newholland","news","next","nextdirect","nexus","nf","nfl","ng","ngo","nhk","ni","nico","nike","nikon","ninja","nissan","nissay","nl","no","nokia","northwesternmutual","norton","now","nowruz","nowtv","np","nr","nra","nrw","ntt","nu","nyc","nz","obi","observer","off","office","okinawa","olayan","olayangroup","oldnavy","ollo","om","omega","one","ong","onl","online","onyourside","ooo","open","oracle","orange","org","organic","origins","osaka","otsuka","ott","ovh","pa","page","panasonic","panerai","paris","pars","partners","parts","party","passagens","pay","pccw","pe","pet","pf","pfizer","pg","ph","pharmacy","phd","philips","phone","photo","photography","photos","physio","piaget","pics","pictet","pictures","pid","pin","ping","pink","pioneer","pizza","pk","pl","place","play","playstation","plumbing","plus","pm","pn","pnc","pohl","poker","politie","porn","post","pr","pramerica","praxi","press","prime","pro","prod","productions","prof","progressive","promo","properties","property","protection","pru","prudential","ps","pt","pub","pw","pwc","py","qa","qpon","quebec","quest","qvc","racing","radio","raid","re","read","realestate","realtor","realty","recipes","red","redstone","redumbrella","rehab","reise","reisen","reit","reliance","ren","rent","rentals","repair","report","republican","rest","restaurant","review","reviews","rexroth","rich","richardli","ricoh","rightathome","ril","rio","rip","rmit","ro","rocher","rocks","rodeo","rogers","room","rs","rsvp","ru","rugby","ruhr","run","rw","rwe","ryukyu","sa","saarland","safe","safety","sakura","sale","salon","samsclub","samsung","sandvik","sandvikcoromant","sanofi","sap","sapo","sarl","sas","save","saxo","sb","sbi","sbs","sc","sca","scb","schaeffler","schmidt","scholarships","school","schule","schwarz","science","scjohnson","scor","scot","sd","se","search","seat","secure","security","seek","select","sener","services","ses","seven","sew","sex","sexy","sfr","sg","sh","shangrila","sharp","shaw","shell","shia","shiksha","shoes","shop","shopping","shouji","show","showtime","shriram","si","silk","sina","singles","site","sj","sk","ski","skin","sky","skype","sl","sling","sm","smart","smile","sn","sncf","so","soccer","social","softbank","software","sohu","solar","solutions","song","sony","soy","space","spiegel","sport","spot","spreadbetting","sr","srl","srt","st","stada","staples","star","starhub","statebank","statefarm","statoil","stc","stcgroup","stockholm","storage","store","stream","studio","study","style","su","sucks","supplies","supply","support","surf","surgery","suzuki","sv","swatch","swiftcover","swiss","sx","sy","sydney","symantec","systems","sz","tab","taipei","talk","taobao","target","tatamotors","tatar","tattoo","tax","taxi","tc","tci","td","tdk","team","tech","technology","tel","telecity","telefonica","temasek","tennis","teva","tf","tg","th","thd","theater","theatre","tiaa","tickets","tienda","tiffany","tips","tires","tirol","tj","tjmaxx","tjx","tk","tkmaxx","tl","tm","tmall","tn","to","today","tokyo","tools","top","toray","toshiba","total","tours","town","toyota","toys","tr","trade","trading","training","travel","travelchannel","travelers","travelersinsurance","trust","trv","tt","tube","tui","tunes","tushu","tv","tvs","tw","tz","ua","ubank","ubs","uconnect","ug","uk","unicom","university","uno","uol","ups","us","uy","uz","va","vacations","vana","vanguard","vc","ve","vegas","ventures","verisign","versicherung","vet","vg","vi","viajes","video","vig","viking","villas","vin","vip","virgin","visa","vision","vista","vistaprint","viva","vivo","vlaanderen","vn","vodka","volkswagen","volvo","vote","voting","voto","voyage","vu","vuelos","wales","walmart","walter","wang","wanggou","warman","watch","watches","weather","weatherchannel","webcam","weber","website","wed","wedding","weibo","weir","wf","whoswho","wien","wiki","williamhill","win","windows","wine","winners","wme","wolterskluwer","woodside","work","works","world","wow","ws","wtc","wtf","xbox","xerox","xfinity","xihuan","xin","xyz","yachts","yahoo","yamaxun","yandex","ye","yodobashi","yoga","yokohama","you","youtube","yt","yun","za","zappos","zara","zero","zip","zippo","zm","zone","zuerich","zw"]
        
        protocols = ["http", "https"]
        containsTld = False 
        containsProto = False
      
        for tld in tlds:
            if((url.find(tld) != -1) and (url != tld) and (url.find(".") != -1)):
                containsTld = True
                break
        for protocol in protocols:
            if url.find(protocol) != -1:
                containsProto = True
                break
        #print(containsTld)
        #print(containsProto)
        if(url == "about:blank#blocked" or url == "about:blank"):
            raise Exception()
        elif(not containsTld):
            url = "http://frogfind.com/?q=" + url
        elif not containsProto:
            url = "http://" + url

        log.info("Loaded url: %s", url)

        r = await get_website(url)
        data = r.html.html
    except Exception:
        data = "<p>Error: The page you have requested is not available. The page may be too large.</p>"
        
        if(exceptionFlag):
            log.error("URL EXCEPTION: %s", url)
        
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

   # encodedSMSb64 = base64.b64encode(compressed).decode("UTF-8")

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

    if(howManyTextsToExpect > 100):
        log.error("Website data too long for url: %s", url)
        return sendMessages(url, True) #website is too large to send, send an error instead

    print(howManyTextsToExpect, "Process starting...")
    #print("If base64 was used:")
    return smsQueue

  #  BNUM_CHARS_PER_SMS = 158 # Leave 2 characters for positional number (115 chars * 115 = 13225 total text (arbitrary limit))
    #smsQueue = [output[i:i+NUM_CHARS_PER_SMS] for i in range(0, len(output), NUM_CHARS_PER_SMS)]
"""
    BsmsQueue =  []
    for Bi in range(0, len(encodedSMSb64), BNUM_CHARS_PER_SMS):
        BsmsQueue.append(encodedSMSb64[Bi:Bi+BNUM_CHARS_PER_SMS])

    Bi = 0
    for Bchunk in BsmsQueue:
        Bstring = ''.join(v2r(Bi, SYMBOL_TABLE))
        #print(string)
        Bchunk = Bstring + Bchunk
        #print(chunk)
        #print("\n")
        #print("{}{}".format(i,chunk))
        BsmsQueue[Bi] = Bchunk
        Bi += 1

    BhowManyTextsToExpect = math.ceil(len(BsmsQueue))
    print(BhowManyTextsToExpect)

"""





   # print("number of texts with indicators: {}".format(math.ceil(len(smsQueue))))

    



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

VALID_TAGS = ['base', 'a', 'abbr', 'address', 'article', 'aside', 'audio', 'b', 'bdi', 'bdo', 'blockquote', 'body', 'br', 'button', 'caption', 'center', 'cite', 'code', 'col', 'colgroup', 'dd', 'del', 'details', 'dfn', 'dialog', 'dl', 'em', 'fieldset', 'figure', 'footer', 'form', 'font', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'header', 'hgroup', 'hr', 'html', 'i', 'input', 'ins', 'keygen', 'legend', 'li', 'link', 'main', 'mark', 'menu', 'menuitem', 'meter', 'nav', 'noscript', 'object', 'ol', 'optgroup', 'option', 'output', 'p', 'param', 'pre', 'progress', 'q', 'rb', 'rp', 'rt', 'rtc', 'ruby', 's', 'samp', 'section', 'select', 'small', 'source', 'span', 'strong', 'sub', 'summary', 'sup', 'table', 'tbody', 'td', 'template', 'textarea', 'tfoot', 'th', 'thead', 'time', 'title', 'tr', 'track', 'u', 'ul', 'wbr', 'img'] #ok to have image here, we will remove source link
#REMOVE_TAGS = ['script', 'head', 'img', 'iframe', 'form', 'style', 'template', 'div', 'noscript']

VALID_ATTRIBUTES = ['title', 'alt', 'href', 'width', 'height', 'cellpadding', 'cellspacing', 'border', 'bgcolor', 'valign', 'align', 'halign', 'colspan', 'size', 'color', 'action', 'method', 'type', 'size', 'name', 'value', 'alink', 'link', 'text', 'vlink', 'checked', 'maxlength', 'for', 'start', 'selected', 'valuetype', 'multiple', 'rules', 'summary', 'headers', 'align', 'bgcolor', 'char', 'charoff', 'height', 'scope', 'valign', 'width', 'color', 'face', 'span', 'datetime', 'cols', 'rows', 'readonly', 'label', 'nowrap', 'align', 'border', 'char', 'cite', 'compact', 'disabled', 'longdesc', 'name', 'value', 'valign', 'vspace']
#REMOVE_ATTRIBUTES = ['style', 'id', '*', 'class', 'target', 'rel'] 
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
        if(tag.text == "img"):
            for attr in list(tag.attrs):
                if attr.startswith('src'):
                    del tag.attrs[attr]
        
        try:
            newAttrs = {}
            for item in tag.attrs.items():
                itemList = list(item)
                #if(item[0] in VALID_ATTRIBUTES):
             #   if(item[0] not in REMOVE_ATTRIBUTES):
                if(item[0] in VALID_ATTRIBUTES):
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
                print(multiPartMessages[from_])
                print(len(multiPartMessages[from_]))
                textOrder = len(multiPartMessages[from_])-1
            else:
                textOrder = r2v(str[:2], SYMBOL_TABLE)
            text = ("{}".format(str[2:]))
            if(len(text) == 157):
                text =  text + "\n"
            reassembled[textOrder] = text


        stringReassembled = (''.join(reassembled))

        #print("STRING REASSEMBLED:")
       # print(stringReassembled)

        nums = []

        for chr in stringReassembled:
            nums.append(SYMBOL_TABLE.index(chr))

       # print("NUUUUUMMMMS:")
      #  print(nums)

        garbageData = 0
        p = len(nums) - 1
        while nums[p] == 114:
            garbageData += 1
            p -= 1
        #print("NUM GARBAGE DATA:")
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

            log.error("Request is too long: %s", body)
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
            log.error("Task not present or not able to cancel. Number: %s", from_)
            print("Task not present or not able to cancel.")
            pass
        
        try:
            for sid in messagesForNumber[from_]:
                try:
                    print(client.queues(sid).delete())
                    print("Deletion of message successful.")
                except Exception:
                    log.error("Can't cancel message for sid: %s", sid)

                    print("can't cancel message! for sid ", sid)
                    pass
        except Exception:
            print("Message SID does not exist. Cancelling early perhaps?")

        try:
            messagesForNumber.pop(from_)
        except KeyError:
            print("ERROR: Could not remove messages queue from memory for {}".format(from_))
            log.error("Cannot remove messages queue from memory for %s", from_)
            pass

    else:
        app.add_background_task(background_task, from_, body)    
    #await sendWebsite(from_, body)
    #return 'Message Received'
    response = MessagingResponse().to_xml()
    return response


#This function not used for regular server purposes, only for testing.
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

async def handler(signum, frame):
    res = input("Ctrl-c was pressed. Do you really want to exit? y/n ")
    if res == 'y':
        #HTMLSession.close()
        await launch.killChrome()
        exit(1)

if __name__ == '__main__':
    #logging.basicConfig(level=logging.INFO, filename='requests.log', filemode='a', format='%(asctime)s - %(levelname)s - %(message)s')
    getLogger('quart.app').removeHandler(default_handler)

    log = logging.getLogger(__name__)
    log.setLevel(logging.INFO)
    formatter = logging.Formatter(fmt="%(asctime)s %(levelname)s: %(message)s", 
                            datefmt="%Y-%m-%d - %H:%M:%S")
    fh = logging.FileHandler("requests.log", "a")
    fh.setLevel(logging.INFO)
    fh.setFormatter(formatter)
    log.addHandler(fh)

    signal.signal(signal.SIGINT, handler)
    #print(asyncio.run((sendMessages("https://en.wikipedia.org/wiki/Bumblebee"))))

    app.run(host='0.0.0.0', debug=True)
    
    #smsQueue = ["@@@ìßGAV&ä¿qùOK3uÜ9ì.Æh9+ÉùßñFT_änåNÆ=+éO%3¤ÜèH*¿MÖG9O£7Ag=p_4=òOüIå&l/#WdG-OR*üh¤l9pc\"Ñ*8\'Vk¿E%C\'#øòåèreM2m4ñTnt>¡ß*k_ù=\'j>yÄC:U¥èHf-=1L#\nyÜÜÉzL;¿dòbCLSäM¤.VN\n", "@£@8-S$G2ì:c\"(xMn!ICZRtÜ4:UüjjrWe\"suUÄXEàààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààààà"]
    #print(decodeIt(smsQueue))
