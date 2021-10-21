Class {
	#name : #AWFYPair,
	#superclass : #Object,
	#instVars : [
		'key',
		'value'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYPair class >> withKey: aKey andValue: aValue [

    ^ self new initialize: aKey and: aValue

]

{ #category : #'as yet unclassified' }
AWFYPair >> initialize: aKey and: aValue [

    key   := aKey.
    value := aValue.

]

{ #category : #'as yet unclassified' }
AWFYPair >> key [
 ^ key 
]

{ #category : #'as yet unclassified' }
AWFYPair >> key: aKey [
 key   := aKey 
]

{ #category : #'as yet unclassified' }
AWFYPair >> value [
 ^ value 
]

{ #category : #'as yet unclassified' }
AWFYPair >> value: aValue [
 value := aValue 
]
