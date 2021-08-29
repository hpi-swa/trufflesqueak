Class {
	#name : #PolyglotInspector,
	#superclass : #Inspector,
	#instVars : [
		'languageInfo'
	],
	#category : #'TruffleSqueak-Tools-Core'
}

{ #category : #'code pane menu' }
PolyglotInspector >> addCustomCodePaneMenuItems: aMenu shifted: aBoolean [
	<codePaneMenu>
	<menuPriority: 400>
	
	^ aMenu
		add: 'select language... (L)'
			target: self
			selector: #selectLanguage;
		add: 'set adaptive language selection... (l)'
			target: self
			selector: #setAdaptiveLanguageSelection;
		add: 'set default language... (h)'
			target: self
			selector: #setDefaultLanguage;
		add: 'set syntax highlighter... (h)'
			target: self
			selector: #setSyntaxHighlighter
]

{ #category : #toolbuilder }
PolyglotInspector >> buildCodePaneWith: builder [
	^ (super buildCodePaneWith: builder)
		help: #helpText;
		stylerClass: PolyglotTextStyler;
		yourself
]

{ #category : #helpers }
PolyglotInspector >> exceptionInfoFor: anInteropException [
	^ 'Message:	{1}
Cause:		{2}
Type:		{3}

Stack trace:
- {4}' format: {
	(Interop hasExceptionMessage: anInteropException)
		ifFalse: [ 'n/a' ]
		ifTrue: [ Interop getExceptionMessage: anInteropException ].
	(Interop hasExceptionCause: anInteropException)
		ifFalse: [ 'n/a' ]
		ifTrue: [ Interop getExceptionCause: anInteropException ].
	Interop getExceptionType: anInteropException.
	(Interop hasExceptionStackTrace: anInteropException)
		ifFalse: [ 'n/a' ]
		ifTrue: [ ((Interop getExceptionStackTrace: anInteropException) asCollection
					collect: [ :ea | ((Interop hasExecutableName: ea)
									ifTrue: [ (Interop getExecutableName: ea) ]
									ifFalse: [ ea ]) asString ]) joinSeparatedBy: String cr, '- ' ] }
]

{ #category : #fields }
PolyglotInspector >> fieldException [

	^ (self newFieldForType: #misc key: #exception)
		name: 'exception info'; emphasizeName;
		valueGetter: [ :o | self exceptionInfoFor: o ]; printValueAsIs;
		yourself
]

{ #category : #fields }
PolyglotInspector >> fieldHashEntries [

	^ (self newFieldForType: #misc key: #hashes)
		name: 'hash entries'; emphasizeName;
		valueGetter: [ :o | self hashEntriesOf: o ]; printValueAsIs;
		yourself
]

{ #category : #fields }
PolyglotInspector >> fieldLanguageInfo [

	^ (self newFieldForType: #misc key: #language)
		name: 'language info'; emphasizeName;
		valueGetter: [ :o | self languageInfoFor: o ]; printValueAsIs;
		valueGetterExpression: 'Interop getLanguageInfo: self';
		yourself
]

{ #category : #fields }
PolyglotInspector >> fieldMetaName [

	^ (self newFieldForType: #misc key: #metaObject)
		name: 'meta name'; emphasizeName;
		valueGetter: [ :o | (Interop getMetaQualifiedName: o) asString ]; printValueAsIs;
		valueGetterExpression: 'Interop getMetaQualifiedName: self';
		yourself
]

{ #category : #fields }
PolyglotInspector >> fieldMetaObjectInfo [

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
]

{ #category : #fields }
PolyglotInspector >> fieldSource [

	^ (self newFieldForType: #misc key: #sourceLocation)
		name: 'source'; emphasizeName;
		valueGetter: [ :o | (Interop getSourceLocation: o) getCharacters asString ]; printValueAsIs;
		valueGetterExpression: 'Interop getSourceLocation: self';
		yourself
]

{ #category : #fields }
PolyglotInspector >> fieldsMembers [

	^ { (self newFieldForType: #misc key: #membersReadable)
			name: 'members (readable)'; emphasizeName;
			valueGetter: [:object | self membersReadableInfoFor: object ];
			printValueAsIs.
		(self newFieldForType: #misc key: #membersInvocable)
			name: 'members (invocable)'; emphasizeName;
			valueGetter: [:object | self membersInvocableInfoFor: object ];
			printValueAsIs. }

]

{ #category : #helpers }
PolyglotInspector >> hashEntriesOf: anObject [
	| size |
	^ (size := (Interop getHashSize: anObject)) < 100 ifTrue: [ | iterator |
		iterator := Interop getHashEntriesIterator: anObject.
		Text streamContents: [ :aStream |
			[ Interop hasIteratorNextElement: iterator ] whileTrue: [ | next key value |
				next := Interop getIteratorNextElement: iterator.
				key := next at: 1.
				value := next at: 2.
				aStream
					withAttributes: { PluggableTextAttribute evalBlock: [ key inspect ]. TextColor color: Color gray }
						do: [ aStream nextPutAll: key printString ];
					nextPutAll: ' -> ';
					withAttributes: { PluggableTextAttribute evalBlock: [ value inspect ]. TextEmphasis italic }
						do: [ aStream nextPutAll: '', value printString ];
					cr; cr
			] ]]
		ifFalse: [ 'Hash size: ', size, ' (too many entries to display)' ]

]

{ #category : #accessing }
PolyglotInspector >> helpText [
	^ 'Evaluate {1} expressions on inspected object'
		format: { self languageInfo getName }
]

{ #category : #initialization }
PolyglotInspector >> initialExtent [
	"Answer the desired extent for the receiver when it is first opened on the screen."

	^ 475 @ 300
]

{ #category : #'initialize-release' }
PolyglotInspector >> inspect: anObject [ 
	"Initialize the receiver so that it is inspecting anObject. There is no current selection."
	
	object := anObject. 
	self initialize
]

{ #category : #'menu commands' }
PolyglotInspector >> inspectAs: aLanguageInfo [
	(Polyglot getLanguageView: aLanguageInfo getId asString for: object) inspect
]

{ #category : #'menu commands' }
PolyglotInspector >> inspectVMObject [
	object vmObjectInspect
]

{ #category : #'user interface - window' }
PolyglotInspector >> labelString [
	^ self object defaultLabelForPolyglotTools
]

{ #category : #language }
PolyglotInspector >> languageId [
	^ self languageInfo getId asSymbol
]

{ #category : #language }
PolyglotInspector >> languageInfo [
	^ languageInfo ifNil: [
		languageInfo := (Polyglot useAdaptiveLanguageSelection and: [ Interop hasLanguage: self object ])
			ifTrue: [ (Interop getLanguageInfo: self object) ]
			ifFalse: [ Polyglot defaultLanguageInfo ] ]
]

{ #category : #language }
PolyglotInspector >> languageInfo: aLanguageInfo [
	languageInfo == aLanguageInfo ifTrue: [ ^ self ].
	languageInfo := aLanguageInfo.
	self changed: #style.
]

{ #category : #helpers }
PolyglotInspector >> languageInfoFor: anObject [
	| info |
	info := (Interop getLanguageInfo: anObject).
	^ 'Name:				{1}
ID:					{2}
Version:			{3}
Default Mime Type:	{4}
Mime Types:		{5}
isInternal:			{6}
isInteractive:		{7}' format: {
	info getName asString. info getId asString. info getVersion asString. info getDefaultMimeType asString.
	info getMimeTypes toString asString. info isInternal. info isInteractive }
]

{ #category : #'menu commands' }
PolyglotInspector >> mainFieldListMenu: aMenu [
	"Arm the supplied menu with items for the field-list of the receiver"

	aMenu addStayUpItemSpecial.

	aMenu addList: #(
		('inspect (i)'						inspectSelection)
		('explore (I)'						exploreSelection)
		('basic inspect'						inspectSelectionBasic)
		('inspect VM object'					inspectVMObject)).

	(#(self ellipsis element nil) includes: self typeOfSelection)
		ifTrue: [self addCollectionItemsTo: aMenu].

	self addFieldItemsTo: aMenu.
	
	aMenu addLine.

	Polyglot availableLanguages do: [ :ea |
		aMenu
			add: 'inspect as ', ea getName
			target: self
			selector: #inspectAs:
			argument: ea ].

	^ aMenu

]

{ #category : #helpers }
PolyglotInspector >> membersInvocableInfoFor: anObject [
	^ ((Interop getMembers: anObject)
			select: [ :ea | Interop isMemberInvocable: anObject member: ea ])
		ifNotEmpty: [ :invocables | Text streamContents: [ :aStream |
			invocables sorted do: [ :member |
				(Interop isMemberReadable: anObject member: member)
					ifTrue: [ aStream
								withAttribute: (PluggableTextAttribute evalBlock: [
										(Interop readMember: self object member: member) inspect ])
									do: [ aStream nextPutAll: member ];
								withAttributes: { TextEmphasis italic. TextColor color: Color gray }
									do: [ aStream nextPutAll: ' (readable)' ] ]
					ifFalse: [ aStream
								nextPutAll: member;
								withAttributes: { TextEmphasis italic. TextColor color: Color gray }
									do: [ aStream nextPutAll: ' (non-readable)' ] ].
						aStream cr ] ] ]
		ifEmpty: [ 'n/a' ]
		
]

{ #category : #helpers }
PolyglotInspector >> membersReadableInfoFor: anObject [
	^ ((Interop getMembers: anObject)
		select: [ :ea | (Interop isMemberReadable: anObject member: ea) and: [
				(Interop isMemberInvocable: anObject member: ea) not ] ])
			ifNotEmpty: [ :readables | Text streamContents: [ :aStream |
				readables sorted do: [ :member | | value |
					value := [ Interop readMember: self object member: member ]
								on: Error do: [ :e | e ].
					aStream
						withAttribute: (PluggableTextAttribute evalBlock: [ value inspect ])
							do: [ aStream nextPutAll: member ];
						nextPutAll: ': ';
						withAttributes: { TextEmphasis italic. TextColor color: Color gray }
							do: [ aStream nextPutAll: '', value asString ];
						cr ] ] ]
			ifEmpty: [ 'n/a' ]

]

{ #category : #language }
PolyglotInspector >> selectLanguage [
	self languageInfo: Polyglot chooseLanguageInfo
]

{ #category : #language }
PolyglotInspector >> setAdaptiveLanguageSelection [
	Polyglot askForAdaptiveLanguageSelection.
	(Polyglot useAdaptiveLanguageSelection and: [
			Interop hasLanguage: self object ]) ifTrue: [
		self languageInfo: (Interop getLanguageInfo: self object ) ]
]

{ #category : #language }
PolyglotInspector >> setDefaultLanguage [
	Polyglot askForDefaultLanguageInfo.
	self languageInfo: Polyglot defaultLanguageInfo
]

{ #category : #'code pane menu' }
PolyglotInspector >> setSyntaxHighlighter [
	Polyglot askForRougeStyler.
	self changed: #style
]

{ #category : #'updating - steps' }
PolyglotInspector >> stepTimeIn: aWindow [
	^ (selectionUpdateTime ifNil: [0]) * 10 max: 4000
]

{ #category : #'fields - streaming' }
PolyglotInspector >> streamArrayElementsOn: aStream [
	"Truncate array elements if there are too many of them."
	
	self
		streamOn: aStream
		truncate: (1 to: ((Interop hasArrayElements: self object)
							ifTrue: [ Interop getArraySize: self object ] ifFalse: [ 0 ] ))
		collectFields: [:index |
			(self newFieldForType: #indexed key: index)
				valueGetter: [ :o | Interop readArrayElement: o index: index];
				valueSetter: [ :o :value | Interop writeArrayElement: o index: index value: value];
				yourself]
]

{ #category : #'fields - streaming' }
PolyglotInspector >> streamBaseFieldsOn: aStream [

	aStream
		nextPut: self fieldSelf.
]

{ #category : #'fields - streaming' }
PolyglotInspector >> streamFieldsOn: aStream [

	self
		streamBaseFieldsOn: aStream;
		streamOptionalFieldsOn: aStream;
		streamArrayElementsOn: aStream;
		streamCustomFieldsOn: aStream.
]

{ #category : #'fields - streaming' }
PolyglotInspector >> streamOptionalFieldsOn: aStream [

	(Interop hasMembers: self object) ifTrue: [
		aStream nextPutAll: self fieldsMembers ].
	(Interop hasHashEntries: self object) ifTrue: [
		aStream nextPut: self fieldHashEntries ].
	(Interop isMetaObject: self object) ifTrue: [
		aStream nextPut: self fieldMetaName ].
	(Interop hasMetaObject: self object) ifTrue: [
		aStream nextPut: self fieldMetaObjectInfo ].
	(Interop hasLanguage: self object) ifTrue: [
		aStream nextPut: self fieldLanguageInfo ].
	(Interop hasSourceLocation: self object) ifTrue: [
		aStream nextPut: self fieldSource ].
	(Interop isException: self object) ifTrue: [
		aStream nextPut: self fieldException ].
]
