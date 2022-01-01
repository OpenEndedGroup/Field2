print(" -- subinterpreter started --")
def __fieldeval__(line, ll):
    try:
        co = compile(line, "<test>", "eval")
        print(eval(co, ll))
    except:
        import sys
        e = sys.exc_info()[0]
        exec(line, ll)
        print(" &#10003; ")

class BoxWrapper(object):
	def __init__(self, bx):
		self.bx = bx

	def __getattr__(self, name):
		return self.bx.asMap_get(name)

	def __setattr__(self, name, value):
		if (name!="bx"):
			return self.bx.asMap_set(name, value)
		else:
			super().__setattr__(name, value)
