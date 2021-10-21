Class {
	#name : #Simulator,
	#superclass : #Object,
	#instVars : [
		'aircrafts'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Simulator class >> new: numAircrafts [

  ^ self new init: numAircrafts

]

{ #category : #'as yet unclassified' }
Simulator >> init: numAircrafts [

    aircrafts := Vector new.

    0 to: numAircrafts - 1 do: [:i |
      aircrafts append: (CallSign new: i)]

]

{ #category : #'as yet unclassified' }
Simulator >> simulate: time [

    | frame |
    frame := Vector new.
    0 to: aircrafts size - 2 by: 2 do: [:i |
      frame append: (Aircraft new: (aircrafts at: i + 1)
                              pos: (Vector3D x: time
                                             y: (time cos * 2.0) + (i * 3.0)
                                             z: 10.0)).
      frame append: (Aircraft new: (aircrafts at: i + 2)
                              pos: (Vector3D x: time
                                             y: (time sin * 2.0) + (i * 3.0)
                                             z: 10.0)) ].
    ^ frame

]
