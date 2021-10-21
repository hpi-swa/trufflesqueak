Class {
	#name : #EqualityConstraint,
	#superclass : #BinaryConstraint,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
EqualityConstraint class >> var: variable1 var: variable2 strength: strengthSymbol addTo: planner [

    "Install a constraint with the given strength equating the given
     variables."

    ^ self new initializeVar: variable1 var: variable2 strength: strengthSymbol addTo: planner

]

{ #category : #'as yet unclassified' }
EqualityConstraint >> execute [

    "Enforce this constraint. Assume that it is satisfied."
    direction = #forward
      ifTrue:  [ v2 value: v1 value ]
      ifFalse: [ v1 value: v2 value ].

]

{ #category : #'as yet unclassified' }
EqualityConstraint >> initializeVar: variable1 var: variable2 strength: strengthSymbol addTo: planner [

    super initializeVar: variable1 var: variable2 strength: strengthSymbol addTo: planner.
    self addConstraint: planner.

]
