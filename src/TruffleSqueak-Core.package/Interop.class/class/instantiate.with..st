primitives instantiables
instantiate: anObject with: arguments
	<primitive: 'primitiveInstantiate' module: 'PolyglotPlugin'>
	^ (self isInstantiable: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopInstantiate: arguments ] ]
		ifFalse: [ self error: 'Object is not instantiable' ]