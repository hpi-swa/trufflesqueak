fields
fieldMetaObject

	^ (self newFieldForType: #misc key: #metaObject)
		name: 'meta object'; emphasizeName;
		valueGetter: [:object | Interop getMetaObject: object ];
		valueGetterExpression: 'Interop getMetaObject: self';
		yourself