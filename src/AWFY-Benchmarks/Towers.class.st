Class {
	#name : #Towers,
	#superclass : #Benchmark,
	#instVars : [
		'piles',
		'movesdone'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Towers class >> new [
 ^ super new initialize 
]

{ #category : #'as yet unclassified' }
Towers >> benchmark [

    piles := Array new: 3.
    self buildTowerAt: 1 disks: 13.
    movesdone := 0.
    self move: 13 disksFrom: 1 to: 2.
    ^ movesdone

]

{ #category : #'as yet unclassified' }
Towers >> buildTowerAt: pile disks: disks [

    disks to: 0 by: -1 do: [ :i |
      self pushDisk: (TowersDisk new: i) onPile: pile ]

]

{ #category : #'as yet unclassified' }
Towers >> initialize [

    piles     := nil.
    movesdone := 0.

]

{ #category : #'as yet unclassified' }
Towers >> move: disks disksFrom: fromPile to: toPile [

    disks = 1
      ifTrue: [ self moveTopDiskFrom: fromPile to: toPile ]
      ifFalse: [ | otherPile |
        otherPile := 6 - fromPile - toPile.
        self move: disks - 1 disksFrom: fromPile to: otherPile.
        self moveTopDiskFrom: fromPile to: toPile.
        self move: disks - 1 disksFrom: otherPile to: toPile. ]

]

{ #category : #'as yet unclassified' }
Towers >> moveTopDiskFrom: fromPile to: toPile [

    self pushDisk: (self popDiskFrom: fromPile) onPile: toPile.
    movesdone := movesdone + 1.

]

{ #category : #'as yet unclassified' }
Towers >> popDiskFrom: pile [

    | top |
    
    top := piles at: pile.
    top isNil
      ifTrue: [
        self error: 'Attempting to remove a disk from an empty pile' ].
    
    piles at: pile put: top next.
    top next: nil.
    ^ top

]

{ #category : #'as yet unclassified' }
Towers >> pushDisk: disk onPile: pile [

    | top |
    
    top := piles at: pile.
    (top notNil and: [ disk size >= top size ])
      ifTrue: [ self error: 'Cannot put a big disk on a smaller one' ].
    
    disk next: top.
    piles at: pile put: disk.

]

{ #category : #'as yet unclassified' }
Towers >> verifyResult: result [

    ^ 8191 = result

]
