Class {
	#name : #AWFYSomDictionary,
	#superclass : #Object,
	#instVars : [
		'buckets',
		'size_'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYSomDictionary class >> new [

    ^ self new: 16

]

{ #category : #'as yet unclassified' }
AWFYSomDictionary class >> new: size [

    ^ super new initialize: size

]

{ #category : #'as yet unclassified' }
AWFYSomDictionary >> at: aKey [

    | hash e |
    hash := self hash: aKey.
    e    := self bucket: hash.
  
    [ e notNil ] whileTrue: [
      (e match: hash key: aKey)
        ifTrue: [ ^ e value ].
      e := e next ].
    ^ nil

]

{ #category : #'as yet unclassified' }
AWFYSomDictionary >> at: aKey put: aVal [

    | hash i current |
    hash := self hash: aKey.
    i    := self bucketIdx: hash.
    current := buckets at: i.
    
    current isNil
      ifTrue: [
        buckets at: i put: (self newEntry: aKey value: aVal hash: hash).
        size_ := size_ + 1 ]
      ifFalse: [
        self insertBucketEntry: aKey value: aVal hash: hash head: current ].

    size_ > buckets size ifTrue: [ self resize ]

]

{ #category : #'as yet unclassified' }
AWFYSomDictionary >> bucket: hash [

    ^ buckets at: (self bucketIdx: hash)

]

{ #category : #'as yet unclassified' }
AWFYSomDictionary >> bucketIdx: hash [

    ^ 1 + ((buckets size - 1) bitAnd: hash)

]

{ #category : #'as yet unclassified' }
AWFYSomDictionary >> containsKey: aKey [

    | hash e |
    hash := self hash: aKey.
    e    := self bucket: hash.
  
    [ e notNil ] whileTrue: [
      (e match: hash key: aKey)
        ifTrue: [ ^ true ].
      e := e next ].
    ^ false

]

{ #category : #'as yet unclassified' }
AWFYSomDictionary >> hash: key [

    | hash |
    key isNil ifTrue: [ ^ 0 ].
    hash := key customHash.
    ^ hash bitXor: (hash >> 16)

]

{ #category : #'as yet unclassified' }
AWFYSomDictionary >> initialize: size [

    buckets := Array new: size.
    size_   := 0

]

{ #category : #'as yet unclassified' }
AWFYSomDictionary >> insertBucketEntry: key value: value hash: hash head: head [

    | current |
    current := head.
    
    [true] whileTrue: [
      (current match: hash key: key) ifTrue: [
        current value: value.
        ^ self ].
      current next isNil ifTrue: [
        size_ := size_ + 1.
        current next: (self newEntry: key value: value hash: hash).
        ^ self ].
      current := current next ]

]

{ #category : #'as yet unclassified' }
AWFYSomDictionary >> isEmpty [
 ^ size_ = 0 
]

{ #category : #'as yet unclassified' }
AWFYSomDictionary >> keys [

    | keys |
    keys := AWFYVector new: size_.
    buckets do: [:b |
      | current |
      current := b.
      [ current notNil ] whileTrue: [
        keys append: current key.
        current := current next ] ].
    ^ keys

]

{ #category : #'as yet unclassified' }
AWFYSomDictionary >> newEntry: aKey value: value hash: hash [

    ^ AWFYDictEntry new: hash key: aKey value: value next: nil

]

{ #category : #'as yet unclassified' }
AWFYSomDictionary >> removeAll [

    buckets := Array new: buckets size.
    size_ := 0.

]

{ #category : #'as yet unclassified' }
AWFYSomDictionary >> resize [

    | oldStorage |
    oldStorage := buckets.
    buckets := Array new: oldStorage size * 2.
    self transferEntries: oldStorage

]

{ #category : #'as yet unclassified' }
AWFYSomDictionary >> size [
 ^ size_ 
]

{ #category : #'as yet unclassified' }
AWFYSomDictionary >> splitBucket: oldStorage bucket: i head: head [

    | loHead loTail hiHead hiTail current |
    loHead := nil. loTail := nil.
    hiHead := nil. hiTail := nil.
    current := head.
    
    [current notNil] whileTrue: [
      (current hash bitAnd: oldStorage size) = 0
        ifTrue: [
          loTail isNil
            ifTrue:  [ loHead := current ]
            ifFalse: [ loTail next: current ].
          loTail := current ]
        ifFalse: [
          hiTail isNil
            ifTrue:  [ hiHead := current ]
            ifFalse: [ hiTail next: current ].
          hiTail := current ].
      current := current next ].
  
    loTail notNil ifTrue: [
      loTail next: nil.
      buckets at: i put: loHead ].
    hiTail notNil ifTrue: [
      hiTail next: nil.
      buckets at: i + oldStorage size put: hiHead ]

]

{ #category : #'as yet unclassified' }
AWFYSomDictionary >> transferEntries: oldStorage [

    1 to: oldStorage size do: [:i |
      | current |
      current := oldStorage at: i.
      current notNil ifTrue: [
        oldStorage at: i put: nil.
        current next isNil
          ifTrue: [
            buckets at: 1 + (current hash bitAnd: (buckets size - 1)) put: current ]
          ifFalse: [
            self splitBucket: oldStorage bucket: i head: current ] ] ]

]

{ #category : #'as yet unclassified' }
AWFYSomDictionary >> values [

    | values |
    values := AWFYVector new: size_.
    buckets do: [:b |
      | current |
      current := b.
      [ current notNil ] whileTrue: [
        values append: current value.
        current := current next ] ].
    ^ values

]
