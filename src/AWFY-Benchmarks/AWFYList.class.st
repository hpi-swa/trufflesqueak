Class {
	#name : #AWFYList,
	#superclass : #AWFYBenchmark,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYList >> benchmark [
 | result |
    result := self
        talkWithX: (self makeList: 15)
        withY:     (self makeList: 10)
        withZ:     (self makeList: 6).
    ^ result length

]

{ #category : #'as yet unclassified' }
AWFYList >> isShorter: x than: y [

    | xTail yTail |
    
    xTail := x. yTail := y.
    [ yTail isNil ]
      whileFalse: [
        xTail isNil ifTrue: [ ^ true ].
        xTail := xTail next.
        yTail := yTail next ].

    ^ false

]

{ #category : #'as yet unclassified' }
AWFYList >> makeList: length [

    (length = 0)
      ifTrue: [ ^ nil ]
      ifFalse: [
        | e |
        e := AWFYListElement new: length.
        e next: (self makeList: (length - 1)).
        ^ e ]

]

{ #category : #'as yet unclassified' }
AWFYList >> talkWithX: x withY: y withZ: z [

    (self isShorter: y than: x)
      ifTrue: [
        ^ (self
            talkWithX: (self talkWithX: x next withY: y withZ: z)
            withY: (self talkWithX: y next withY: z withZ: x)
            withZ: (self talkWithX: z next withY: x withZ: y)) ]
      ifFalse: [ ^ z ].

]

{ #category : #'as yet unclassified' }
AWFYList >> verifyResult: result [

    ^ 10 = result

]
