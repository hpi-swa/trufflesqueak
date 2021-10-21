Class {
	#name : #AWFYEditConstraint,
	#superclass : #AWFYUnaryConstraint,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYEditConstraint class >> var: aVariable strength: strengthSymbol addTo: planner [

      "Install an edit constraint with the given strength on the given
       variable."

      ^ self new initializeVar: aVariable strength: strengthSymbol addTo: planner

]

{ #category : #'as yet unclassified' }
AWFYEditConstraint >> execute [


]

{ #category : #'as yet unclassified' }
AWFYEditConstraint >> isInput [

    "I indicate that a variable is to be changed by imperative code."
    ^ true

]
