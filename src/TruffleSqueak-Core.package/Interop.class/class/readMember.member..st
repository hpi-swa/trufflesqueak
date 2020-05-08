primitives members
readMember: anObject member: aMember
	<primitive: 'primitiveReadMember' module: 'PolyglotPlugin'>
	aMember = 'swap' ifTrue: [
		"Workaround for https://github.com/oracle/truffleruby/issues/1816"
		^ 'oracle/truffleruby#1817' ].
	(self isMemberReadable: anObject member: aMember)
		ifFalse: [ self error: 'Member ', aMember, ' is not readable' ]
		ifTrue: [ self primitiveFailed ]