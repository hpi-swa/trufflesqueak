Class {
	#name : #AWFYJsonNumber,
	#superclass : #AWFYJsonValue,
	#instVars : [
		'string'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYJsonNumber class >> new: string [

    string ifNil: [ self error: 'string is null' ].
    ^ self new initializeWith: string

]

{ #category : #'as yet unclassified' }
AWFYJsonNumber >> asString [
 ^ string 
]

{ #category : #'as yet unclassified' }
AWFYJsonNumber >> initializeWith: str [
 string := str 
]

{ #category : #'as yet unclassified' }
AWFYJsonNumber >> isNumber [
 ^ true 
]
