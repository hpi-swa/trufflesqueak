Class {
	#name : #CallTargetBrowserRuby,
	#superclass : #Model,
	#instVars : [
		'selectedModuleName',
		'selectedMethodName',
		'sortBy',
		'autoUpdate'
	],
	#classVars : [
		'MethodGetter',
		'MethodListGetter'
	],
	#category : #'TruffleSqueak-Utilities'
}

{ #category : #helpers }
CallTargetBrowserRuby class >> callTargetFor: aMethodName of: aModuleName [
	^ (self methodFor: aMethodName of: aModuleName)
		vmObject method callTarget
]

{ #category : #helpers }
CallTargetBrowserRuby class >> methodFor: aMethodName of: aModuleName [
	^ self methodGetter value: aModuleName value: aMethodName
]

{ #category : #helpers }
CallTargetBrowserRuby class >> methodGetter [
	"MethodGetter := nil"
	^ MethodGetter ifNil: [ MethodGetter := Polyglot eval: #ruby string:
		'Proc.new{|mod_name,meth_name| m1 = Object.const_get(mod_name); m2 = meth_name.to_s.to_sym; if m1.methods(false).include?(m2);m1.method(m2);else;m1.instance_method(m2);end}' ]
]

{ #category : #helpers }
CallTargetBrowserRuby class >> methodList: aModuleName [
	^ ((self methodListGetter value: aModuleName) asCollection
			collect: [:ea | ea asString]) sorted
]

{ #category : #helpers }
CallTargetBrowserRuby class >> methodListGetter [
	"MethodListGetter := nil"
	^ MethodListGetter ifNil: [ MethodListGetter := Polyglot eval: #ruby string:
		'Proc.new{|mod_name| m = Object.const_get(mod_name); m.methods(false).concat(m.instance_methods(false))}' ]
]

{ #category : #opening }
CallTargetBrowserRuby class >> open [
	self new open
]

{ #category : #accessing }
CallTargetBrowserRuby >> autoUpdate [
	^ autoUpdate ifNil: [ autoUpdate := true ]
]

{ #category : #toolbuilder }
CallTargetBrowserRuby >> buildCallTargetPaneWith: builder [
	| textSpec |
	textSpec := builder pluggableTextSpec new.
	textSpec 
		model: self;
		getText: #callTargetInfo.
	^ textSpec
]

{ #category : #toolbuilder }
CallTargetBrowserRuby >> buildMethodListWith: builder [
	| listSpec |
	listSpec := builder pluggableListSpec new.
	listSpec 
		model: self;
		list: #methodList; 
		getIndex: #methodListIndex; 
		setIndex: #methodListIndex:; 
		icon: #methodIconAt:;
		menu: #methodListMenu:.
	^ listSpec

]

{ #category : #toolbuilder }
CallTargetBrowserRuby >> buildModuleListWith: builder [
	| listSpec |
	listSpec := builder pluggableListSpec new.
	listSpec 
		model: self;
		list: #moduleAndClassList; 
		getIndex: #moduleAndClassListIndex;
		setIndex: #moduleAndClassListIndex:.
	^ listSpec
]

{ #category : #toolbuilder }
CallTargetBrowserRuby >> buildWith: builder [
	^ builder build: (self buildWindowWith: builder specs: {
		(0@0 corner: 0.25@1) -> [self buildModuleListWith: builder].
		(0.25@0 corner: 0.5@1) -> [self buildMethodListWith: builder].
		(0.5@0 corner: 1@1) -> [self buildCallTargetPaneWith: builder].
	})
]

{ #category : #'call targets' }
CallTargetBrowserRuby >> callTargetInfo [
	^ self selectedCallTarget
		ifNotNil: [ :c | CallTargetBrowser callTargetInfoStringFor: c ]
		ifNil: [ 'no call target available' ]
]

{ #category : #methods }
CallTargetBrowserRuby >> countsToMethods: aMethodList [
	| result |
	result := Dictionary new.
	aMethodList do: [ :ea | | callTarget |
		callTarget := (self class callTargetFor: ea of: self selectedModuleName).
		result
			at: (callTarget ifNil: [-1] ifNotNil: [ :c |
				self sortBy caseOf: {
					[#callCount] -> [ | v |
						v := c getCallCount.
						v > 0 ifTrue: [ v ln ] ifFalse: [ v ] ].
					[#callAndLoopCount] -> [ | v |
						v := c getCallAndLoopCount.
						v > 0 ifTrue: [ v ln ] ifFalse: [ v ] ].
					[#knownCallSiteCount] -> [ c getKnownCallSiteCount ].
					[#nonTrivialNodeCount] -> [ c getNonTrivialNodeCount ] } ] )
			ifPresent: [ :v | v add: ea asString ]
			ifAbsentPut: [ OrderedCollection with: ea ] ].
	^ result
]

{ #category : #overrides }
CallTargetBrowserRuby >> initialExtent [
	^ 800 @ 400
]

{ #category : #'call targets' }
CallTargetBrowserRuby >> inspectCallTarget [
	self selectedCallTarget inspect
]

{ #category : #methods }
CallTargetBrowserRuby >> inspectMethod [
	self selectedMethod inspect
]

{ #category : #toolbuilder }
CallTargetBrowserRuby >> labelString [
	| label |
	label := 'CallTargetBrowser for TruffleRuby'.
	self selectedModuleName
		ifNotNil: [ :name | label := label, ': ', name ].
	^ label
]

{ #category : #methods }
CallTargetBrowserRuby >> methodIconAt: anIndex [
	^ nil
]

{ #category : #methods }
CallTargetBrowserRuby >> methodList [
	| methods countsToMessages sortedCounts highestCount result |
	methods := self selectedMethods.
	(self sortBy == #alphabetically or: [ methods isEmpty])
		ifTrue: [ ^ methods ].
	countsToMessages := self countsToMethods: methods.
	result := OrderedCollection new.
	sortedCounts := countsToMessages keysInOrder reversed.
	highestCount := sortedCounts first max: 1.
	sortedCounts do: [ :count |
		result addAll: ((countsToMessages at: count) collect: [ :ea |
			ea asText
				addAttribute: (CallTargetBrowser textColorFor: count with: highestCount)
				yourself])].
	^ result
]

{ #category : #methods }
CallTargetBrowserRuby >> methodListIndex [
	^ self methodList indexOf: self selectedMethodName
]

{ #category : #methods }
CallTargetBrowserRuby >> methodListIndex: anInteger [
	self selectedMethodName: (self methodList at: anInteger ifPresent: [ :t | t asString ] ifAbsent: [ nil ])
]

{ #category : #methods }
CallTargetBrowserRuby >> methodListMenu: aMenu [
	^ aMenu
		add: 'inspect method' action: #inspectMethod;
		add: 'inspect call target' action: #inspectCallTarget;
		addLine;
		add: ('toggle auto-update ', (self autoUpdate ifTrue: [ '(on)' ] ifFalse: [ '(off)' ])) action: #toggleAutoUpdate;
		addLine;
		add: 'sort alphabetically' action: #sortAlphabetically;
		add: 'sort by call and loop count (default)' action: #sortByCallAndLoopCount;
		add: 'sort by call count' action: #sortByCallCount;
		add: 'sort by known call site count' action: #sortByKnownCallSiteCount;
		add: 'sort by non-trivial node count' action: #sortByNonTrivialNodeCount;
		yourself
]

{ #category : #'modules-classes' }
CallTargetBrowserRuby >> moduleAndClassList [
	^ ((Polyglot eval: #ruby string: 'Object.constants')
			asCollection collect: [ :ea | ea asString ]) sorted
]

{ #category : #'modules-classes' }
CallTargetBrowserRuby >> moduleAndClassListIndex [
	^ self moduleAndClassList indexOf: self selectedModuleName
]

{ #category : #'modules-classes' }
CallTargetBrowserRuby >> moduleAndClassListIndex: anInteger [
	self selectedModuleName: (self moduleAndClassList at: anInteger ifAbsent: [ nil ])
]

{ #category : #'initialize-release' }
CallTargetBrowserRuby >> open [
	^ ToolBuilder open: self
]

{ #category : #'call targets' }
CallTargetBrowserRuby >> selectedCallTarget [
	^ self selectedModuleName
		ifNil: [ nil ]
		ifNotNil: [ :moduleName |
			self selectedMethodName
				ifNil: [ nil ]
				ifNotNil: [ :methodName | self class callTargetFor: methodName of: moduleName] ]
]

{ #category : #methods }
CallTargetBrowserRuby >> selectedMethod [
	^ self selectedModuleName
		ifNil: [ nil ]
		ifNotNil: [ :moduleName |
			self selectedMethodName
				ifNil: [ nil ]
				ifNotNil: [ :methodName | self class methodFor: methodName of: moduleName] ]
]

{ #category : #accessing }
CallTargetBrowserRuby >> selectedMethodName [

	^ selectedMethodName
]

{ #category : #accessing }
CallTargetBrowserRuby >> selectedMethodName: anObject [

	selectedMethodName := anObject.
	self changed: #methodListIndex.
	self changed: #callTargetInfo.
]

{ #category : #methods }
CallTargetBrowserRuby >> selectedMethods [
	^ self selectedModuleName
		ifNotNil: [ :name | self class methodList: name ]
		ifNil: [ #() ]
]

{ #category : #accessing }
CallTargetBrowserRuby >> selectedModuleName [

	^ selectedModuleName
]

{ #category : #accessing }
CallTargetBrowserRuby >> selectedModuleName: anObject [

	selectedModuleName := anObject.
	selectedMethodName := nil.
	self changed: #moduleAndClassListIndex.
	self changed: #methodList.
	self changed: #relabel.
]

{ #category : #sorting }
CallTargetBrowserRuby >> sortAlphabetically [
	self sortBy: #alphabetically
]

{ #category : #sorting }
CallTargetBrowserRuby >> sortBy [

	^ sortBy ifNil: [ sortBy := #callAndLoopCount ]
]

{ #category : #sorting }
CallTargetBrowserRuby >> sortBy: anObject [

	sortBy := anObject.
	self changed: #methodList
]

{ #category : #sorting }
CallTargetBrowserRuby >> sortByCallAndLoopCount [
	self sortBy: #callAndLoopCount
]

{ #category : #sorting }
CallTargetBrowserRuby >> sortByCallCount [
	self sortBy: #callCount
]

{ #category : #sorting }
CallTargetBrowserRuby >> sortByKnownCallSiteCount [
	self sortBy: #knownCallSiteCount
]

{ #category : #sorting }
CallTargetBrowserRuby >> sortByNonTrivialNodeCount [
	self sortBy: #nonTrivialNodeCount
]

{ #category : #testing }
CallTargetBrowserRuby >> stepIn: aSystemWindow [
	self selectedModuleName ifNotNil: [
		self changed: #methodList.
		self selectedMethodName ifNotNil: [
			self changed: #callTargetInfo ] ]
]

{ #category : #accessing }
CallTargetBrowserRuby >> toggleAutoUpdate [
	autoUpdate := autoUpdate not
]

{ #category : #testing }
CallTargetBrowserRuby >> wantsStepsIn: aWindow [
	^ self autoUpdate
]
