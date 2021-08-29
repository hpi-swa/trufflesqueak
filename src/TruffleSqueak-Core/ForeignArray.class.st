Class {
	#name : #ForeignArray,
	#superclass : #ArrayedCollection,
	#instVars : [
		'foreignObject'
	],
	#category : 'TruffleSqueak-Core'
}

{ #category : #'instance creation' }
ForeignArray class >> new: sizeRequested [
	sizeRequested = 0
		ifTrue: [ ^ self on: #() ]
		ifFalse: [ ^ self error: 'Instances can only be created from ForeignObjects.' ]
]

{ #category : #'instance creation' }
ForeignArray class >> on: aForeignObject [
	(Interop hasArrayElements: aForeignObject)
		ifTrue: [ ^ self basicNew initialize on: aForeignObject ]
		ifFalse: [ self error: 'ForeignObject cannot be represented as collection' ]
]

{ #category : #accessing }
ForeignArray >> at: index [
	^  Interop readArrayElement: foreignObject index: index
]

{ #category : #accessing }
ForeignArray >> at: index put: value [
	^  Interop writeArrayElement: foreignObject index: index value: value
]

{ #category : #accessing }
ForeignArray >> basicAt: index [
	^ Interop readArrayElement: foreignObject index: index
]

{ #category : #accessing }
ForeignArray >> basicAt: index put: value [
	^ Interop writeArrayElement: foreignObject index: index value: value
]

{ #category : #accessing }
ForeignArray >> basicSize [
	^ Interop getArraySize: foreignObject
]

{ #category : #'initialize-release' }
ForeignArray >> on: aForeignObject [
	foreignObject := aForeignObject
]

{ #category : #accessing }
ForeignArray >> size [
	^ Interop getArraySize: foreignObject
]

{ #category : #private }
ForeignArray >> species [

	^ Array
]
