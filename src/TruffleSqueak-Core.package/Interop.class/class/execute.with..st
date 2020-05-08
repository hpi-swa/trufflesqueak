primitives
execute: anObject with: arguments
	<primitive: 'primitiveExecute' module: 'PolyglotPlugin'>
	(self isExecutable: anObject)
		ifFalse: [ self error: 'Object is not executable' ]
		ifTrue: [ Polyglot signalPolyglotError ]