rouge
rubyUnbox: aString
	"This is a terrible hack to work around the fact that TruffleRuby does not implement interop 2.0 yet"
	^ rubyUnbox value: aString