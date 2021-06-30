helpers
methodGetter
	"MethodGetter := nil"
	^ MethodGetter ifNil: [ MethodGetter := Polyglot eval: #ruby string:
		'Proc.new{|mod_name,meth_name| m1 = Object.const_get(mod_name); m2 = meth_name.to_s.to_sym; if m1.methods(false).include?(m2);m1.method(m2);else;m1.instance_method(m2);end}' ]