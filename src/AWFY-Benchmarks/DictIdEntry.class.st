Class {
	#name : #DictIdEntry,
	#superclass : #DictEntry,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
DictIdEntry class >> new: hash key: key value: val next: next [

    ^ self new init: hash key: key value: val next: next

]

{ #category : #'as yet unclassified' }
DictIdEntry >> match: aHash key: aKey [

    ^ hash = aHash and: [key == aKey]

]
