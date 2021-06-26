primitives executables
execute: anObject with: arguments
	<primitive: 'primitiveExecute' module: 'PolyglotPlugin'>
	^ (self isExecutable: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopExecute: arguments ] ]
		ifFalse: [ self error: 'Object is not executable' ]