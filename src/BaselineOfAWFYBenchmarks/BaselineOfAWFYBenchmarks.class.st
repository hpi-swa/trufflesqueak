Class {
	#name : #BaselineOfAWFYBenchmarks,
	#superclass : #BaselineOf,
	#category : #BaselineOfAWFYBenchmarks
}

{ #category : #baseline }
BaselineOfAWFYBenchmarks >> baseline: spec [
    <baseline>
    spec
        for: #'common'
        do: [ 
            spec
                package: 'AWFY-Benchmarks'. 
            spec
                group: 'default' with: #('AWFY-Benchmarks') ]
]
