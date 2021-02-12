fields - streaming
streamOptionalFieldsOn: aStream

	(Interop hasLanguage: self object) ifTrue: [
		aStream nextPut: self fieldLanguage ].
	(Interop isMetaObject: self object) ifTrue: [
		aStream nextPut: self fieldMetaName ].
	(Interop hasMetaObject: self object) ifTrue: [
		aStream
			nextPut: self fieldMetaObject;
			nextPut: self fieldMetaObjectInfo ].
	(Interop hasSourceLocation: self object) ifTrue: [
		aStream nextPut: self fieldSource ].
	(Interop hasMembers: self object) ifTrue: [
		aStream nextPutAll: self fieldsMembers ].