polyglot
polyglotId

	^ Smalltalk globals at: #Polyglot ifPresent: #languageIdJS ifAbsent: ['js']