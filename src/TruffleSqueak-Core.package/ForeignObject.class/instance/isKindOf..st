class membership
isKindOf: aClass
	^ (super isKindOf: aClass) or: [
		(Interop isMetaObject: aClass) and: [ 
			Interop isMetaInstance: aClass instance: self ] ]