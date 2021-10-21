Class {
	#name : #Strength,
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
Strength class >> SymAbsoluteStrongest [
 ^ SymAbsoluteStrongest 
]

{ #category : #'as yet unclassified' }
Strength class >> SymAbsoluteWeakest [
 ^ SymAbsoluteWeakest 
]

{ #category : #'as yet unclassified' }
Strength class >> SymDefault [
 ^ SymDefault 
]

{ #category : #'as yet unclassified' }
Strength class >> SymPreferred [
 ^ SymPreferred 
]

{ #category : #'as yet unclassified' }
Strength class >> SymRequired [
 ^ SymRequired 
]

{ #category : #'as yet unclassified' }
Strength class >> SymStrongDefault [
 ^ SymStrongDefault 
]

{ #category : #'as yet unclassified' }
Strength class >> SymStrongPreferred [
 ^ SymStrongPreferred 
]

{ #category : #'as yet unclassified' }
Strength class >> SymWeakDefault [
 ^ SymWeakDefault 
]

{ #category : #'as yet unclassified' }
Strength class >> absoluteStrongest [

    ^ AbsoluteStrongest

]

{ #category : #'as yet unclassified' }
Strength class >> absoluteWeakest [

    ^ AbsoluteWeakest

]

{ #category : #'as yet unclassified' }
Strength class >> createStrengthConstants [

    | constants |
    constants := SomIdentityDictionary new.
    StrengthTable keys forEach: [:strengthSymbol |
      constants
        at: strengthSymbol
        put: (self new: strengthSymbol)].
    ^ constants

]

{ #category : #'as yet unclassified' }
Strength class >> createStrengthTable [

    | table |
    table := SomIdentityDictionary new.
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
Strength class >> initialize [

    SymAbsoluteStrongest := Sym new: 0.
    SymRequired          := Sym new: 1.
    SymStrongPreferred   := Sym new: 2.
    SymPreferred         := Sym new: 3.
    SymStrongDefault     := Sym new: 4.
    SymDefault           := Sym new: 5.
    SymWeakDefault       := Sym new: 6.
    SymAbsoluteWeakest   := Sym new: 7.
  
    StrengthTable := self createStrengthTable.
    StrengthConstants := self createStrengthConstants.

    AbsoluteStrongest := Strength of: SymAbsoluteStrongest.
    AbsoluteWeakest := Strength of: SymAbsoluteWeakest.
    Required := Strength of: SymRequired.

]

{ #category : #'as yet unclassified' }
Strength class >> new: symVal [

    ^ self new initializeWith: symVal

]

{ #category : #'as yet unclassified' }
Strength class >> of: aSymbol [

    "Answer an instance with the specified strength."
    ^ StrengthConstants at: aSymbol

]

{ #category : #'as yet unclassified' }
Strength class >> required [

    ^ Required

]

{ #category : #'as yet unclassified' }
Strength class >> strengthTable [

      ^ StrengthTable

]

{ #category : #'as yet unclassified' }
Strength >> arithmeticValue [

    "Answer my arithmetic value. Used for comparisons. Note that
     STRONGER constraints have SMALLER arithmetic values."

    ^ arithmeticValue

]

{ #category : #'as yet unclassified' }
Strength >> initializeWith: symVal [

    symbolicValue   := symVal.
    arithmeticValue := Strength strengthTable at: symVal

]

{ #category : #'as yet unclassified' }
Strength >> sameAs: aStrength [

    "Answer true if I am the same strength as the given Strength."
    ^ arithmeticValue = aStrength arithmeticValue

]

{ #category : #'as yet unclassified' }
Strength >> stronger: aStrength [

    "Answer true if I am stronger than the given Strength."
    ^ arithmeticValue < aStrength arithmeticValue

]

{ #category : #'as yet unclassified' }
Strength >> strongest: aStrength [

    "Answer the stronger of myself and aStrength."

    (aStrength stronger: self)
      ifTrue:  [ ^ aStrength ]
      ifFalse: [ ^ self ].

]

{ #category : #'as yet unclassified' }
Strength >> weaker: aStrength [

    "Answer true if I am weaker than the given Strength."
    ^ arithmeticValue > aStrength arithmeticValue

]

{ #category : #'as yet unclassified' }
Strength >> weakest: aStrength [

    "Answer the weaker of myself and aStrength."

    (aStrength weaker: self)
      ifTrue:  [ ^ aStrength ]
      ifFalse: [ ^ self ].

]
