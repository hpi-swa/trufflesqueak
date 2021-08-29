"
This class provides access to GraalVM's host language (Java).
"
Class {
	#name : #Java,
	#superclass : #Object,
	#category : 'TruffleSqueak-Core'
}

{ #category : #primitives }
Java class >> addToHostClassPath: aPath [
	<primitive: 'primitiveAddToHostClassPath' module: 'PolyglotPlugin'>
	PolyglotError signal
]

{ #category : #'primitives internal' }
Java class >> asHostObject: aGuestObject [
	<primitive: 'primitiveToHostObject' module: 'PolyglotPlugin'>
	(self isHostObject: self)
		ifFalse: [ self error: 'Object not a host object' ]
		ifTrue: [ self error: 'Unexpected error' ]
]

{ #category : #'primitives internal' }
Java class >> asInteger: aString [
	<primitive: 'primitiveToJavaInteger' module: 'PolyglotPlugin'>
	self primitiveFailed
]

{ #category : #'primitives internal' }
Java class >> asString: aString [
	<primitive: 'primitiveToJavaString' module: 'PolyglotPlugin'>
	^ aString
]

{ #category : #'primitives internal' }
Java class >> asTriState: aBoolean [
	<primitive: 'primitiveToTriState' module: 'PolyglotPlugin'>
	self primitiveFailed
]

{ #category : #'primitives internal' }
Java class >> parseExceptionType [
	<primitive: 'primitiveGetParseExceptionType' module: 'PolyglotPlugin'>
	self primitiveFailed
]

{ #category : #primitives }
Java class >> primitiveAddToHostClassPath: aPath [
	<primitive: 'primitiveAddToHostClassPath' module: 'PolyglotPlugin'>
	PolyglotError signal
]

{ #category : #primitives }
Java class >> primitiveIsHostLookupAllowed [
	<primitive: 'primitiveIsHostLookupAllowed' module: 'PolyglotPlugin'>
	^ false
]

{ #category : #primitives }
Java class >> primitiveLookupHostSymbol: aSymbol [
	<primitive: 'primitiveLookupHostSymbol' module: 'PolyglotPlugin'>
	self error: aSymbol, ' not found'
]

{ #category : #'primitives internal' }
Java class >> runtimeExceptionType [
	<primitive: 'primitiveGetRuntimeExceptionType' module: 'PolyglotPlugin'>
	self primitiveFailed
]

{ #category : #'primitives internal' }
Java class >> throwAsPolyglotException: anException [
	<primitive: 'primitiveThrowAsPolyglotException' module: 'PolyglotPlugin'>
	self primitiveFailed
]

{ #category : #'primitives internal' }
Java class >> throwUnsupportedMessageException [
	<primitive: 'primitiveThrowUnsupportedMessageException' module: 'PolyglotPlugin'>
	self primitiveFailed
]

{ #category : #'polyglot-api' }
Java class >> type: aJavaType [
	^ self primitiveLookupHostSymbol: aJavaType
]
