Class {
	#name : #CallTargetBrowser,
	#superclass : #Browser,
	#instVars : [
		'sortBy',
		'showCoverage'
	],
	#category : #'TruffleSqueak-Utilities'
}

{ #category : #helpers }
CallTargetBrowser class >> callTargetInfoStringFor: aCallTarget [
	^ aCallTarget
		ifNil: [ 'no call target available' ]
		ifNotNil: [ :c |
'name:					{1}
highestCompiledTier:	{2}
callCount:				{3}
callAndLoopCount:		{4}
knownCallSiteCount:	{5}
nonTrivialNodeCount:	{6}
profiled return value:	{7}
profiled arguments:		{8}
included call nodes:
	- {9}' format: {
	c getName asString.
	c highestCompiledTier.
	c getCallCount.
	c getCallAndLoopCount.
	c getKnownCallSiteCount.
	c getNonTrivialNodeCount.
	c returnProfile
		ifNil: [ 'n/a' ]
		ifNotNil: [ :p | p getType ifNil: [ 'n/a' ] ifNotNil: [ :e | e getSimpleName asString ]].
	c argumentsProfile
		ifNil: [ 'all n/a' ]
		ifNotNil: [ :p | p getTypes
			ifNil: [ 'all n/a' ]
			ifNotNil: [ :t | (t asCollection collect: [ :ea | ea
				ifNil: ['n/a']
				ifNotNil: [ ea getSimpleName asString ]]) joinSeparatedBy: ', ' ] ].
	(c getCallNodes asCollection
		ifNotEmpty: [:n | (n collect: [ :ea |
				'{1} (callCount: {2}; inliningForced: {3}; identity: {4})' format: {
					ea getCurrentCallTarget toString asString.
					ea getCallCount.
					ea isInliningForced.
					ea identityHash } ])
				joinSeparatedBy: String cr, '	- ' ]
		ifEmpty: [ 'n/a' ] ) } ]

]

{ #category : #'class initialization' }
CallTargetBrowser class >> initialize [
	"self initialize"
	super initialize.
	self registerInWorldMenu
]

{ #category : #'world menu' }
CallTargetBrowser class >> registerInWorldMenu [
	self environment at: #TheWorldMenu ifPresent: [ :class |
		class registerOpenCommand: (Array 
			with: 'CallTarget Browser' 
			with: (Array
				with: self
				with: #open)) ].
]

{ #category : #preferences }
CallTargetBrowser class >> showClassIcons [
	^ false
]

{ #category : #preferences }
CallTargetBrowser class >> showMessageIcons [
	^ false
]

{ #category : #helpers }
CallTargetBrowser class >> textColorFor: aCount with: aMax [
	
	^ aCount >= 0
		ifTrue: [ | r |
			r := 1.0 * aCount / aMax.
			TextColor color: (Color
				r: r
				g: 0
				b: 1.0 - r) ]
		ifFalse: [ TextColor gray]
]

{ #category : #'what to show' }
CallTargetBrowser >> aboutToStyle: aStyler [
	^ self showCoverage not and: [ super aboutToStyle: aStyler ]
]

{ #category : #'message list' }
CallTargetBrowser >> callTargetInfoContents [
	^ contents := self selectedClassOrMetaClass methodDict
		at: self selectedMessageName
		ifPresent: [ :cm | self class callTargetInfoStringFor: cm callTarget ]
		ifAbsent: [ 'not found' ]

]

{ #category : #controls }
CallTargetBrowser >> contentsSymbolQuints [
	^ super contentsSymbolQuints, #(#-
		(callTargetInfo toggleCallTargetInfo showingCallTargetInfoString 'call target' 'information on underlying call target'))
]

{ #category : #helpers }
CallTargetBrowser >> countsToMessages: aMessageList [
	| result |
	result := Dictionary new.
	aMessageList do: [ :ea | | callTarget |
		callTarget := (self selectedClassOrMetaClass >> ea asSymbol) callTarget.
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
			ifPresent: [ :v | v add: ea ]
			ifAbsentPut: [ OrderedCollection with: ea ] ].
	^ result
]

{ #category : #'initialize-release' }
CallTargetBrowser >> defaultBrowserTitle [
	^  'CallTarget Browser'
]

{ #category : #'message list' }
CallTargetBrowser >> formattedLabel: aString forSelector: aSymbol inClass: aClass [
	^ aString
]

{ #category : #'message functions' }
CallTargetBrowser >> inspectCallTarget [
	(self selectedClassOrMetaClass
		compiledMethodAt: self selectedMessageName
		ifAbsent: []) ifNotNil:
			[:method| method callTarget inspect]
]

{ #category : #'message functions' }
CallTargetBrowser >> mainMessageListMenu: aMenu [ 
	^ (super mainMessageListMenu: aMenu)
		add: 'inspect call target' action: #inspectCallTarget;
		addLine;
		add: 'sort alphabetically' action: #sortAlphabetically;
		add: 'sort by call and loop count (default)' action: #sortByCallAndLoopCount;
		add: 'sort by call count' action: #sortByCallCount;
		add: 'sort by known call site count' action: #sortByKnownCallSiteCount;
		add: 'sort by non-trivial node count' action: #sortByNonTrivialNodeCount;
		addLine;
		add: 'toggle coverage based on Truffle ASTs' action: #toggleCoverage;
		addLine;
		yourself
]

{ #category : #'message list' }
CallTargetBrowser >> messageList [
	| messages countsToMessages sortedCounts highestCount result |
	messages := super messageList.
	(self sortBy == #alphabetically or: [ messages isEmpty])
		ifTrue: [ ^ messages ].
	countsToMessages := self countsToMessages: messages.
	result := OrderedCollection new.
	sortedCounts := countsToMessages keysInOrder reversed.
	highestCount := sortedCounts first max: 1.0.
	sortedCounts do: [ :count |
		result addAll: ((countsToMessages at: count) collect: [ :ea |
			ea asText
				addAttribute: (self class textColorFor: count with: highestCount)
				yourself])].
	^ result
]

{ #category : #'message list' }
CallTargetBrowser >> selectedBytecodes [
	| cm text |
	cm := (self selectedClassOrMetaClass compiledMethodAt: self selectedMessageName
			ifAbsent: [ ^ '' asText ]).
	text := cm symbolic asText.
	text addAttribute: TextColor gray; yourself.
	cm callTarget ifNotNil: [ :ct | | nodes index string |
		nodes := ct getRootNode executeBytecodeNode bytecodeNodes.
		index := 1.
		string := text string.
		string lineIndicesDo: [ :start :end :endWithLineEnding |
			(string at: start) = Character tab ifFalse: [ | numBytesOfBytecode |
				text addAttribute: (TextColor color: (
						(nodes at: index)
							ifNil: [ Color red ] ifNotNil: [ Color green ]) muchDarker)
					from: start to: end.
				numBytesOfBytecode := ((string indexOf: $> startingAt: start) -
											(string indexOf: $< startingAt: start)) // 3.
				index := index + numBytesOfBytecode ]]].
	^ text
]

{ #category : #'message list' }
CallTargetBrowser >> selectedMessage [
	contents == nil ifFalse: [^ contents copy].

	self showingCallTargetInfo ifTrue:
		[ ^ self callTargetInfoContents ].

	^ super selectedMessage
]

{ #category : #'what to show' }
CallTargetBrowser >> showCallTargetInfo: aBoolean [
	self contentsSymbol: (aBoolean ifFalse: [#source] ifTrue: [#callTargetInfo])
]

{ #category : #accessing }
CallTargetBrowser >> showCoverage [

	^ showCoverage ifNil: [ false ]
]

{ #category : #accessing }
CallTargetBrowser >> showCoverage: anObject [

	showCoverage := anObject.
]

{ #category : #'what to show' }
CallTargetBrowser >> showingCallTargetInfo [
	^ contentsSymbol == #callTargetInfo
]

{ #category : #'what to show' }
CallTargetBrowser >> showingCallTargetInfoString [
	"Answer a string telling whether the receiver is showing plain source"

	^ (self showingCallTargetInfo
		ifTrue:
			['<yes>']
		ifFalse:
			['<no>']), 'call target'
]

{ #category : #actions }
CallTargetBrowser >> sortAlphabetically [
	self sortBy: #alphabetically
]

{ #category : #accessing }
CallTargetBrowser >> sortBy [

	^ sortBy ifNil: [ sortBy := #callAndLoopCount ]
]

{ #category : #accessing }
CallTargetBrowser >> sortBy: anObject [

	sortBy := anObject.
]

{ #category : #actions }
CallTargetBrowser >> sortByCallAndLoopCount [
	self sortBy: #callAndLoopCount
]

{ #category : #actions }
CallTargetBrowser >> sortByCallCount [
	self sortBy: #callCount
]

{ #category : #actions }
CallTargetBrowser >> sortByKnownCallSiteCount [
	self sortBy: #knownCallSiteCount
]

{ #category : #actions }
CallTargetBrowser >> sortByNonTrivialNodeCount [
	self sortBy: #nonTrivialNodeCount
]

{ #category : #'message list' }
CallTargetBrowser >> sourceStringPrettifiedAndDiffed [
	| text |
	text := super sourceStringPrettifiedAndDiffed.
	(self showCoverage and: [ contentsSymbol = #source ]) ifFalse: [ ^ text ].
	currentCompiledMethod callTarget ifNotNil: [ :ct | | map nodes initialPC scanner client |
		map := DebuggerMethodMap forMethod: currentCompiledMethod.
		(ct getRootNode respondsTo: #executeBytecodeNode) ifFalse: [ ^ text ].
		nodes := ct getRootNode executeBytecodeNode bytecodeNodes.
		initialPC := currentCompiledMethod initialPC.
		scanner := InstructionStream on: currentCompiledMethod.
		client := InstructionClient new.
		[ scanner pc <= currentCompiledMethod endPC ] whileTrue: [ | pc range attribute |
			pc := scanner pc.
			((scanner willSend or: [ scanner willReturn ]) or: [ scanner willStore ]) ifTrue: [
				range := map rangeForPC: pc in: currentCompiledMethod contextIsActiveContext: true.
				attribute := TextColor color: (
						(nodes at: (1 + pc - initialPC))
							ifNil: [ Color red ] ifNotNil: [ Color green ]) muchDarker.
				text addAttribute: attribute from: range first to: range last ].
			scanner interpretNextInstructionFor: client ] ].
	^ text
]

{ #category : #'what to show' }
CallTargetBrowser >> toggleCallTargetInfo [
	| wasShowing |
	self okToChange ifTrue:
		[wasShowing := self showingCallTargetInfo.
		self restoreTextualCodingPane.
		self showCallTargetInfo: wasShowing not.
		self setContentsToForceRefetch.
		self contentsChanged]
]

{ #category : #actions }
CallTargetBrowser >> toggleCoverage [
	self showCoverage: self showCoverage not.
	self changed: #contents
]

{ #category : #'self-updating' }
CallTargetBrowser >> updateListsAndCodeIn: aWindow [
	super updateListsAndCodeIn: aWindow.
	(self showingCallTargetInfo or: [ self showCoverage ]) ifTrue: [
		self setContentsToForceRefetch.
		self contentsChanged ]
]
