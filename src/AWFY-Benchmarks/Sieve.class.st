Class {
	#name : #Sieve,
	#superclass : #Benchmark,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Sieve >> benchmark [

    | flags |
    flags  := Array new: 5000 withAll: true.
    ^ self sieve: flags size: 5000.

]

{ #category : #'as yet unclassified' }
Sieve >> sieve: flags size: size [

    | primeCount |
    primeCount := 0.

    2 to: size do: [ :i |
      (flags at: i - 1)
        ifTrue: [
          | k |
          primeCount := primeCount + 1.
          k := i + i.
          [ k <= size ]
            whileTrue: [
              flags at: k - 1 put: false.
              k := k + i ]. ] ].
    ^ primeCount

]

{ #category : #'as yet unclassified' }
Sieve >> verifyResult: result [

    ^ 669 = result

]
