Class {
	#name : #CallSign,
	#superclass : #Object,
	#instVars : [
		'value'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
CallSign class >> new: val [

    ^ self new init: val

]

{ #category : #'as yet unclassified' }
CallSign >> compareTo: other [

    ^ value = other value
        ifTrue:  [ 0 ]
        ifFalse: [
          value < other value ifTrue: [ -1 ] ifFalse: [ 1 ]]

]

{ #category : #'as yet unclassified' }
CallSign >> init: val [
 value := val 
]

{ #category : #'as yet unclassified' }
CallSign >> value [
 ^ value 
]
