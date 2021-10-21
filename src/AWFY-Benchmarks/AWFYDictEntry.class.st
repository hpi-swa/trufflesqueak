Class {
	#name : #AWFYDictEntry,
	#superclass : #Object,
	#instVars : [
		'hash',
		'key',
		'value',
		'next'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYDictEntry class >> new: hash key: key value: val next: next [

    ^ self new init: hash key: key value: val next: next

]

{ #category : #'as yet unclassified' }
AWFYDictEntry >> hash [
 ^ hash 
]

{ #category : #'as yet unclassified' }
AWFYDictEntry >> init: aHash key: aKey value: val next: anEntry [

    hash  := aHash.
    key   := aKey.
    value := val.
    next  := anEntry.

]

{ #category : #'as yet unclassified' }
AWFYDictEntry >> key [
 ^ key 
]

{ #category : #'as yet unclassified' }
AWFYDictEntry >> match: aHash key: aKey [

    ^ hash = aHash and: [key = aKey]

]

{ #category : #'as yet unclassified' }
AWFYDictEntry >> next [
 ^ next 
]

{ #category : #'as yet unclassified' }
AWFYDictEntry >> next: e [
 next := e 
]

{ #category : #'as yet unclassified' }
AWFYDictEntry >> value [
 ^ value 
]

{ #category : #'as yet unclassified' }
AWFYDictEntry >> value: val [
 value := val 
]
