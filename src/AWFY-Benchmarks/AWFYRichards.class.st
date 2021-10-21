Class {
	#name : #AWFYRichards,
	#superclass : #AWFYBenchmark,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYRichards >> benchmark [

    ^ AWFYScheduler new start.

]

{ #category : #'as yet unclassified' }
AWFYRichards >> verifyResult: result [

    ^ result

]
