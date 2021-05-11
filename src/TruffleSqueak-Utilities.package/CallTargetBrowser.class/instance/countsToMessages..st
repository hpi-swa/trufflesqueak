helpers
countsToMessages: aMessageList
	| result |
	result := Dictionary new.
	aMessageList do: [ :ea | | callTarget |
		callTarget := (self selectedClassOrMetaClass >> ea asSymbol) callTarget.
		result
			at: (callTarget ifNil: [-1] ifNotNil: [ :c |
				self sortBy caseOf: {
					[#callCount] -> [ | v |
						v := c getCallCount.
						v > 0 ifTrue: [ v ln ] ifFalse: [ v ] ].
					[#callAndLoopCount] -> [ | v |
						v := c getCallAndLoopCount.
						v > 0 ifTrue: [ v ln ] ifFalse: [ v ] ].
					[#knownCallSiteCount] -> [ c getKnownCallSiteCount ].
					[#nonTrivialNodeCount] -> [ c getNonTrivialNodeCount ] } ] )
			ifPresent: [ :v | v add: ea ]
			ifAbsentPut: [ OrderedCollection with: ea ] ].
	^ result