fields
fieldMetaObjectInfo

	^ (self newFieldForType: #misc key: #metaObjectInfo)
		name: 'meta object info'; emphasizeName;
		valueGetter: [ :o | | metaObject |
			metaObject := Interop getMetaObject: o.
			Text streamContents: [ :aStream |
				aStream
					nextPutAll: 'Meta object:	';
					withAttribute: (PluggableTextAttribute evalBlock: [ metaObject inspect ])
						do: [ aStream nextPutAll: metaObject asString ]; cr;
					nextPutAll: 'Simple name:	';
					nextPutAll: (Interop getMetaSimpleName: metaObject) asString; cr;
					nextPutAll: 'Qualified name:	';
					nextPutAll: (Interop getMetaQualifiedName: metaObject) asString ]]; printValueAsIs;
		yourself