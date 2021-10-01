helpers
methodGetter
	"MethodGetter := nil"
	^ MethodGetter ifNil: [ MethodGetter := Polyglot eval: #python string:
		'import sys; lambda mod_name, meth_name : getattr(sys.modules[str(mod_name)], str(meth_name))' ]