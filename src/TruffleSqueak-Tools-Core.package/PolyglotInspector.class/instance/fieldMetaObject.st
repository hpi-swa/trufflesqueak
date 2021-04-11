fields
fieldMetaObject

	^ (self newFieldForType: #misc key: #metaObject)
		name: 'meta object'; emphasizeName;
		valueGetter: [ :o | Interop getMetaObject: o ];
		valueGetterExpression: 'Interop getMetaObject: self';
		yourself