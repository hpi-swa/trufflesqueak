Class {
	#name : #AWFYVector,
	#superclass : #Object,
	#instVars : [
		'first',
		'last',
		'storage'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYVector class >> new [
 ^ self new: 50 
]

{ #category : #'as yet unclassified' }
AWFYVector class >> new: initialSize [
 ^ super new initialize: initialSize 
]

{ #category : #'as yet unclassified' }
AWFYVector class >> with: elem [

    | newVector |
    newVector := self new: 1.
    newVector append: elem.
    ^ newVector

]

{ #category : #'as yet unclassified' }
AWFYVector >> append: element [

    (last > storage size) ifTrue: [
      "Need to expand capacity first"
      | newStorage |
      newStorage := Array new: (2 * storage size).
      1 to: storage size do: [:i | newStorage at: i put: (storage at: i) ].
      storage := newStorage. ].
    
    storage at: last put: element.
    last := last + 1.
    ^ self

]

{ #category : #'as yet unclassified' }
AWFYVector >> at: index [

    index > storage size ifTrue: [ ^ nil ].
    ^ storage at: index

]

{ #category : #'as yet unclassified' }
AWFYVector >> at: index put: val [

    index > storage size ifTrue: [
      | newLength newStorage |
      newLength := storage size.
      [ newLength < index ] whileTrue: [
        newLength := newLength * 2 ].
      newStorage := Array new: newLength.
      1 to: storage size do: [:i | newStorage at: i put: (storage at: i) ].
      storage := newStorage ].

    storage at: index put: val.
    last < (index + 1) ifTrue: [
      last := index + 1 ]

]

{ #category : #'as yet unclassified' }
AWFYVector >> capacity [
 ^ storage size 
]

{ #category : #'as yet unclassified' }
AWFYVector >> forEach: block [

    first to: last - 1 do: [ :i | block value: (storage at: i) ]

]

{ #category : #'as yet unclassified' }
AWFYVector >> getOne: block [

    first to: last - 1 do: [ :i |
      | e |
      e := storage at: i.
      (block value: e)
        ifTrue: [ ^ e ] ].
    ^ nil

]

{ #category : #'as yet unclassified' }
AWFYVector >> hasSome: block [

    first to: last - 1 do: [ :i |
      (block value: (storage at: i))
        ifTrue: [ ^ true ] ].
    ^ false

]

{ #category : #'as yet unclassified' }
AWFYVector >> initialize: size [

    first := 1.
    last  := 1.
    storage := Array new: size.

]

{ #category : #'as yet unclassified' }
AWFYVector >> isEmpty [
 ^ last = first 
]

{ #category : #'as yet unclassified' }
AWFYVector >> remove: object [

    | newArray newLast found |
    newArray := Array new: self capacity.
    newLast := 1.
    found := false.
   
    self forEach: [ :it |
      it == object
          ifTrue:  [ found := true ]
          ifFalse: [
              newArray at: newLast put: it.
              newLast := newLast + 1 ] ].

    storage := newArray.
    last := newLast.
    first := 1.
    ^ found

]

{ #category : #'as yet unclassified' }
AWFYVector >> removeAll [

    first := 1.
    last  := 1.
    storage  := Array new: storage size

]

{ #category : #'as yet unclassified' }
AWFYVector >> removeFirst [

    self isEmpty ifTrue: [ ^ nil ].
    first := first + 1.
    ^ storage at: first - 1

]

{ #category : #'as yet unclassified' }
AWFYVector >> size [
 ^ last - first 
]

{ #category : #'as yet unclassified' }
AWFYVector >> sort: aBlock [

    " Make the argument, aBlock, be the criterion for ordering elements of
       the receiver.
       sortBlocks with side effects may not work right "
    self size > 0 ifTrue: [
      self sort: first
             to: last - 1
           with: aBlock ]

]

{ #category : #'as yet unclassified' }
AWFYVector >> sort: i to: j with: sortBlock [

    " Sort elements i through j of self to be non-descending according to
       sortBlock. "
    | di dij dj tt ij k l n |
    sortBlock isNil ifTrue: [ ^ self defaultSort: i to: j ].
    
    "The prefix d means the data at that index."
    (n := j + 1  - i) <= 1 ifTrue: [ ^ self ]. "Nothing to sort."
    " Sort di,dj. "
    di := storage at: i.
    dj := storage at: j.
    
    "i.e., should di precede dj?"
    (sortBlock value: di with: dj) ifFalse: [
      storage swap: i with: j.
      tt := di.
      di := dj.
      dj := tt
    ].

    n > 2 ifTrue: [ " More than two elements. "
      ij := (i + j) // 2.  " ij is the midpoint of i and j. "
      dij := storage at: ij.  " Sort di,dij,dj.  Make dij be their median. "
      (sortBlock value: di with: dij)
        ifTrue: [ " i.e. should di precede dij? "
          (sortBlock value: dij with: dj) ifFalse: [ " i.e., should dij precede dj? "
            storage swap: j with: ij.
            dij := dj]]
        ifFalse: [ " i.e. di should come after dij "
          storage swap: i with: ij.
          dij := di].
      
      n > 3 ifTrue: [
        " More than three elements.
          Find k>i and l<j such that dk,dij,dl are in reverse order.
          Swap k and l.  Repeat this procedure until k and l pass each other. "
        k := i.
        l := j.
        [ [l := l - 1.  k <= l and: [sortBlock value: dij with: (storage at: l)]]
            whileTrue.  " i.e. while dl succeeds dij "
          [k := k + 1.  k <= l and: [sortBlock value: (storage at: k) with: dij]]
            whileTrue.  " i.e. while dij succeeds dk "
          k <= l]
            whileTrue:
              [ storage swap: k with: l ].

        " Now l<k (either 1 or 2 less), and di through dl are all less than or equal to dk
          through dj.  Sort those two segments. "
        self sort: i to: l with: sortBlock.
        self sort: k to: j with: sortBlock
      ]
    ]

]
