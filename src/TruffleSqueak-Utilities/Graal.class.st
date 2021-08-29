Class {
	#name : #Graal,
	#superclass : #Object,
	#classInstVars : [
		'runtime'
	],
	#category : 'TruffleSqueak-Utilities'
}

{ #category : #primitives }
Graal class >> primitiveGetTruffleRuntime [
	<primitive: 'primitiveGetTruffleRuntime' module: 'TruffleSqueakPlugin'>
	^ nil
]

{ #category : #accessing }
Graal class >> runtime [
	^ runtime ifNil: [ runtime := self primitiveGetTruffleRuntime ]
]
