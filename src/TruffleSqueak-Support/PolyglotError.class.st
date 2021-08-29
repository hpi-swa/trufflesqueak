Class {
	#name : #PolyglotError,
	#superclass : #Error,
	#instVars : [
		'errorObject'
	],
	#category : #'TruffleSqueak-Support'
}

{ #category : #exceptionInstantiator }
PolyglotError class >> signal [
	self new signal: Polyglot primitiveLastError
]

{ #category : #'priv handling' }
PolyglotError >> defaultAction [
	errorObject inspect.
	super defaultAction.
]

{ #category : #signaling }
PolyglotError >> signal: anErrorObject [
	errorObject := anErrorObject.
	self messageText: anErrorObject asString.
	^ self signal
]
