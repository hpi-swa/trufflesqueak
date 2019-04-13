helpers
colorFor: aLanguageID
	| id |
	id := aLanguageID asLowercase.
	id = self markdownID ifTrue: [^ Color fromString: '#E0E0E0' ].
	id = 'js' ifTrue: [^ Color fromString: '#FFEB3B'].
	id = 'llvm' ifTrue: [^ Color fromString: '#CE93D8'].
	id = 'python' ifTrue: [^ Color fromString: '#039BE5'].
	id = 'r' ifTrue: [^ Color fromString: '#81D4FA'].
	id = 'ruby' ifTrue: [^ Color fromString: '#E57373'].
	id = 'squeaksmalltalk' ifTrue: [^ Color fromString: '#9CCC65'].
	^ Color gray