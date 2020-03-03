rouge
rubyUnbox: anObject
	"This is a terrible hack to work around the fact that TruffleRuby does not implement interop 2.0 yet"
	^ Interop execute: (rubyUnbox ifNil: [
		{[ Polyglot eval: 'ruby' string: '1 + 1' ]} do: [ :block | block value ]
			displayingProgress: [ :block | 'Initializing Ruby for polyglot text styler...' ]
			every: 100.
		rubyUnbox := self rubyInteropUnbox ])
		with: {anObject}