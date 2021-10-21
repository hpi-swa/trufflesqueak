Class {
	#name : #AWFYDictIdEntry,
	#superclass : #AWFYDictEntry,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYDictIdEntry class >> new: hash key: key value: val next: next [

    ^ self new init: hash key: key value: val next: next

]

{ #category : #'as yet unclassified' }
AWFYDictIdEntry >> match: aHash key: aKey [

    ^ hash = aHash and: [key == aKey]

]
