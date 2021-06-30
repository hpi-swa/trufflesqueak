polyglot
polyglotId

	^ Smalltalk globals at: #Polyglot ifPresent: #languageIdSmalltalk ifAbsent: [#smalltalk]