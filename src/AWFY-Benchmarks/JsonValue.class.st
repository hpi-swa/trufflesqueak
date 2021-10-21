Class {
	#name : #JsonValue,
	#superclass : #Object,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
JsonValue >> asArray [

    self error: 'Unsupported operation, not an array: ', self asString

]

{ #category : #'as yet unclassified' }
JsonValue >> asObject [

    self error: 'Unsupported operation, not an object: ', self asString

]

{ #category : #'as yet unclassified' }
JsonValue >> isArray [
 ^ false 
]

{ #category : #'as yet unclassified' }
JsonValue >> isBoolean [
 ^ false 
]

{ #category : #'as yet unclassified' }
JsonValue >> isFalse [
 ^ false 
]

{ #category : #'as yet unclassified' }
JsonValue >> isNull [
 ^ false 
]

{ #category : #'as yet unclassified' }
JsonValue >> isNumber [
 ^ false 
]

{ #category : #'as yet unclassified' }
JsonValue >> isObject [
 ^ false 
]

{ #category : #'as yet unclassified' }
JsonValue >> isString [
 ^ false 
]

{ #category : #'as yet unclassified' }
JsonValue >> isTrue [
 ^ false 
]
