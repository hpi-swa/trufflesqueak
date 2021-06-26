class membership
isKindOf: aClass
	^ (super isKindOf: aClass) or: [
		aClass isForeignObject and: [
			(Interop isMetaObject: aClass) and: [ 
				Interop isMetaInstance: aClass instance: self ] ] ]