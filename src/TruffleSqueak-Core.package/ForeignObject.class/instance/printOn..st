printing
printOn: aStream
	Polyglot emphasizeInterop
		ifTrue: [ self printInteropOn: aStream ]
		ifFalse: [ aStream nextPutAll: self asString ]