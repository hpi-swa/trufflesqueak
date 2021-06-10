primitives instantiables
instantiate: anObject with: arguments
	<primitive: 'primitiveInstantiate' module: 'PolyglotPlugin'>
	^ (self isInstantiable: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ Polyglot signalPolyglotError ]
			ifFalse: [ anObject interopInstantiate: arguments ] ]
		ifFalse: [ self error: 'Object is not instantiable' ]