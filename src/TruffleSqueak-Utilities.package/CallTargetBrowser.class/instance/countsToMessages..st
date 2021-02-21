helpers
countsToMessages: aMessageList
	| result |
	result := Dictionary new.
	aMessageList do: [ :ea | | callTarget |
		callTarget := (self selectedClassOrMetaClass >> ea asSymbol) callTarget.
		result
			at: (callTarget ifNil: [-1] ifNotNil: [ :c |
				self sortBy caseOf: {
					[#callCount] -> [ c getCallCount ln ].
					[#callAndLoopCount] -> [ c getCallAndLoopCount ln ].
					[#knownCallSiteCount] -> [ c getKnownCallSiteCount ].
					[#nonTrivialNodeCount] -> [ c getNonTrivialNodeCount ] } ] )
			ifPresent: [ :v | v add: ea ]
			ifAbsentPut: [ OrderedCollection with: ea ] ].
	^ result