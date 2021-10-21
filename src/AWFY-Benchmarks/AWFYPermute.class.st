Class {
	#name : #AWFYPermute,
	#superclass : #AWFYBenchmark,
	#instVars : [
		'count',
		'v'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYPermute >> benchmark [

    count := 0.
    v     := Array new: 6 withAll: 0.
    self permute: 6.
    ^ count

]

{ #category : #'as yet unclassified' }
AWFYPermute >> permute: n [

    count := count + 1.
    (n ~= 0)
      ifTrue: [
        self permute: n - 1.
        n to: 1 by: -1 do: [ :i |
          self swap: n with: i.
          self permute: n - 1.
          self swap: n with: i ] ]

]

{ #category : #'as yet unclassified' }
AWFYPermute >> swap: i with: j [

    | tmp |
    tmp := v at: i.
    v at: i put: (v at: j).
    v at: j put: tmp

]

{ #category : #'as yet unclassified' }
AWFYPermute >> verifyResult: result [

    ^ 8660 = result

]
