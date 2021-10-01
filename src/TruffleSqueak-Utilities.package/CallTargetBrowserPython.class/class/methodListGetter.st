helpers
methodListGetter
	"MethodListGetter := nil"
	^ MethodListGetter ifNil: [ MethodListGetter := Polyglot eval: #python string:
		'import sys; lambda mod_name : [x for x in dir(sys.modules[str(mod_name)]) if x[0].islower()]' ]