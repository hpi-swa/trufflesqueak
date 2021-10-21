Class {
	#name : #Motion,
	#superclass : #Object,
	#instVars : [
		'callsign',
		'posOne',
		'posTwo'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Motion class >> new: callsign old: posOne new: posTwo [

    ^ self new init: callsign old: posOne new: posTwo

]

{ #category : #'as yet unclassified' }
Motion >> callsign [
 ^ callsign 
]

{ #category : #'as yet unclassified' }
Motion >> delta [

    ^ posTwo minus: posOne

]

{ #category : #'as yet unclassified' }
Motion >> findIntersection: other [

    | init1 init2 vec1 vec2 radius a dist |
    init1 := posOne.
    init2 := other posOne.
    vec1  := self delta.
    vec2  := other delta.
    radius := Constants ProximityRadius.

    " this test is not geometrical 3-d intersection test, it takes the fact that the aircraft move
       into account ; so it is more like a 4d test
       (it assumes that both of the aircraft have a constant speed over the tested interval)

       we thus have two points, each of them moving on its line segment at constant speed ; we are looking
       for times when the distance between these two points is smaller than r
       
       vec1 is vector of aircraft 1
       vec2 is vector of aircraft 2
       
       a = (V2 - V1)^T * (V2 - V1)"
    a := (vec2 minus: vec1) squaredMagnitude.

    a ~= 0.0 ifTrue: [
      | b c discr v1 v2 |
      "  we are first looking for instances of time when the planes are exactly r from each other
         at least one plane is moving ; if the planes are moving in parallel, they do not have constant speed

         if the planes are moving in parallel, then
           if the faster starts behind the slower, we can have 2, 1, or 0 solutions
           if the faster plane starts in front of the slower, we can have 0 or 1 solutions

         if the planes are not moving in parallel, then

         point P1 = I1 + vV1
         point P2 = I2 + vV2
           - looking for v, such that dist(P1,P2) = || P1 - P2 || = r

         it follows that || P1 - P2 || = sqrt( < P1-P2, P1-P2 > )
           0 = -r^2 + < P1 - P2, P1 - P2 >
          from properties of dot product
           0 = -r^2 + <I1-I2,I1-I2> + v * 2<I1-I2, V1-V2> + v^2 *<V1-V2,V1-V2>
           so we calculate a, b, c - and solve the quadratic equation
           0 = c + bv + av^2

       b = 2 * <I1-I2, V1-V2>"
      b := 2.0 * ((init1 minus: init2) dot: (vec1 minus: vec2)).

      "c = -r^2 + (I2 - I1)^T * (I2 - I1)"
      c := ((0.0 - radius) * radius) + ((init2 minus: init1) squaredMagnitude).

      discr := (b * b) - (4.0 * a * c).
      discr < 0.0 ifTrue: [ ^ nil ].

      v1 := ((0.0 - b) - discr sqrt) / (2.0 * a).
      v2 := ((0.0 - b) + discr sqrt) / (2.0 * a).

      (v1 <= v2 and: [((v1  <= 1.0 and: [1.0 <= v2]) or: [
                       (v1  <= 0.0 and: [0.0 <= v2]) or: [
                       (0.0 <= v1  and: [v2  <= 1.0])]])]) ifTrue: [
        "Pick a good 'time' at which to report the collision"
        | v result1 result2 result |
        v1 <= 0.0
          ifTrue: [
            "The collision started before this frame. Report it at the start of the frame"
            v := 0.0 ]
          ifFalse: [
            "The collision started during this frame. Report it at that moment"
            v := v1 ].

        result1 := init1 plus: (vec1 times: v).
        result2 := init2 plus: (vec2 times: v).

        result := (result1 plus: result2) times: 0.5.
        
        (result x >= Constants MinX and: [
         result x <= Constants MaxX and: [
         result y >= Constants MinY and: [
         result y <= Constants MaxY and: [
         result z >= Constants MinZ and: [
         result z <= Constants MaxZ ]]]]]) ifTrue: [ ^ result ] ].

      ^ nil ].

    "  the planes have the same speeds and are moving in parallel (or they are not moving at all)
       they  thus have the same distance all the time ; we calculate it from the initial point

       dist = || i2 - i1 || = sqrt(  ( i2 - i1 )^T * ( i2 - i1 ) )"
    dist := (init2 minus: init1) magnitude.
    dist <= radius ifTrue: [
      ^ (init1 plus: init2) times: 0.5 ].

    ^ nil

]

{ #category : #'as yet unclassified' }
Motion >> init: aCallsign old: aPosOne new: aPosTwo [

    callsign := aCallsign.
    posOne   := aPosOne.
    posTwo   := aPosTwo.

]

{ #category : #'as yet unclassified' }
Motion >> posOne [
 ^ posOne 
]

{ #category : #'as yet unclassified' }
Motion >> posTwo [
 ^ posTwo 
]
