experimental
eval: aLanguage string: aString withBindingsNamed: aName
	^ self eval: aLanguage string: (self code: aString of: aLanguage withBindingsNamed: aName)