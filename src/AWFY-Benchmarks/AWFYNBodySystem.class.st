Class {
	#name : #AWFYNBodySystem,
	#superclass : #Object,
	#instVars : [
		'bodies'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYNBodySystem class >> new [

      ^ super new initialize

]

{ #category : #'as yet unclassified' }
AWFYNBodySystem >> advance: dt [

    1 to: bodies size do: [:i |
      | iBody |
      iBody := bodies at: i.

      i + 1 to: bodies size do: [:j |
        | dx dy dz jBody dSquared distance mag |
        jBody := bodies at: j.
        dx := iBody x - jBody x.
        dy := iBody y - jBody y.
        dz := iBody z - jBody z.

        dSquared := (dx * dx) + (dy * dy) + (dz * dz).
        distance := dSquared sqrt.
        mag      := dt / (dSquared * distance).

        iBody vx: iBody vx - (dx * jBody mass * mag).
        iBody vy: iBody vy - (dy * jBody mass * mag).
        iBody vz: iBody vz - (dz * jBody mass * mag).

        jBody vx: jBody vx + (dx * iBody mass * mag).
        jBody vy: jBody vy + (dy * iBody mass * mag).
        jBody vz: jBody vz + (dz * iBody mass * mag).
      ].
    ].

    bodies do: [:body |
       body x: body x + (dt * body vx).
       body y: body y + (dt * body vy).
       body z: body z + (dt * body vz).
    ].

]

{ #category : #'as yet unclassified' }
AWFYNBodySystem >> createBodies [

    | px py pz bodies |

    bodies := Array new: 5.
    bodies at: 1 put: AWFYBody sun.
    bodies at: 2 put: AWFYBody jupiter.
    bodies at: 3 put: AWFYBody saturn.
    bodies at: 4 put: AWFYBody uranus.
    bodies at: 5 put: AWFYBody neptune.
    
    "bodies do: [:b | b print. '' println ]."

    px := 0.0.  py := 0.0.  pz := 0.0.

    bodies do: [:b |
        px := px + (b vx * b mass).
        py := py + (b vy * b mass).
        pz := pz + (b vz * b mass).
    ].
    
    (bodies at: 1) offsetMomentumX: px y: py z: pz.
    
    "bodies do: [:b | b print. '' println ]."
    ^ bodies

]

{ #category : #'as yet unclassified' }
AWFYNBodySystem >> energy [

    | dx dy dz distance e |
    e := 0.0.

    1 to: bodies size do: [:i |
      | iBody |
      iBody := bodies at: i.
  
      e := e + (0.5 * iBody mass *
           ((iBody vx * iBody vx) +
            (iBody vy * iBody vy) +
            (iBody vz * iBody vz))).
  
      i + 1 to: bodies size do: [:j |
          | jBody |
          jBody := bodies at: j.
      
          dx := iBody x - jBody x.
          dy := iBody y - jBody y.
          dz := iBody z - jBody z.

          distance := ((dx*dx) + (dy*dy) + (dz*dz)) sqrt.
          e := e - ((iBody mass * jBody mass) / distance).
      ].
    ].
    ^ e

]

{ #category : #'as yet unclassified' }
AWFYNBodySystem >> initialize [

    bodies := self createBodies

]
