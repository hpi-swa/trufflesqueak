Class {
	#name : #AWFYBenchmark,
	#superclass : #Object,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYBenchmark >> benchmark [
 self subclassResponsibility 
]

{ #category : #'as yet unclassified' }
AWFYBenchmark >> innerBenchmarkLoop: innerIterations [

      1 to: innerIterations do: [:i |
        (self verifyResult: self benchmark) ifFalse: [ ^ false ].
      ].
      ^ true

]

{ #category : #'as yet unclassified' }
AWFYBenchmark >> verifyResult: result [
 self subclassResponsibility 
]
