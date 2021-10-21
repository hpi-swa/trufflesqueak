Class {
	#name : #SomIdentityDictionary,
	#superclass : #SomDictionary,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
SomIdentityDictionary class >> new [
 ^ super new: 16 
]

{ #category : #'as yet unclassified' }
SomIdentityDictionary class >> new: size [

    ^ self new: size

]

{ #category : #'as yet unclassified' }
SomIdentityDictionary >> newEntry: aKey value: value hash: hash [

    ^ DictIdEntry new: hash key: aKey value: value next: nil

]
