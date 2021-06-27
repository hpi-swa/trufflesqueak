*TruffleSqueak-Core
defaultLabelForPolyglotTools
	^ String streamContents: [ :aStream |
		aStream nextPutAll: (
			(Interop hasMetaObject: self)
				ifTrue: [ (Interop getMetaQualifiedName: (Interop getMetaObject: self)) asString ]
				ifFalse: [ self class name ]).
		(Interop hasLanguage: self)
			ifTrue: [ aStream
				nextPutAll: ' (';
				nextPutAll: (Interop getLanguageInfo: self) getName asString;
				nextPutAll: ')' ] ]
