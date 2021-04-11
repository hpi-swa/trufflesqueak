fields
fieldMetaObjectInfo

	^ (self newFieldForType: #misc key: #metaObjectInfo)
		name: 'meta object info'; emphasizeName;
		valueGetter: [ :o | | metaObject |
			metaObject := Interop getMetaObject: o.
			 'Simple name:	{1}
Qualified name:	{2}' format: {
				(Interop getMetaSimpleName: metaObject) asString.
				(Interop getMetaQualifiedName: metaObject) asString} ]; printValueAsIs;
		yourself