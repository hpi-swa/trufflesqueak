Class {
	#name : #AWFYSimulator,
	#superclass : #Object,
	#instVars : [
		'aircrafts'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYSimulator class >> new: numAircrafts [

  ^ self new init: numAircrafts

]

{ #category : #'as yet unclassified' }
AWFYSimulator >> init: numAircrafts [

    aircrafts := AWFYVector new.

    0 to: numAircrafts - 1 do: [:i |
      aircrafts append: (AWFYCallSign new: i)]

]

{ #category : #'as yet unclassified' }
AWFYSimulator >> simulate: time [

    | frame |
    frame := AWFYVector new.
    0 to: aircrafts size - 2 by: 2 do: [:i |
      frame append: (AWFYAircraft new: (aircrafts at: i + 1)
                              pos: (AWFYVector3D x: time
                                             y: (time cos * 2.0) + (i * 3.0)
                                             z: 10.0)).
      frame append: (AWFYAircraft new: (aircrafts at: i + 2)
                              pos: (AWFYVector3D x: time
                                             y: (time sin * 2.0) + (i * 3.0)
                                             z: 10.0)) ].
    ^ frame

]
