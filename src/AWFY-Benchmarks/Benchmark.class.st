Class {
	#name : #Benchmark,
	#superclass : #Object,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Benchmark >> benchmark [
 self subclassResponsibility 
]

{ #category : #'as yet unclassified' }
Benchmark >> innerBenchmarkLoop: innerIterations [

      1 to: innerIterations do: [:i |
        (self verifyResult: self benchmark) ifFalse: [ ^ false ].
      ].
      ^ true

]

{ #category : #'as yet unclassified' }
Benchmark >> verifyResult: result [
 self subclassResponsibility 
]
