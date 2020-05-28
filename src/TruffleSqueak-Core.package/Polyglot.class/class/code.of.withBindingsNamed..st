helpers
code: aString of: aLanguage withBindingsNamed: aName
	^ (self importStatementFor: aLanguage named: aName)
		ifNotNil: [ :importStatement | importStatement, String crlf, aString ]
		ifNil: [ aString ]
	