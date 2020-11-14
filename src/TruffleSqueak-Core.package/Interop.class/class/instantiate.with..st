primitives instantiables
instantiate: anObject with: anArray
	<primitive: 'primitiveInstantiate' module: 'PolyglotPlugin'>
	(self isInstantiable: anObject)
		ifFalse: [ self error: 'Object is not instantiable' ]
		ifTrue: [ Polyglot signalPolyglotError ]