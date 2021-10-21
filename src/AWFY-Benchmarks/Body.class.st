Class {
	#name : #Body,
	#superclass : #Object,
	#instVars : [
		'x',
		'y',
		'z',
		'vx',
		'vy',
		'vz',
		'mass'
	],
	#classVars : [
		'solarMass'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Body class >> DaysPerYear [
 ^ 365.24 
]

{ #category : #'as yet unclassified' }
Body class >> Pi [
 ^ 3.141592653589793 
]

{ #category : #'as yet unclassified' }
Body class >> SolarMass [
 ^ solarMass 
]

{ #category : #'as yet unclassified' }
Body class >> initialize [

    solarMass := 4 * self Pi * self Pi.

]

{ #category : #'as yet unclassified' }
Body class >> jupiter [

     ^ super new
         initX:    4.8414314424647209
             y:   -1.16032004402742839
             z:   -0.103622044471123109
             vx:   0.00166007664274403694
             vy:   0.00769901118419740425
             vz:  -0.0000690460016972063023
             mass: 0.000954791938424326609

]

{ #category : #'as yet unclassified' }
Body class >> neptune [

    ^ super new
        initX:   15.3796971148509165
            y:  -25.9193146099879641
            z:    0.179258772950371181
            vx:   0.00268067772490389322
            vy:   0.00162824170038242295
            vz:  -0.000095159225451971587
            mass: 0.0000515138902046611451

]

{ #category : #'as yet unclassified' }
Body class >> saturn [

    ^ super new
        initX:    8.34336671824457987
            y:    4.12479856412430479
            z:   -0.403523417114321381
            vx:  -0.00276742510726862411
            vy:   0.00499852801234917238
            vz:   0.0000230417297573763929
            mass: 0.000285885980666130812

]

{ #category : #'as yet unclassified' }
Body class >> sun [

    ^ super new initX: 0.0 y: 0.0 z: 0.0 vx: 0.0 vy: 0.0 vz: 0.0 mass: 1.0

]

{ #category : #'as yet unclassified' }
Body class >> uranus [

    ^ super new
        initX:   12.894369562139131
            y:  -15.1111514016986312
            z:   -0.223307578892655734
            vx:   0.00296460137564761618
            vy:   0.0023784717395948095
            vz:  -0.0000296589568540237556
            mass: 0.0000436624404335156298

]

{ #category : #'as yet unclassified' }
Body >> initX: anX y: aY z: aZ vx: aVX vy: aVY vz: aVZ mass: aMass [

    x := anX.
    y := aY.
    z := aZ.
    vx := aVX * Body DaysPerYear.
    vy := aVY * Body DaysPerYear.
    vz := aVZ * Body DaysPerYear.
    mass := aMass * Body SolarMass.

]

{ #category : #'as yet unclassified' }
Body >> mass [
 ^ mass 
]

{ #category : #'as yet unclassified' }
Body >> mass: val [
 mass := val 
]

{ #category : #'as yet unclassified' }
Body >> offsetMomentumX: px y: py z: pz [

    vx := 0.0 - (px / Body SolarMass).
    vy := 0.0 - (py / Body SolarMass).
    vz := 0.0 - (pz / Body SolarMass).

]

{ #category : #'as yet unclassified' }
Body >> print [

    'x:  ' print. x println.
    'y:  ' print. y println.
    'z:  ' print. z println.
    
    'vx: ' print. vx println.
    'vy: ' print. vy println.
    'vz: ' print. vz println.
    
    'mass: ' print. mass println.

]

{ #category : #'as yet unclassified' }
Body >> vx [
 ^ vx 
]

{ #category : #'as yet unclassified' }
Body >> vx: val [
 vx := val 
]

{ #category : #'as yet unclassified' }
Body >> vy [
 ^ vy 
]

{ #category : #'as yet unclassified' }
Body >> vy: val [
 vy := val 
]

{ #category : #'as yet unclassified' }
Body >> vz [
 ^ vz 
]

{ #category : #'as yet unclassified' }
Body >> vz: val [
 vz := val 
]

{ #category : #'as yet unclassified' }
Body >> x [
 ^ x 
]

{ #category : #'as yet unclassified' }
Body >> x: val [
 x := val 
]

{ #category : #'as yet unclassified' }
Body >> y [
 ^ y 
]

{ #category : #'as yet unclassified' }
Body >> y: val [
 y := val 
]

{ #category : #'as yet unclassified' }
Body >> z [
 ^ z 
]

{ #category : #'as yet unclassified' }
Body >> z: val [
 z := val 
]
