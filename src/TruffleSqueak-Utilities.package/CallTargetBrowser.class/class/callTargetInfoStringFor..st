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
profiled arguments:		{7}
included call nodes:
	- {8}' format: {
	c getName asString.
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
			ifNotNil: [ :t | ((t asCollection allButFirst: 3) collect: [ :ea | ea
				ifNil: ['n/a']
				ifNotNil: [ ea getSimpleName asString ]]) joinSeparatedBy: ', ' ] ].
	(c getCallNodes asCollection collect: [ :ea |
		ea getCurrentCallTarget toString asString])
			joinSeparatedBy: String cr, '	- ' } ]
