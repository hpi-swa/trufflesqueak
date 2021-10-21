Class {
	#name : #AWFYVariable,
	#superclass : #Object,
	#instVars : [
		'value',
		'constraints',
		'determinedBy',
		'walkStrength',
		'stay',
		'mark'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYVariable class >> new [

    ^ super new initialize

]

{ #category : #'as yet unclassified' }
AWFYVariable class >> value: aValue [

      | o |
      o := self new.
      o value: aValue.
      ^ o

]

{ #category : #'as yet unclassified' }
AWFYVariable >> addConstraint: aConstraint [

    "Add the given constraint to the set of all constraints that refer
     to me."

    constraints append: aConstraint.

]

{ #category : #'as yet unclassified' }
AWFYVariable >> constraints [

    "Answer the set of constraints that refer to me."
    ^ constraints

]

{ #category : #'as yet unclassified' }
AWFYVariable >> determinedBy [

    "Answer the constraint that determines my value in the current
     dataflow."
    ^ determinedBy

]

{ #category : #'as yet unclassified' }
AWFYVariable >> determinedBy: aConstraint [

    "Record that the given constraint determines my value in the current
     data flow."
    determinedBy := aConstraint.

]

{ #category : #'as yet unclassified' }
AWFYVariable >> initialize [

    value := 0.
    constraints  := AWFYVector new: 2.
    determinedBy := nil.
    walkStrength := AWFYStrength absoluteWeakest.
    stay := true.
    mark := 0.

]

{ #category : #'as yet unclassified' }
AWFYVariable >> mark [

    "Answer my mark value."
    ^ mark

]

{ #category : #'as yet unclassified' }
AWFYVariable >> mark: markValue [

    "Set my mark value."
    mark := markValue.

]

{ #category : #'as yet unclassified' }
AWFYVariable >> removeConstraint: c [

    "Remove all traces of c from this variable."
    constraints remove: c.
    determinedBy == c ifTrue: [ determinedBy := nil ].

]

{ #category : #'as yet unclassified' }
AWFYVariable >> stay [

    "Answer my stay flag."
    ^ stay

]

{ #category : #'as yet unclassified' }
AWFYVariable >> stay: aBoolean [

    "Set my stay flag."
    stay := aBoolean

]

{ #category : #'as yet unclassified' }
AWFYVariable >> value [

    "Answer my value."
    ^ value

]

{ #category : #'as yet unclassified' }
AWFYVariable >> value: anObject [

    "Set my value."
    value := anObject.

]

{ #category : #'as yet unclassified' }
AWFYVariable >> walkStrength [

    "Answer my walkabout strength in the current dataflow."
    ^ walkStrength

]

{ #category : #'as yet unclassified' }
AWFYVariable >> walkStrength: aStrength [

    "Set my walkabout strength in the current dataflow."
    walkStrength := aStrength.

]
