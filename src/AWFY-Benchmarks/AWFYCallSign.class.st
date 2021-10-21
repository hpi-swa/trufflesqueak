Class {
	#name : #AWFYCallSign,
	#superclass : #Object,
	#instVars : [
		'value'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYCallSign class >> new: val [

    ^ self new init: val

]

{ #category : #'as yet unclassified' }
AWFYCallSign >> compareTo: other [

    ^ value = other value
        ifTrue:  [ 0 ]
        ifFalse: [
          value < other value ifTrue: [ -1 ] ifFalse: [ 1 ]]

]

{ #category : #'as yet unclassified' }
AWFYCallSign >> init: val [
 value := val 
]

{ #category : #'as yet unclassified' }
AWFYCallSign >> value [
 ^ value 
]
