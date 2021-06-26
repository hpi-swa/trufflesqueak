fields - streaming
streamOptionalFieldsOn: aStream

	(Interop hasMembers: self object) ifTrue: [
		aStream nextPutAll: self fieldsMembers ].
	(Interop isMetaObject: self object) ifTrue: [
		aStream nextPut: self fieldMetaName ].
	(Interop hasMetaObject: self object) ifTrue: [
		aStream nextPut: self fieldMetaObjectInfo ].
	(Interop hasLanguage: self object) ifTrue: [
		aStream nextPut: self fieldLanguageInfo ].
	(Interop hasSourceLocation: self object) ifTrue: [
		aStream nextPut: self fieldSource ].
	(Interop isException: self object) ifTrue: [
		aStream nextPut: self fieldException ].