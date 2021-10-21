Class {
	#name : #JsonArray,
	#superclass : #JsonValue,
	#instVars : [
		'values'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
JsonArray class >> new [
 ^ super new initialize 
]

{ #category : #'as yet unclassified' }
JsonArray >> add: value [

    value ifNil: [ self error: 'value is null' ].
    values append: value

]

{ #category : #'as yet unclassified' }
JsonArray >> asArray [

    ^ self

]

{ #category : #'as yet unclassified' }
JsonArray >> at: index [

    ^ values at: index

]

{ #category : #'as yet unclassified' }
JsonArray >> initialize [
 values := Vector new 
]

{ #category : #'as yet unclassified' }
JsonArray >> isArray [

    ^ true

]

{ #category : #'as yet unclassified' }
JsonArray >> size [

    ^ values size

]
