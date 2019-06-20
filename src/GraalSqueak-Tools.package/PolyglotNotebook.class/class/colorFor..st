helpers
colorFor: aLanguageID
	| id |
	id := aLanguageID asLowercase.
	id = Polyglot languageIdJS ifTrue: [^ Color fromString: '#FFEB3B'].
	id = Polyglot languageIdLLVM ifTrue: [^ Color fromString: '#CE93D8'].
	id = Polyglot languageIdMarkdown ifTrue: [^ Color fromString: '#E0E0E0' ].
	id = Polyglot languageIdPython ifTrue: [^ Color fromString: '#039BE5'].
	id = Polyglot languageIdR ifTrue: [^ Color fromString: '#81D4FA'].
	id = Polyglot languageIdRuby ifTrue: [^ Color fromString: '#E57373'].
	id = Polyglot languageIdSqueakSmalltalk ifTrue: [^ Color fromString: '#9CCC65'].
	^ Color gray