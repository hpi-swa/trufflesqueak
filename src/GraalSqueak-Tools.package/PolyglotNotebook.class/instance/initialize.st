accessing
initialize
	super initialize.
	Polyglot supportsMultipleLanguages ifTrue: [
		{[PolyglotTextStyler default startUp]} do: [ :block | block value ]
			displayingProgress: [ :block | 'Initializing polyglot environment...' ]
			every: 100 ]