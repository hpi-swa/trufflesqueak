Class {
	#name : #AbstractConstraint,
	#superclass : #Object,
	#instVars : [
		'strength'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AbstractConstraint >> addConstraint: planner [

    "Activate this constraint and attempt to satisfy it."

    self addToGraph.
    planner incrementalAdd: self.

]

{ #category : #'as yet unclassified' }
AbstractConstraint >> addToGraph [

    "Add myself to the constraint graph."
    self subclassResponsibility

]

{ #category : #'as yet unclassified' }
AbstractConstraint >> chooseMethod: mark [

    "Decide if I can be satisfied and record that decision. The output of
     the chosen method must not have the given mark and must have a
     walkabout strength less than that of this constraint."
    self subclassResponsibility

]

{ #category : #'as yet unclassified' }
AbstractConstraint >> destroyConstraint: planner [

    "Deactivate this constraint, remove it from the constraint graph,
     possibly causing other constraints to be satisfied, and destroy it."

    self isSatisfied ifTrue: [planner incrementalRemove: self].
    self removeFromGraph.

]

{ #category : #'as yet unclassified' }
AbstractConstraint >> execute [

    "Enforce this constraint. Assume that it is satisfied."
    self subclassResponsibility

]

{ #category : #'as yet unclassified' }
AbstractConstraint >> initialize: strengthSymbol [

    strength := Strength of: strengthSymbol

]

{ #category : #'as yet unclassified' }
AbstractConstraint >> inputsDo: aBlock [

    "Assume that I am satisfied. Evaluate the given block on all my current
     input variables."
    self subclassResponsibility

]

{ #category : #'as yet unclassified' }
AbstractConstraint >> inputsKnown: mark [

    "Assume that I am satisfied. Answer true if all my current inputs are
     known. A variable is known if either a) it is 'stay' (i.e. it is a
     constant at plan execution time), b) it has the given mark (indicating
     that it has been computed by a constraint appearing earlier in the
     plan), or c) it is not determined by any constraint."

    ^ (self inputsHasOne: [:v |
       (v mark = mark or: [v stay or: [v determinedBy == nil]]) not ]) not

]

{ #category : #'as yet unclassified' }
AbstractConstraint >> isInput [

    "Normal constraints are not input constraints. An input constraint is
     one that depends on external state, such as the mouse, the keyboard,
     a clock, or some arbitrary piece of imperative code."
    ^ false

]

{ #category : #'as yet unclassified' }
AbstractConstraint >> isSatisfied [

    "Answer true if this constraint is satisfied in the current solution."
    self subclassResponsibility

]

{ #category : #'as yet unclassified' }
AbstractConstraint >> markUnsatisfied [

    "Record the fact that I am unsatisfied."
    self subclassResponsibility

]

{ #category : #'as yet unclassified' }
AbstractConstraint >> output [

    "Answer my current output variable. Raise an error if I am not
     currently satisfied."
    self subclassResponsibility

]

{ #category : #'as yet unclassified' }
AbstractConstraint >> recalculate [

    "Calculate the walkabout strength, the stay flag, and, if it is 'stay',
     the value for the current output of this constraint. Assume this
     constraint is satisfied."
    self subclassResponsibility

]

{ #category : #'as yet unclassified' }
AbstractConstraint >> removeFromGraph [

    "Remove myself from the constraint graph."
    self subclassResponsibility

]

{ #category : #'as yet unclassified' }
AbstractConstraint >> satisfy: mark propagate: planner [

    "Attempt to find a way to enforce this (still unsatisfied) constraint.
     If successful, record the solution, perhaps modifying the current
     dataflow graph.  Answer the constraint that this constraint overrides,
     if there is one, or nil, if there isn't."

    | overridden |
    self chooseMethod: mark.
    self isSatisfied
        ifTrue:         "constraint can be satisfied"
            ["mark inputs to allow cycle detection in addPropagate"
             | out |
             self inputsDo: [: in | in mark: mark].
             out := self output.
             overridden := out determinedBy.
             (overridden == nil) ifFalse: [overridden markUnsatisfied].
             out determinedBy: self.
             (planner addPropagate: self mark: mark) ifFalse:
                [self error: 'Cycle encountered adding:\tConstraint removed.'.
                 ^nil].
             out mark: mark]
        ifFalse:            "constraint cannot be satisfied"
            [overridden := nil.
             (strength sameAs: (Strength required)) ifTrue:
                [self error: 'Failed to satisfy a required constraint']].
    ^ overridden

]

{ #category : #'as yet unclassified' }
AbstractConstraint >> strength [

    "Answer my strength."
    ^ strength

]
