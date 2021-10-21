Class {
	#name : #AWFYSomIdentityDictionary,
	#superclass : #AWFYSomDictionary,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYSomIdentityDictionary class >> new [
 ^ super new: 16 
]

{ #category : #'as yet unclassified' }
AWFYSomIdentityDictionary class >> new: size [

    ^ self new: size

]

{ #category : #'as yet unclassified' }
AWFYSomIdentityDictionary >> newEntry: aKey value: value hash: hash [

    ^ AWFYDictIdEntry new: hash key: aKey value: value next: nil

]
