fields - streaming
streamOptionalFieldsOn: aStream

	(Interop isMetaObject: self object) ifTrue: [
		aStream nextPut: self fieldMetaName ].
	(Interop hasMetaObject: self object) ifTrue: [
		aStream
			nextPut: self fieldMetaObject;
			nextPut: self fieldMetaObjectInfo ].
	(Interop hasLanguage: self object) ifTrue: [
		aStream nextPut: self fieldLanguageInfo ].
	(Interop hasSourceLocation: self object) ifTrue: [
		aStream nextPut: self fieldSource ].
	(Interop hasMembers: self object) ifTrue: [
		aStream nextPutAll: self fieldsMembers ].