polyglot
polyglotId

	^ Smalltalk globals at: #Polyglot ifPresent: #languageIdPython ifAbsent: [#python]