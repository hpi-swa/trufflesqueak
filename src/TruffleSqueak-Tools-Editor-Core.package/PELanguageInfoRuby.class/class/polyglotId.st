polyglot
polyglotId

	^ Smalltalk globals at: #Polyglot ifPresent: #languageIdRuby ifAbsent: [#ruby]