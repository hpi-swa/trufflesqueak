experimental
eval: aLanguage string: aString withBindingsNamed: aName
	^ self primitiveEval: aLanguage string: (self code: aString of: aLanguage withBindingsNamed: aName)