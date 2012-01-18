import httplib
import urllib
import sys

def usage():
	print "read the code"

def make_query(address, port, path):
	headers = {
		'Content-type': 'application/x-www-form-urlencoded',
		'User-Agent': 'RuuviTracker test client/1.0',
	}

	content = {'version': '1'}
	connection = httplib.HTTPConnection(address, port)
	connection.request("POST", path, urllib.urlencode(content), headers)
	response = connection.getresponse()
	print("%s %s" % (response.status, response.reason))
	print(response.read())
	connection.close()

if __name__ == '__main__':
	make_query("localhost", 8000, "/")
