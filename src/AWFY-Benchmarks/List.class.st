Class {
	#name : #List,
	#superclass : #Benchmark,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
List >> benchmark [
 | result |
    result := self
        talkWithX: (self makeList: 15)
        withY:     (self makeList: 10)
        withZ:     (self makeList: 6).
    ^ result length

]

{ #category : #'as yet unclassified' }
List >> isShorter: x than: y [

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
List >> makeList: length [

    (length = 0)
      ifTrue: [ ^ nil ]
      ifFalse: [
        | e |
        e := ListElement new: length.
        e next: (self makeList: (length - 1)).
        ^ e ]

]

{ #category : #'as yet unclassified' }
List >> talkWithX: x withY: y withZ: z [

    (self isShorter: y than: x)
      ifTrue: [
        ^ (self
            talkWithX: (self talkWithX: x next withY: y withZ: z)
            withY: (self talkWithX: y next withY: z withZ: x)
            withZ: (self talkWithX: z next withY: x withZ: y)) ]
      ifFalse: [ ^ z ].

]

{ #category : #'as yet unclassified' }
List >> verifyResult: result [

    ^ 10 = result

]
