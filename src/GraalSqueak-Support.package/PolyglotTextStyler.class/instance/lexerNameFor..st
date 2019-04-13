as yet unclassified
lexerNameFor: languageID
	languageID = 'ruby' ifTrue: [ ^ 'Ruby' ].
	languageID = 'python' ifTrue: [ ^ 'Python' ].
	languageID = 'r' ifTrue: [ ^ 'R' ].
	languageID = 'squeaksmalltalk' ifTrue: [ ^ 'Smalltalk' ].
	^ 'Markdown'