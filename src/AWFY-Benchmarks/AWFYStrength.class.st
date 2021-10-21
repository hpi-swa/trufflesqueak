Class {
	#name : #AWFYStrength,
	#superclass : #Object,
	#instVars : [
		'symbolicValue',
		'arithmeticValue'
	],
	#classVars : [
		'AbsoluteStrongest',
		'AbsoluteWeakest',
		'Required',
		'StrengthConstants',
		'StrengthTable',
		'SymAbsoluteStrongest',
		'SymAbsoluteWeakest',
		'SymDefault',
		'SymPreferred',
		'SymRequired',
		'SymStrongDefault',
		'SymStrongPreferred',
		'SymWeakDefault'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYStrength class >> SymAbsoluteStrongest [
 ^ SymAbsoluteStrongest 
]

{ #category : #'as yet unclassified' }
AWFYStrength class >> SymAbsoluteWeakest [
 ^ SymAbsoluteWeakest 
]

{ #category : #'as yet unclassified' }
AWFYStrength class >> SymDefault [
 ^ SymDefault 
]

{ #category : #'as yet unclassified' }
AWFYStrength class >> SymPreferred [
 ^ SymPreferred 
]

{ #category : #'as yet unclassified' }
AWFYStrength class >> SymRequired [
 ^ SymRequired 
]

{ #category : #'as yet unclassified' }
AWFYStrength class >> SymStrongDefault [
 ^ SymStrongDefault 
]

{ #category : #'as yet unclassified' }
AWFYStrength class >> SymStrongPreferred [
 ^ SymStrongPreferred 
]

{ #category : #'as yet unclassified' }
AWFYStrength class >> SymWeakDefault [
 ^ SymWeakDefault 
]

{ #category : #'as yet unclassified' }
AWFYStrength class >> absoluteStrongest [

    ^ AbsoluteStrongest

]

{ #category : #'as yet unclassified' }
AWFYStrength class >> absoluteWeakest [

    ^ AbsoluteWeakest

]

{ #category : #'as yet unclassified' }
AWFYStrength class >> createStrengthConstants [

    | constants |
    constants := AWFYSomIdentityDictionary new.
    StrengthTable keys forEach: [:strengthSymbol |
      constants
        at: strengthSymbol
        put: (self new: strengthSymbol)].
    ^ constants

]

{ #category : #'as yet unclassified' }
AWFYStrength class >> createStrengthTable [

    | table |
    table := AWFYSomIdentityDictionary new.
    table at: SymAbsoluteStrongest put: -10000.
    table at: SymRequired          put: -800.
    table at: SymStrongPreferred   put: -600.
    table at: SymPreferred         put: -400.
    table at: SymStrongDefault     put: -200.
    table at: SymDefault           put: 0.
    table at: SymWeakDefault       put: 500.
    table at: SymAbsoluteWeakest   put: 10000.
    ^ table

]

{ #category : #'as yet unclassified' }
AWFYStrength class >> initialize [

    SymAbsoluteStrongest := AWFYSym new: 0.
    SymRequired          := AWFYSym new: 1.
    SymStrongPreferred   := AWFYSym new: 2.
    SymPreferred         := AWFYSym new: 3.
    SymStrongDefault     := AWFYSym new: 4.
    SymDefault           := AWFYSym new: 5.
    SymWeakDefault       := AWFYSym new: 6.
    SymAbsoluteWeakest   := AWFYSym new: 7.
  
    StrengthTable := self createStrengthTable.
    StrengthConstants := self createStrengthConstants.

    AbsoluteStrongest := AWFYStrength of: SymAbsoluteStrongest.
    AbsoluteWeakest := AWFYStrength of: SymAbsoluteWeakest.
    Required := AWFYStrength of: SymRequired.

]

{ #category : #'as yet unclassified' }
AWFYStrength class >> new: symVal [

    ^ self new initializeWith: symVal

]

{ #category : #'as yet unclassified' }
AWFYStrength class >> of: aSymbol [

    "Answer an instance with the specified strength."
    ^ StrengthConstants at: aSymbol

]

{ #category : #'as yet unclassified' }
AWFYStrength class >> required [

    ^ Required

]

{ #category : #'as yet unclassified' }
AWFYStrength class >> strengthTable [

      ^ StrengthTable

]

{ #category : #'as yet unclassified' }
AWFYStrength >> arithmeticValue [

    "Answer my arithmetic value. Used for comparisons. Note that
     STRONGER constraints have SMALLER arithmetic values."

    ^ arithmeticValue

]

{ #category : #'as yet unclassified' }
AWFYStrength >> initializeWith: symVal [

    symbolicValue   := symVal.
    arithmeticValue := AWFYStrength strengthTable at: symVal

]

{ #category : #'as yet unclassified' }
AWFYStrength >> sameAs: aStrength [

    "Answer true if I am the same strength as the given Strength."
    ^ arithmeticValue = aStrength arithmeticValue

]

{ #category : #'as yet unclassified' }
AWFYStrength >> stronger: aStrength [

    "Answer true if I am stronger than the given Strength."
    ^ arithmeticValue < aStrength arithmeticValue

]

{ #category : #'as yet unclassified' }
AWFYStrength >> strongest: aStrength [

    "Answer the stronger of myself and aStrength."

    (aStrength stronger: self)
      ifTrue:  [ ^ aStrength ]
      ifFalse: [ ^ self ].

]

{ #category : #'as yet unclassified' }
AWFYStrength >> weaker: aStrength [

    "Answer true if I am weaker than the given Strength."
    ^ arithmeticValue > aStrength arithmeticValue

]

{ #category : #'as yet unclassified' }
AWFYStrength >> weakest: aStrength [

    "Answer the weaker of myself and aStrength."

    (aStrength weaker: self)
      ifTrue:  [ ^ aStrength ]
      ifFalse: [ ^ self ].

]
