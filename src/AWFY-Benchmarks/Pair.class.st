Class {
	#name : #Pair,
	#superclass : #Object,
	#instVars : [
		'key',
		'value'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Pair class >> withKey: aKey andValue: aValue [

    ^ self new initialize: aKey and: aValue

]

{ #category : #'as yet unclassified' }
Pair >> initialize: aKey and: aValue [

    key   := aKey.
    value := aValue.

]

{ #category : #'as yet unclassified' }
Pair >> key [
 ^ key 
]

{ #category : #'as yet unclassified' }
Pair >> key: aKey [
 key   := aKey 
]

{ #category : #'as yet unclassified' }
Pair >> value [
 ^ value 
]

{ #category : #'as yet unclassified' }
Pair >> value: aValue [
 value := aValue 
]
