Class {
	#name : #AWFYJsonArray,
	#superclass : #AWFYJsonValue,
	#instVars : [
		'values'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYJsonArray class >> new [
 ^ super new initialize 
]

{ #category : #'as yet unclassified' }
AWFYJsonArray >> add: value [

    value ifNil: [ self error: 'value is null' ].
    values append: value

]

{ #category : #'as yet unclassified' }
AWFYJsonArray >> asArray [

    ^ self

]

{ #category : #'as yet unclassified' }
AWFYJsonArray >> at: index [

    ^ values at: index

]

{ #category : #'as yet unclassified' }
AWFYJsonArray >> initialize [
 values := AWFYVector new 
]

{ #category : #'as yet unclassified' }
AWFYJsonArray >> isArray [

    ^ true

]

{ #category : #'as yet unclassified' }
AWFYJsonArray >> size [

    ^ values size

]
