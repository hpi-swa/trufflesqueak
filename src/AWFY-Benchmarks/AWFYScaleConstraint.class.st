Class {
	#name : #AWFYScaleConstraint,
	#superclass : #AWFYBinaryConstraint,
	#instVars : [
		'scale',
		'offset'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYScaleConstraint class >> var: src var: scale var: offset var: dst strength: strengthSymbol addTo: planner [

    "Install a scale constraint with the given strength on the given
     variables."
    ^ self new initializeSrc: src scale: scale offset: offset dst: dst strength: strengthSymbol addTo: planner

]

{ #category : #'as yet unclassified' }
AWFYScaleConstraint >> addToGraph [

    "Add myself to the constraint graph."
    v1 addConstraint: self.
    v2 addConstraint: self.
    scale  addConstraint: self.
    offset addConstraint: self.
    direction := nil.

]

{ #category : #'as yet unclassified' }
AWFYScaleConstraint >> execute [

    "Enforce this constraint. Assume that it is satisfied."
    direction = #forward
        ifTrue:  [ v2 value: (v1 value * scale value) + offset value ]
        ifFalse: [ v1 value: (v2 value - offset value) // scale value ].

]

{ #category : #'as yet unclassified' }
AWFYScaleConstraint >> initializeSrc: srcVar scale: scaleVar offset: offsetVar dst: dstVar strength: strengthSymbol addTo: planner [

    "Initialize myself with the given variables and strength."

    super initializeVar: srcVar var: dstVar strength: strengthSymbol addTo: planner.
    scale  := scaleVar.
    offset := offsetVar.

    self addConstraint: planner.

]

{ #category : #'as yet unclassified' }
AWFYScaleConstraint >> inputsDo: aBlock [

    "Evaluate the given block on my current input variable."
    direction = #forward
        ifTrue: [aBlock value: v1.
                 aBlock value: scale.
                 aBlock value: offset]
        ifFalse: [aBlock value: v2.
                  aBlock value: scale.
                  aBlock value: offset].

]

{ #category : #'as yet unclassified' }
AWFYScaleConstraint >> recalculate [

    "Calculate the walkabout strength, the stay flag, and, if it is 'stay',
     the value for the current output of this constraint. Assume this
     constraint is satisfied."

    | in out |
    direction = #forward
        ifTrue:  [in  := v1. out := v2]
        ifFalse: [out := v1. in  := v2].
    out walkStrength: (strength weakest: in walkStrength).
    out stay: (in stay and: [scale stay and: [offset stay]]).
    out stay ifTrue: [self execute]. 
]

{ #category : #'as yet unclassified' }
AWFYScaleConstraint >> removeFromGraph [

    "Remove myself from the constraint graph."
    v1 == nil ifFalse: [ v1 removeConstraint: self ].
    v2 == nil ifFalse: [ v2 removeConstraint: self ].
    scale  == nil ifFalse: [ scale removeConstraint: self ].
    offset == nil ifFalse: [ offset removeConstraint: self ].
    direction := nil.

]
