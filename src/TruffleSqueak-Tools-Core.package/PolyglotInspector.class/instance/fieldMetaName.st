fields
fieldMetaName

	^ (self newFieldForType: #misc key: #metaObject)
		name: 'meta name'; emphasizeName;
		valueGetter: [:object | (Interop getMetaQualifiedName: object) asString ]; printValueAsIs;
		valueGetterExpression: 'Interop getMetaQualifiedName: self';
		yourself