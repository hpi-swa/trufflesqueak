Class {
	#name : #JsonLiteral,
	#superclass : #JsonValue,
	#instVars : [
		'value',
		'isNull',
		'isTrue',
		'isFalse'
	],
	#classVars : [
		'FALSE',
		'NULL',
		'TRUE'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
JsonLiteral class >> FALSE [
 ^ FALSE 
]

{ #category : #'as yet unclassified' }
JsonLiteral class >> NULL [
 ^ NULL 
]

{ #category : #'as yet unclassified' }
JsonLiteral class >> TRUE [
 ^ TRUE 
]

{ #category : #'as yet unclassified' }
JsonLiteral class >> initialize [

    NULL  := self new initializeWith: 'null'.
    TRUE  := self new initializeWith: 'true'.
    FALSE := self new initializeWith: 'false'.

]

{ #category : #'as yet unclassified' }
JsonLiteral >> asString [
 ^ value 
]

{ #category : #'as yet unclassified' }
JsonLiteral >> initializeWith: val [

    value   := val.
    isNull  := 'null'  = val.
    isTrue  := 'true'  = val.
    isFalse := 'false' = val.

]

{ #category : #'as yet unclassified' }
JsonLiteral >> isBoolean [
 ^ isTrue || isFalse 
]

{ #category : #'as yet unclassified' }
JsonLiteral >> isFalse [
 ^ isFalse 
]

{ #category : #'as yet unclassified' }
JsonLiteral >> isNull [
 ^ isNull 
]

{ #category : #'as yet unclassified' }
JsonLiteral >> isTrue [
 ^ isTrue 
]
