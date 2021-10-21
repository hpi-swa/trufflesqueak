Class {
	#name : #Vector3D,
	#superclass : #Object,
	#instVars : [
		'x',
		'y',
		'z'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Vector3D class >> x: x y: y z: z [

    ^ self new initX: x y: y z: z

]

{ #category : #'as yet unclassified' }
Vector3D >> dot: other [

    ^ (x * other x) + (y * other y) + (z * other z)

]

{ #category : #'as yet unclassified' }
Vector3D >> initX: anX y: aY z: aZ [

    x := anX.
    y := aY.
    z := aZ

]

{ #category : #'as yet unclassified' }
Vector3D >> magnitude [

    ^ self squaredMagnitude sqrt

]

{ #category : #'as yet unclassified' }
Vector3D >> minus: other [

    ^ Vector3D x: x - other x
               y: y - other y
               z: z - other z

]

{ #category : #'as yet unclassified' }
Vector3D >> plus: other [

    ^ Vector3D x: x + other x
               y: y + other y
               z: z + other z

]

{ #category : #'as yet unclassified' }
Vector3D >> squaredMagnitude [

    ^ self dot: self

]

{ #category : #'as yet unclassified' }
Vector3D >> times: amount [

    ^ Vector3D x: x * amount
               y: y * amount
               z: z * amount

]

{ #category : #'as yet unclassified' }
Vector3D >> x [
 ^ x 
]

{ #category : #'as yet unclassified' }
Vector3D >> y [
 ^ y 
]

{ #category : #'as yet unclassified' }
Vector3D >> z [
 ^ z 
]
