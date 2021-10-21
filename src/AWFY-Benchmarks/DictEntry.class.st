Class {
	#name : #DictEntry,
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
DictEntry class >> new: hash key: key value: val next: next [

    ^ self new init: hash key: key value: val next: next

]

{ #category : #'as yet unclassified' }
DictEntry >> hash [
 ^ hash 
]

{ #category : #'as yet unclassified' }
DictEntry >> init: aHash key: aKey value: val next: anEntry [

    hash  := aHash.
    key   := aKey.
    value := val.
    next  := anEntry.

]

{ #category : #'as yet unclassified' }
DictEntry >> key [
 ^ key 
]

{ #category : #'as yet unclassified' }
DictEntry >> match: aHash key: aKey [

    ^ hash = aHash and: [key = aKey]

]

{ #category : #'as yet unclassified' }
DictEntry >> next [
 ^ next 
]

{ #category : #'as yet unclassified' }
DictEntry >> next: e [
 next := e 
]

{ #category : #'as yet unclassified' }
DictEntry >> value [
 ^ value 
]

{ #category : #'as yet unclassified' }
DictEntry >> value: val [
 value := val 
]
