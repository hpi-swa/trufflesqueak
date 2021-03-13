helpers
methodListGetter
	"MethodListGetter := nil"
	^ MethodListGetter ifNil: [ MethodListGetter := Polyglot eval: 'ruby' string:
		'Proc.new{|mod_name| m = Object.const_get(mod_name); m.methods(false).concat(m.instance_methods(false))}' ]