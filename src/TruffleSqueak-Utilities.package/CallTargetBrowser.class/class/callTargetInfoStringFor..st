helpers
callTargetInfoStringFor: aCallTarget
	^ aCallTarget
		ifNil: [ 'no call target available' ]
		ifNotNil: [ :c |
'name:					{1}
highestCompiledTier:	{2}
callCount:				{3}
callAndLoopCount:		{4}
knownCallSiteCount:	{5}
nonTrivialNodeCount:	{6}
profiled return value:	{7}
profiled arguments:		{8}
included call nodes:
	- {9}' format: {
	c getName asString.
	c highestCompiledTier.
	c getCallCount.
	c getCallAndLoopCount.
	c getKnownCallSiteCount.
	c getNonTrivialNodeCount.
	c returnProfile
		ifNil: [ 'n/a' ]
		ifNotNil: [ :p | p getType ifNil: [ 'n/a' ] ifNotNil: [ :e | e getSimpleName asString ]].
	c argumentsProfile
		ifNil: [ 'all n/a' ]
		ifNotNil: [ :p | p getTypes
			ifNil: [ 'all n/a' ]
			ifNotNil: [ :t | (t asCollection collect: [ :ea | ea
				ifNil: ['n/a']
				ifNotNil: [ ea getSimpleName asString ]]) joinSeparatedBy: ', ' ] ].
	(c getCallNodes asCollection
		ifNotEmpty: [:n | (n collect: [ :ea |
				'{1} (callCount: {2}; inliningForced: {3}; identity: {4})' format: {
					ea getCurrentCallTarget toString asString.
					ea getCallCount.
					ea isInliningForced.
					ea identityHash } ])
				joinSeparatedBy: String cr, '	- ' ]
		ifEmpty: [ 'n/a' ] ) } ]
