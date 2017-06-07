
// builder is imported from the box above
// _.builder takes a lot more CPU time than local variable access

b = _.builder
b.open()

// aux channel '1'= (1,1,1) for future vertices
b.aux(1,1,1,1)

// which is at position (0,0,0)
b.v(0,0,0)

b.aux(1,1,0,1)
b.v(1,0,0)
b.aux(1,0,1,0)
b.v(1,1,0)

// this makes a triangle over the last three vertices (counting backwards from the most recent one '0'
b.e(0,1,2) 
b.close()