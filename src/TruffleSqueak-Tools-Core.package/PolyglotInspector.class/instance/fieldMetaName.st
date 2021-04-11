fields
fieldMetaName

	^ (self newFieldForType: #misc key: #metaObject)
		name: 'meta name'; emphasizeName;
		valueGetter: [ :o | (Interop getMetaQualifiedName: o) asString ]; printValueAsIs;
		valueGetterExpression: 'Interop getMetaQualifiedName: self';
		yourself