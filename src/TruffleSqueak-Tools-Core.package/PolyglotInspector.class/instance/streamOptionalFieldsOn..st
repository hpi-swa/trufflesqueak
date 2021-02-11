fields - streaming
streamOptionalFieldsOn: aStream

	(Interop hasLanguage: self object) ifTrue: [
		aStream nextPut: self fieldLanguage ].
	(Interop hasMetaObject: self object) ifTrue: [
		aStream nextPut: self fieldMetaObject ].
	(Interop isMetaObject: self object) ifTrue: [
		aStream nextPut: self fieldMetaName ].
	(Interop hasSourceLocation: self object) ifTrue: [
		aStream nextPut: self fieldSource ].
	(Interop hasMembers: self object) ifTrue: [
		aStream nextPut: self fieldMembers ].