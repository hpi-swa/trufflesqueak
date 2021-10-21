Class {
	#name : #JsonNumber,
	#superclass : #JsonValue,
	#instVars : [
		'string'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
JsonNumber class >> new: string [

    string ifNil: [ self error: 'string is null' ].
    ^ self new initializeWith: string

]

{ #category : #'as yet unclassified' }
JsonNumber >> asString [
 ^ string 
]

{ #category : #'as yet unclassified' }
JsonNumber >> initializeWith: str [
 string := str 
]

{ #category : #'as yet unclassified' }
JsonNumber >> isNumber [
 ^ true 
]
