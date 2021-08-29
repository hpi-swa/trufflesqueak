Class {
	#name : #InteropTestDNUTestSubject,
	#superclass : #Object,
	#category : 'TruffleSqueak-Tests'
}

{ #category : #'error handling' }
InteropTestDNUTestSubject >> doesNotUnderstand: aMessage [
	^ aMessage arguments
		ifEmpty: [ 1234 ]
		ifNotEmpty: [ :args | args size ]
	
]
