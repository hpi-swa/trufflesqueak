as yet unclassified
rubyUnbox: anObject
	"This is a terrible hack to work around the fact that TruffleRuby does not implement interop 2.0 yet"
	^ (rubyUnbox ifNil: [
			rubyUnbox := Polyglot eval: 'ruby' string: 'Truffle::Interop.method(:unbox)' ])
		primitiveExecuteWithArguments: {anObject}