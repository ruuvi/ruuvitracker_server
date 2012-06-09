import httplib, urllib, hashlib, hmac
import json
import time, sys, getopt
from urlparse import urlparse

API_VERSION = '1'

def usage():
	print """USAGE: 
  python client.py dataformat URL sharedSecret trackerCode param1key param1value param2key param2value ...

PARAMS:
  - dataformat: either json or form
EXAMPLE:
  python client.py json http://localhost:8080/api/v1/events VerySecret1 490154203237518 latitude 4916.46,N longitude 12311.12,W"""

def createJsonQuery(data):
	return json.dumps(data)

def createPostQuery(data):
	return urllib.urlencode(data)

def makeQuery(url, content, data_format):
	headers = {
		'User-Agent': 'RuuviTracker test client/1.0',
	}

	connection = httplib.HTTPConnection(url.hostname, url.port)
	if data_format == "json":
		headers['Content-type'] = 'application/json'
		dataToSend = createJsonQuery(content)
	else:
		headers['Content-type'] = 'application/x-www-form-urlencoded'
		dataToSend = createPostQuery(content)
	print("Data to send:");
	print(headers)
	print(dataToSend)
	print("   ------- ")
	connection.request("POST", url.path, dataToSend, headers)
	response = connection.getresponse()
	print("Response: %s %s" % (response.status, response.reason))
	connection.close()

def macInput(data):
  m = ""
  for key in sorted(data.iterkeys()):
    value = data[key]
    m += key
    m += ':'
    m += value
    m += '|'
  print "macInput: " + m
  return m

def computeMac(data, sharedSecret):
  digest = hashlib.sha1()
  digest.update(macInput(data))
  digest.update(sharedSecret)
  return digest.hexdigest()

def computeHMac(data, sharedSecret):
  digest = hmac.new(sharedSecret, digestmod=hashlib.sha1)
  digest.update(macInput(data))
  return digest.hexdigest() 
  
def createContent(data, sharedSecret, trackerCode):
  data['version'] = API_VERSION
  data['tracker_code'] = trackerCode
  data['time'] = time.strftime('%Y-%m-%dT%H:%M:%S.000+0200')
  data['mac'] = computeHMac(data, sharedSecret)
  return data

if __name__ == '__main__':
  if len(sys.argv) < 5:
    usage()
    sys.exit(1)
  
  data_format = sys.argv[1]
  if not data_format in ["json", "form"]:
    usage()
    sys.exit(1)

  url = urlparse(sys.argv[2])
  sharedSecret = sys.argv[3]
  trackerCode = sys.argv[4]
  
  fields = sys.argv[5:]
  data = {}
  for item in zip(fields, fields[1:])[::2]:
    data[item[0]]=item[1]
    
  content = createContent(data, sharedSecret, trackerCode)
  makeQuery(url, content, data_format)
