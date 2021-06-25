rouge
lexerNameFor: languageID
	languageID = Polyglot languageIdJS ifTrue: [ ^ 'Javascript' ].
	languageID = Polyglot languageIdRuby ifTrue: [ ^ 'Ruby' ].
	languageID = Polyglot languageIdPython ifTrue: [ ^ 'Python' ].
	languageID = Polyglot languageIdR ifTrue: [ ^ 'R' ].
	languageID = Polyglot languageIdSmalltalk ifTrue: [ ^ 'Smalltalk' ].
	^ 'Markdown'