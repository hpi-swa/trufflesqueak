polyglot-api
eval: aLanguage string: aString
	^ self primitiveEval: aLanguage string: (aString replaceAll: Character cr with: Character lf) 