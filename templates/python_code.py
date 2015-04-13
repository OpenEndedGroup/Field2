

import csv
def readData(filename):
	with open(filename, 'rU') as csvfile:
		datareader = csv.reader(csvfile, delimiter=',')
		for row in datareader:
			yield row


def x(): pass
