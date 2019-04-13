helpers
iconFor: aLanguageID
	aLanguageID = 'python' ifTrue: [ ^ PolyglotNotebook iconPython ].
	aLanguageID = 'ruby' ifTrue: [ ^ PolyglotNotebook iconRuby ].
	aLanguageID = 'r' ifTrue: [ ^ PolyglotNotebook iconR ].
	aLanguageID = 'js' ifTrue: [ ^ PolyglotNotebook iconJavascript ].
	aLanguageID = 'squeaksmalltalk' ifTrue: [ ^ PolyglotNotebook iconSqueakSmalltalk ].
	aLanguageID = 'llvm' ifTrue: [ ^ PolyglotNotebook iconLLVM ].
	^ PolyglotNotebook iconMarkdown