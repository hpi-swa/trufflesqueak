helpers
callTargetInfoStringFor: aCompiledMethod
	^ aCompiledMethod callTarget
		ifNil: [ 'no call target available' ]
		ifNotNil: [ :c |
'name:					{1}
callCount:				{2}
callAndLoopCount:		{3}
knownCallSiteCount:	{4}
nonTrivialNodeCount:	{5}
profiled return value:	{6}
profiled arguments:		{7}' format: {
	c getName asString.
	c getCallCount.
	c getCallAndLoopCount.
	c getKnownCallSiteCount.
	c getNonTrivialNodeCount.
	c returnProfile
		ifNil: [ 'unknown' ]
		ifNotNil: [ :p | p getType ifNil: [ 'unknown' ] ifNotNil: [ :e | e getSimpleName asString ]].
	c argumentsProfile
		ifNil: [ 'all unknown' ]
		ifNotNil: [ :p | p getTypes
			ifNil: [ 'all unknown' ]
			ifNotNil: [ :t | ((t asCollection allButFirst: 3) collect: [ :ea | ea
				ifNil: ['unknown']
				ifNotNil: [ ea getSimpleName asString ]]) joinSeparatedBy: ', ' ] ] } ]
