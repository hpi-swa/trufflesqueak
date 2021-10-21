Class {
	#name : #Plan,
	#superclass : #Vector,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Plan class >> new [
 ^ self new: 15 
]

{ #category : #'as yet unclassified' }
Plan >> execute [

    "Execute my constraints in order."

    self forEach: [: c | c execute ]

]
