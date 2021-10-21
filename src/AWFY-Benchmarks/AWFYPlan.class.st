Class {
	#name : #AWFYPlan,
	#superclass : #AWFYVector,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYPlan class >> new [
 ^ self new: 15 
]

{ #category : #'as yet unclassified' }
AWFYPlan >> execute [

    "Execute my constraints in order."

    self forEach: [: c | c execute ]

]
