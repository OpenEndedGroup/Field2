(defprotocol IBoxAccess
	"helpful methods for accessing '_', the Field box hierarchy"
	(x_-> [this k] "Looks up a key in the box hierarchy")
	(x_<- [this k v] "Sets a key here in the box hierarchy"))

(extend-type fieldlinker.Linker$AsMap
	IBoxAccess
	(x_-> [this k] (.asMap_get this (name k)))
	(x_<- [this k v] (.asMap_set this (name k) v)))

(defn _<- 
	([this k v] (x_<- this k v))
	([k v] (x_<- _ k v))
	([this k1 k2 & more] 
	 (if (satisfies? IBoxAccess this)
		 (apply _<- (x_-> this k1) k2 more)
		 (apply _<- (x_-> _ k1) k2 more))))

(defn _-> ([this k] (x_-> this k))
	([k] (x_-> _ k)))
