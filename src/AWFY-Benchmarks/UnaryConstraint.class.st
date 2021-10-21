Class {
	#name : #UnaryConstraint,
	#superclass : #AbstractConstraint,
	#instVars : [
		'output',
		'satisfied'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
UnaryConstraint >> addToGraph [

    "Add myself to the constraint graph."
    output addConstraint: self.
    satisfied := false.

]

{ #category : #'as yet unclassified' }
UnaryConstraint >> chooseMethod: mark [

    "Decide if I can be satisfied and record that decision."

    satisfied :=
        output mark ~= mark and:
        [strength stronger: output walkStrength].
    ^ nil

]

{ #category : #'as yet unclassified' }
UnaryConstraint >> execute [

    "Enforce this constraint. Assume that it is satisfied."
    self subclassResponsibility

]

{ #category : #'as yet unclassified' }
UnaryConstraint >> initializeVar: aVariable strength: strengthSymbol addTo: planner [

    "Initialize myself with the given variable and strength."
    super initialize: strengthSymbol.
    output := aVariable.
    satisfied := false.
    self addConstraint: planner.

]

{ #category : #'as yet unclassified' }
UnaryConstraint >> inputsDo: aBlock [


]

{ #category : #'as yet unclassified' }
UnaryConstraint >> inputsHasOne: aBlock [

    ^ false

]

{ #category : #'as yet unclassified' }
UnaryConstraint >> isSatisfied [

    "Answer true if this constraint is satisfied in the current solution."
    ^ satisfied

]

{ #category : #'as yet unclassified' }
UnaryConstraint >> markUnsatisfied [

    "Record the fact that I am unsatisfied."
    satisfied := false.

]

{ #category : #'as yet unclassified' }
UnaryConstraint >> output [

    "Answer my current output variable."
    ^ output

]

{ #category : #'as yet unclassified' }
UnaryConstraint >> recalculate [

    "Calculate the walkabout strength, the stay flag, and, if it is 'stay',
     the value for the current output of this constraint. Assume this
     constraint is satisfied."

    output walkStrength: strength.
    output stay: self isInput not.
    output stay ifTrue: [self execute]. 
]

{ #category : #'as yet unclassified' }
UnaryConstraint >> removeFromGraph [

    "Remove myself from the constraint graph."
    output == nil ifFalse: [output removeConstraint: self].
    satisfied := false.

]
