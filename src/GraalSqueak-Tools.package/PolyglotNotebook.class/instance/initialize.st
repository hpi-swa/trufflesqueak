accessing
initialize
	super initialize.
	Polyglot availableLanguages size > 1 ifTrue: [
		{[PolyglotTextStyler default startUp]} do: [ :block | block value ]
			displayingProgress: [ :block | 'Initializing polyglot environment...' ]
			every: 100 ]