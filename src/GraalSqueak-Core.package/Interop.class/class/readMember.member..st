primitives
readMember: anObject member: aMember
	<primitive: 'primitiveReadMember' module: 'PolyglotPlugin'>
	aMember = 'swap' ifTrue: [
		"Workaround for https://github.com/oracle/truffleruby/issues/1816"
		^ 'oracle/truffleruby#1817' ].
	Polyglot signalPolyglotError