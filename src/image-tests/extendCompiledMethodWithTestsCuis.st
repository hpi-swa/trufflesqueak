'Merged ChangeSets for extending CompiledMethod and testing the extension'!
'From Cuis7.5 [latest update: #7766] on 16 January 2026 at 4:04:17 pm'!
!classDefinition: #CompiledMethod category: #'Kernel-Methods'!
ByteArray variableByteSubclass: #CompiledMethod
	instanceVariableNames: ''
	classVariableNames: 'LargeFrame PreferredBytecodeSetEncoderClass SmallFrame HugeFrame '
	poolDictionaries: ''
	category: 'Kernel-Methods'!

!CompiledMethod commentStamp: 'sss 14/Jan/2026 16:02:53' prior: 0!
My instances are methods suitable for interpretation by the virtual machine.  This is the only class in the system whose instances intermix both indexable pointer fields and indexable integer fields. The pointer fields are used for literals and metadata, and the bytes are used for bytecodes and a variety of encoded informaton such as source code, source code position, etc.  The first part of a CompiledMethod object is pointers, the second part is bytes.  CompiledMethod inherits from ByteArray to avoid duplicating some of ByteArray's methods, not because a CompiledMethod is a ByteArray.

Instance variables: *indexed* (no named inst vars)

Class variables:
SmallFrame							- the number of stack slots in a small frame Context
LargeFrame							- the number of stack slots in a large frame Context
HugeFrame							- the number of stack slots in a huge frame Context
PreferredBytecodeSetEncoderClass	- the encoder class that is used to create new methods (either EncoderForV3PlusClosures or EncoderForSistaV1)

The current format of a CompiledMethod is as follows:

	header (8 bytes, SmallInteger)
	literals (8 bytes each, Object, see "The last literal..." below)
	bytecodes  (variable, bytes)
	trailer (variable, bytes)

The header is a SmallInteger (61 bits) in the following format:

	(index 0)		15 bits:	number of literals (#numLiterals)
	(index 15)		  1 bit:	jit without counters - reserved for methods that have been optimized by Sista
	(index 16)		  1 bit:	has primitive
	(index 17)		  1 bit:	whether a large frame size is needed (#frameSize => either SmallFrame or LargeFrame)
	(index 18)		  6 bits:	number of temporary variables (#numTemps)
	(index 24)		  4 bits:	number of arguments to the method (#numArgs)
	(index 28)		  2 bits:	reserved for an access modifier (00-unused, 01-private, 10-protected, 11-public), although accessors for bit 29 exist (see #flag).
	(index 30)		  2 bits:	reserved, 0
	(index 32)		  5 bits:	reserved, 0
	(index 37)^		  1 bit:	whether a huge frame is needed (#frameSize => HugeFrame)
	(index 38)^		  2 bits:	number of temporary variables extension upper bits
	(index 40)		  4 bits:	reserved, 0
	(index 44)^		  4 bits:	number of arguments extension upper bits
	(index 48)		12 bits:	reserved, 0
	sign bit:			  1 bit:	selects the instruction set, >= 0 Primary, < 0 Secondary (#signFlag)

^ These 3 fields are zero in vanilla Squeak and Cuis images. They are used to extend numArgs / numTemps.

If the method has a primitive then the first bytecode of the method must be a callPrimitive: bytecode that encodes the primitive index.  This bytecode can encode a primitive index from 0 to 65535.

The trailer has two variant formats.  In the first variant, the last byte is at least 252 and the last four bytes represent a source pointer into one of the sources files (see #sourcePointer).  In the second variant, the last byte is less than 252, and the last several bytes are a compressed version of the names of the method's temporary variables.  The number of bytes used for this purpose is the value of the last byte in the method.

While there are disadvantages to this "flat" representation (it is impossible to add named instance variables to CompiledMethodor its subclasses, but it is possible indirectly; see AdditionalMethodState) it is effective for interpreters.  It means that both bytecodes and literals can be fetched directly from a single method object, and that only one object, the method, must be saved and restored on activation and return.  A more natural representation, in which there are searate instance variables for the bytecode, and (conveniently) the literals, requires either much more work on activation and return setting up references to the literals and bytecodes, or slower access to bytecodes and literals, indirecting on each access.

The last literal of a CompiledMethod is reserved for special use by the kernel and/or the virtual machine.  It must either be the methodClassAssociation, used to implement super sends, or nil, if the method is anonymous.

By convention, the penultimate literal is reserved for special use by the kernel. In CompiledMethod instances it must either be the method selector, or an instance of AdditionalMethodState which holds the selector and any pragmas or properties in the method.
!

!CompiledMethod class methodsFor: 'class initialization' stamp: 'sss 14/Jan/2026 16:26:54'!
initialize    "CompiledMethod initialize"
	"Initialize class variables specifying the size of the temporary frame
	needed to run instances of me."

	SmallFrame := 16.	"Context range for temps+stack"
	LargeFrame := 56.
	HugeFrame := 127.
	PreferredBytecodeSetEncoderClass ifNil:
		[PreferredBytecodeSetEncoderClass := EncoderForV3PlusClosures]! !

CompiledMethod initialize!


!Encoder methodsFor: 'accessing' stamp: 'sss 14/Jan/2026 20:37:47'!
maxIndexableArgs
	"Answer the maximum number of arguments supported by the receiver's
	 bytecode set. This is a nominal value based on the Blue Book bytecode
	 set; subclasses answer a more accurate value."
	^ 15! !

!Encoder methodsFor: 'accessing' stamp: 'sss 14/Jan/2026 20:45:13'!
maxIndexableTemps
	"Answer the maximum number of temporaries supported by the receiver's
	 bytecode set. This is a nominal value based on the Blue Book bytecode
	 set; subclasses answer a more accurate value."
	^ 63! !


!BytecodeEncoder methodsFor: 'accessing' stamp: 'sss 14/Jan/2026 20:39:31'!
maxNumArgs
	^ CompiledMethod maxNumArgs min: self maxIndexableArgs! !

!BytecodeEncoder methodsFor: 'accessing' stamp: 'sss 14/Jan/2026 20:45:53'!
maxNumTemps
	^ CompiledMethod maxNumTemps min: self maxIndexableTemps! !

!BytecodeEncoder methodsFor: 'method generation' stamp: 'sss 14/Jan/2026 19:25:25'!
checkLimitsNumArgs: numArgs numTemps: numTemps numLits: numLits errorPrefix: errorPrefixString
	"Fails with an error if the limits for the encoder are exceeded."

	CompiledMethod
		checkLimitsLegacyNumArgs: numArgs
		numTemps: numTemps
		numLits: numLits
		errorPrefix: errorPrefixString.! !

!BytecodeEncoder methodsFor: 'accessing' stamp: 'sss 18/Jan/2026 15:49:19'!
maxFrameSize

	^ CompiledMethod fullFrameSize! !


!EncoderForSistaV1 methodsFor: 'accessing' stamp: 'sss 18/Jan/2026 15:49:35'!
maxFrameSize

	^ CompiledMethod maxFrameSize! !


!CompiledMethod class methodsFor: 'constants' stamp: 'sss 18/Jan/2026 15:47:41'!
maxFrameSize

	^ HugeFrame! !

!EncoderForSistaV1 methodsFor: 'accessing' stamp: 'sss 14/Jan/2026 20:39:11'!
maxIndexableArgs
	"Answer the maximum number of arguments supported by the receiver's
	 bytecode set."
	^ 255! !

!EncoderForSistaV1 methodsFor: 'accessing' stamp: 'sss 14/Jan/2026 20:46:33'!
maxIndexableTemps
	"Answer the maximum number of temporaries supported by the receiver's
	 bytecode set."
	^ 255! !

!EncoderForSistaV1 methodsFor: 'method generation' stamp: 'sss 14/Jan/2026 19:24:42'!
checkLimitsNumArgs: numArgs numTemps: numTemps numLits: numLits errorPrefix: errorPrefixString
	"Fails with an error if the limits for the encoder are exceeded."

	CompiledMethod
		checkLimitsNumArgs: numArgs
		numTemps: numTemps
		numLits: numLits
		errorPrefix: errorPrefixString.! !


!CompiledMethod class methodsFor: 'class initialization' stamp: 'sss 14/Jan/2026 16:43:36'!
hugeFrameSize

	^ HugeFrame! !

!CompiledMethod class methodsFor: 'constants' stamp: 'sss 14/Jan/2026 19:58:30'!
maxNumArgs
	"The current header format and the TruffleSqueak VM's interpretation of it allows for a maximum of 127 arguments."

	^ 127! !

!CompiledMethod class methodsFor: 'constants' stamp: 'sss 14/Jan/2026 19:58:34'!
maxNumTemps
	"The current header format and the TruffleSqueak VM's interpretation of it allows for a maximum of 127 temporaries."

	^ 127! !

!CompiledMethod class methodsFor: 'header manipulation' stamp: 'sss 14/Jan/2026 19:17:09'!
checkLimitsLegacyNumArgs: numArgs numTemps: numTemps numLits: numLits
	"Return a Boolean indicating whether a method can be created with the given argument, temporary and literal counts."

	^ self checkLimitsLegacyNumArgs: numArgs numTemps: numTemps numLits: numLits errorPrefix: nil! !

!CompiledMethod class methodsFor: 'header manipulation' stamp: 'sss 14/Jan/2026 19:16:58'!
checkLimitsLegacyNumArgs: numArgs numTemps: numTemps numLits: numLits errorPrefix: errorPrefixStringOrNil
	"If the last argument is nil, return a Boolean indicating whether a method can be created with the given argument, temporary and literal counts. Otherwise, the last argument is assumed to be a String prefix to be used on a raised error."
	| errorSuffix |

	numArgs > 15
		ifTrue: [errorSuffix := ' -- too many arguments'].
	numTemps > 63
		ifTrue: [errorSuffix := ' -- too many temporary variables'].
	numLits > self maxNumLiterals
		ifTrue: [errorSuffix := ' -- too many literals'].

	^ errorSuffix
		ifNil: [true]
		ifNotNil: [
			errorPrefixStringOrNil
				ifNil: [false]
				ifNotNil: [self error: errorPrefixStringOrNil, errorSuffix]]! !

!CompiledMethod class methodsFor: 'header manipulation' stamp: 'sss 14/Jan/2026 16:25:00'!
checkLimitsNumArgs: numArgs numTemps: numTemps numLits: numLits
	"Return a Boolean indicating whether a method can be created with the given argument, temporary and literal counts."

	^ self checkLimitsNumArgs: numArgs numTemps: numTemps numLits: numLits errorPrefix: nil! !

!CompiledMethod class methodsFor: 'header manipulation' stamp: 'sss 14/Jan/2026 18:31:15'!
checkLimitsNumArgs: numArgs numTemps: numTemps numLits: numLits errorPrefix: errorPrefixStringOrNil
	"If the last argument is nil, return a Boolean indicating whether a method can be created with the given argument, temporary and literal counts. Otherwise, the last argument is assumed to be a String prefix to be used on a raised error."
	| errorSuffix |

	numArgs > self maxNumArgs
		ifTrue: [errorSuffix := ' -- too many arguments'].
	numTemps > self maxNumTemps
		ifTrue: [errorSuffix := ' -- too many temporary variables'].
	numLits > self maxNumLiterals
		ifTrue: [errorSuffix := ' -- too many literals'].

	^ errorSuffix
		ifNil: [true]
		ifNotNil: [
			errorPrefixStringOrNil
				ifNil: [false]
				ifNotNil: [self error: errorPrefixStringOrNil, errorSuffix]]! !

!CompiledMethod class methodsFor: 'header manipulation' stamp: 'sss 14/Jan/2026 19:43:27'!
headerForEncoderClass: encoderClass numArgs: numArgs numTemps: numTemps numLiterals: numLits frameSize: frameSize hasPrimitive: primitiveBoolean
	"Answer the method header value for the given argument, temporary and literal count, with the given frame size and whether the method has a primitive."

	^ (self headerFlagForEncoderClass: encoderClass)
		+ ((numArgs bitShift: -4) bitShift: 44)
		+ ((numTemps bitShift: -6) bitShift: 38)
		+ (frameSize > LargeFrame ifTrue: [1 bitShift: 37] ifFalse: [0])
		+ ((numArgs bitAnd: 16r0F) bitShift: 24)
		+ ((numTemps bitAnd: 16r3F) bitShift: 18)
		+ (frameSize > SmallFrame ifTrue: [1 bitShift: 17] ifFalse: [0])
		+ (primitiveBoolean ifTrue: [1 bitShift: 16] ifFalse: [0])
		+ numLits
		! !

!CompiledMethod class methodsFor: 'header manipulation' stamp: 'sss 14/Jan/2026 19:12:55'!
headerForNumArgs: numArgs numTemps: numTemps numLiterals: numLits frameSize: frameSize hasPrimitive: primitiveBoolean
	"Answer the method header value for the given argument, temporary and literal count, with the given frame size and whether the method has a primitive."

	^ self headerForEncoderClass: PreferredBytecodeSetEncoderClass numArgs: numArgs numTemps: numTemps numLiterals: numLits frameSize: frameSize hasPrimitive: primitiveBoolean
		! !


!Encoder methodsFor: 'temps' stamp: 'sss 14/Jan/2026 20:43:33'!
bindArg: name
	"Declare an argument."
	| node |
	nTemps >= self maxNumArgs
		ifTrue: [^self notify: 'Too many arguments'].
	node := self bindTemp: name.
	^ node nowHasDef nowHasRef! !


!BytecodeEncoder methodsFor: 'method generation' stamp: 'sss 14/Jan/2026 19:27:09'!
computeMethodHeaderForNumArgs: numArgs numTemps: numTemps numLits: numLits primitive: primitiveIndex

	self checkLimitsNumArgs: numArgs numTemps: numTemps numLits: numLits errorPrefix: 'Cannot compile'.

	^ CompiledMethod
		headerForEncoderClass: self class
		numArgs: numArgs
		numTemps: numTemps
		numLiterals: numLits
		frameSize: 0						"frameSize gets filled in later"
		hasPrimitive: primitiveIndex > 0
	! !


!EncoderForSistaV1 methodsFor: 'bytecode generation' stamp: 'sss 14/Jan/2026 20:49:09'!
genPushTemp: tempIndex
	(tempIndex < 0 or: [tempIndex > self maxNumTemps]) ifTrue:
		[^self outOfRangeError: 'index' index: tempIndex range: 0 to: self maxNumTemps].
	tempIndex < 12 ifTrue:
		["64-71		01000 i i i		Push Temporary Variable #iii
		   72-75	010010 i i		Push Temporary Variable #ii + 8"
		 stream nextPut: 64 + tempIndex.
		 ^self].
	"229		11100101	i i i i i i i i	Push Temporary Variable #iiiiiiii"
	stream
		nextPut: 229;
		nextPut: tempIndex! !

!EncoderForSistaV1 methodsFor: 'bytecode generation' stamp: 'sss 16/Jan/2026 20:48:50'!
genSend: selectorLiteralIndex numArgs: nArgs
	| extendedIndex extendedNArgs |
	(selectorLiteralIndex < 0 or: [selectorLiteralIndex > 65535]) ifTrue:
		[^self outOfRangeError: 'selectorLiteralIndex' index: selectorLiteralIndex range: 0 to: 65535].
	(nArgs < 0 or: [nArgs > self maxNumArgs]) ifTrue:
		[^self outOfRangeError: 'numArgs' index: nArgs range: 0 to: self maxNumArgs].
	(selectorLiteralIndex < 16 and: [nArgs < 3]) ifTrue:
	 	["128-143	1000 iiii			Send Literal Selector #iiii With 0 Argument
		  144-159	1001 iiii			Send Literal Selector #iiii With 1 Arguments
		  160-175	1010 iiii			Send Literal Selector #iiii With 2 Arguments"
		 stream nextPut: 128 + (nArgs * 16) + selectorLiteralIndex.
		 ^self].
	(extendedIndex := selectorLiteralIndex) > 31 ifTrue:
		[self genUnsignedMultipleExtendA: extendedIndex // 32.
		 extendedIndex := extendedIndex \\ 32].
	(extendedNArgs := nArgs) > 7 ifTrue:
		[self genUnsignedSingleExtendB: extendedNArgs // 8.
		 extendedNArgs := extendedNArgs \\ 8].
	"234		11101010	i i i i i j j j	Send Literal Selector #iiiii (+ Extend A * 32) with jjj (+ Extend B * 8) Arguments"
	stream
		nextPut: 234;
		nextPut: extendedNArgs + (extendedIndex * 8)! !

!CompiledMethod methodsFor: 'accessing' stamp: 'sss 14/Jan/2026 16:31:30'!
frameSize
	"Answer the size of temporary frame needed to run the receiver."
	"NOTE:  Versions 2.7 and later use two sizes of contexts."
	"NOTE: TruffleSqueak uses three sizes of contexts."

	(self header noMask: 16r2000000000)
		ifTrue: [
			(self header noMask: 16r20000)
				ifTrue: [^ SmallFrame]
				ifFalse: [^ LargeFrame]]
		ifFalse: [^ HugeFrame]
! !

!CompiledMethod methodsFor: 'accessing' stamp: 'sss 14/Jan/2026 17:52:30'!
numArgs
	"Answer the number of arguments the receiver takes."
	| header |

	header := self header.
	^ ((header bitShift: -24) bitAnd: 16r0F) + ((header bitShift: -40) bitAnd: 16r70)! !

!CompiledMethod methodsFor: 'accessing' stamp: 'sss 14/Jan/2026 17:54:59'!
numTemps
	"Answer the number of temporary variables used by the receiver."
	| header |

	header := self header.
	^ ((self header bitShift: -18) bitAnd: 16r3F) + ((header bitShift: -32) bitAnd: 16r40)! !

!CompiledMethod methodsFor: 'initialization' stamp: 'sss 14/Jan/2026 19:55:14'!
needsFrameSize: newFrameSize
	"Set the largeFrameBit and the hugeFrameBit to accomodate the newFrameSize"
	| largeFrameBit hugeFrameBit totalFrameSize header |

	largeFrameBit := 16r20000.
	hugeFrameBit := 16r2000000000.

	totalFrameSize := self numTemps + newFrameSize.

	totalFrameSize > HugeFrame ifTrue:
		[^ self error: 'Cannot compile -- stack including temps is too deep'].

	header := self objectAt: 1.
	(header bitAnd: hugeFrameBit) ~= 0
		ifTrue: [header := header - hugeFrameBit].
	(header bitAnd: largeFrameBit) ~= 0
		ifTrue: [header := header - largeFrameBit].

	"Does perform:withArguments: really need to reserve a maximum-size Context?"
	(totalFrameSize > LargeFrame or: [ self primitive = 84 "perform:withArguments:"])
		ifTrue: [header := header + hugeFrameBit].
	totalFrameSize > SmallFrame
		ifTrue: [header := header + largeFrameBit].

	self objectAt: 1 put: header.! !

!CompiledMethod class methodsFor: 'instance creation' stamp: 'sss 14/Jan/2026 16:26:26'!
newBytes: numberOfBytes trailerBytes: trailer nArgs: nArgs nTemps: nTemps nStack: stackSize nLits: nLits primitive: primitiveIndex
	"Answer an instance of me. The header is specified by the message
	 arguments. The remaining parts are not as yet determined."
	| method pc |

	self checkLimitsNumArgs: nArgs numTemps: nTemps numLits: nLits errorPrefix: 'Cannot compile'.

	method := self
				createMethod: numberOfBytes
				trailer: trailer
				header:
					(self
						headerForNumArgs: nArgs
						numTemps: nTemps
						numLiterals: nLits
						frameSize: nTemps + stackSize
						hasPrimitive: primitiveIndex > 0).
	primitiveIndex > 0 ifTrue: [
		pc := method initialPC.
		method
			at: pc + 0 put: method encoderClass callPrimitiveCode;
			at: pc + 1 put: (primitiveIndex bitAnd: 16rFF);
			at: pc + 2 put: (primitiveIndex bitShift: -8)].
	^method! !

!CompiledMethod class methodsFor: 'private' stamp: 'sss 14/Jan/2026 17:56:31'!
handleFailingNewMethod: numberOfBytes header: headerWord
	"This newMethod:header: gets sent after newMethod:header: has failed
	 and allowed a scavenging garbage collection to occur.  The scavenging
	 collection will have happened as the VM is activating the (failing) basicNew:.
	 If handleFailingBasicNew: fails then the scavenge failed to reclaim sufficient
	 space and a global garbage collection is required.  Retry after garbage
	 collecting and growing memory if necessary.

	 Primitive. Answer an instance of this class with the number of indexable variables
	 specified by the argument, headerWord, and the number of bytecodes specified
	 by numberOfBytes.  Fail if this if the arguments are not Integers, or if numberOfBytes
	 is negative, or if the receiver is not a CompiledMethod class, or if there is not enough
	 memory available. Essential. See Object documentation whatIsAPrimitive."

	<primitive: 79>
	| bytesRequested |
	bytesRequested := (headerWord bitAnd: 16r7FFF) + 1 * Smalltalk wordSize + numberOfBytes + 16.
	Smalltalk garbageCollect < bytesRequested ifTrue:
		[Smalltalk growMemoryByAtLeast: bytesRequested].
	"retry after global garbage collect and possible grow"
	^self handleFailingFailingNewMethod: numberOfBytes header: headerWord! !

!BytecodeEncoder methodsFor: 'temps' stamp: 'sss 18/Jan/2026 15:51:04'!
bindBlockArg: name within: aBlockNode
	| nArgs |
	(nArgs := aBlockNode nArgsSlot) isNil ifTrue:
		[aBlockNode nArgsSlot: (nArgs := 0)].
	nArgs >= self maxNumArgs ifTrue:
		[^self notify: 'Too many arguments'].
	aBlockNode nArgsSlot: nArgs + 1.
	^(self bindTemp: name)
		beBlockArg;
		nowHasDef;
		nowHasRef;
		yourself! !

!BytecodeEncoder methodsFor: 'temps' stamp: 'sss 18/Jan/2026 15:49:59'!
bindBlockTemp: name within: aBlockNode
	| nArgs |
	(nArgs := aBlockNode nArgsSlot) ifNil:
		[aBlockNode nArgsSlot: (nArgs := 0)].
	nArgs >= (self maxFrameSize - 1) ifTrue:
		[^self notify: 'Too many temporaries'].
	aBlockNode nArgsSlot: nArgs + 1.
	^self bindTemp: name! !


!EncoderForSistaV1 methodsFor: 'bytecode generation' stamp: 'sss 18/Jan/2026 19:32:47'!
genStorePopTemp: tempIndex
	"208-215	11010 iii			Pop and Store Temporary Variable #iii
	 242		11110010	iiiiiiii	Pop and Store Temporary Variable #iiiiiiii"
	(tempIndex < 0 or: [tempIndex > self maxNumTemps]) ifTrue:
		[^self outOfRangeError: 'index' index: tempIndex range: 0 to: self maxNumTemps].
	tempIndex < 8 ifTrue:
		[stream nextPut: 208 + tempIndex.
		 ^self].
	stream
		nextPut: 242;
		nextPut: tempIndex! !

!EncoderForSistaV1 methodsFor: 'bytecode generation' stamp: 'sss 18/Jan/2026 19:33:36'!
genStoreTemp: tempIndex
	"245		11110110	iiiiiiii		Store Temporary Variable #iiiiiiii"
	(tempIndex < 0 or: [tempIndex > self maxNumTemps]) ifTrue:
		[^self outOfRangeError: 'index' index: tempIndex range: 0 to: self maxNumTemps].
	stream
		nextPut: 245;
		nextPut: tempIndex! !


!methodRemoval: EncoderForSistaV1 #computeMethodHeaderForNumArgs:numTemps:numLits:primitive: stamp: 'sss 14/Jan/2026 19:28:44'!
EncoderForSistaV1 removeSelector: #computeMethodHeaderForNumArgs:numTemps:numLits:primitive:!
!classDefinition: #CompiledMethod category: #'Kernel-Methods'!
ByteArray variableByteSubclass: #CompiledMethod
	instanceVariableNames: ''
	classVariableNames: 'HugeFrame LargeFrame PreferredBytecodeSetEncoderClass SmallFrame'
	poolDictionaries: ''
	category: 'Kernel-Methods'!
CompiledMethod initialize!

!CompiledMethod class reorganize!
('class initialization' fullFrameSize hugeFrameSize initialize smallFrameSize)
('instance creation' basicNew: createMethod:trailer:header: new new: newBytes:trailerBytes:nArgs:nTemps:nStack:nLits:primitive: newFrom: newInstanceFrom:variable:size:map: newMethod:header: toReturnConstant:trailerBytes: toReturnField:trailerBytes: toReturnSelf toReturnSelfTrailerBytes:)
('constants' maxNumArgs maxNumLiterals maxNumTemps)
('accessing' preferredBytecodeSetEncoderClass)
('preferences' preferredBytecodeSetEncoderClass: useSistaBytecodes useV3Bytecodes)
('header manipulation' checkLimitsLegacyNumArgs:numTemps:numLits: checkLimitsLegacyNumArgs:numTemps:numLits:errorPrefix: checkLimitsNumArgs:numTemps:numLits: checkLimitsNumArgs:numTemps:numLits:errorPrefix: headerForEncoderClass:numArgs:numTemps:numLiterals:frameSize:hasPrimitive: headerForNumArgs:numTemps:numLiterals:frameSize:hasPrimitive:)
('method encoding' byteCodeSetsKnownToTheVM headerFlagForEncoderClass:)
('evaluating' receiver:withArguments:executeMethod:)
('services' authorAndStamp:partsDo: unboundMethods)
('source code management aux' classCategoryFrom: field:from:ifAbsentOrNil: libraryNameFrom: priorReferenceFrom: stampFrom:)
('private' handleFailingFailingNewMethod:header: handleFailingNewMethod:header:)
!

"Postscript: Recompile methods depending on primitive 84."

"Force larger frame size for those methods that need it."
(Smalltalk allSelect: [ :method | method primitive = 84 ])
	do: [ :methodReference | | method |
		method := methodReference compiledMethod.
		method needsFrameSize: method frameSize].!


'From Cuis7.3 [latest update: #7099] on 16 January 2026 at 4:42:26 pm'!
!classDefinition: #ExtendedCompilerTest category: #'BaseImageTests-Compiler'!
DynamicallyCodeCreationTest subclass: #ExtendedCompilerTest
	instanceVariableNames: ''
	classVariableNames: ''
	poolDictionaries: ''
	category: 'BaseImageTests-Compiler'!

!ExtendedCompilerTest methodsFor: 'tests' stamp: 'sss 18/Jan/2026 15:51:56'!
testLargeNumberOfBlockArguments
	| numArgs sourceCode testBlock testArguments |

	numArgs := 120.

	sourceCode :=
		String streamContents: [ :strm |
			strm nextPutAll: ' [ :index'.
			1 to: numArgs do: [ :i | strm nextPutAll: ' :a', i asString].
			strm nextPutAll: ' |'; lf; nextPutAll: '{ '.
			1 to: numArgs do: [ :i | strm nextPutAll: 'a', i asString, ' . '].
			strm nextPutAll: '} at: index]'].

	self
		shouldnt: [testBlock := Compiler evaluate: sourceCode for: nil logged: false]
		raise: SyntaxErrorNotification
		description: 'VM does not support extended block argument count'.

	testArguments := (1 to: numArgs) asArray.
	1 to: numArgs do: [ :i |
		self
			assert: (testBlock valueWithArguments: { i }, testArguments) = i
			description: 'Large block arg count valueWithArguments fails'].
	! !

!ExtendedCompilerTest methodsFor: 'tests' stamp: 'sss 18/Jan/2026 15:32:57'!
testLargeNumberOfMethodArguments
	| numArgs sourceCode testCode selector testClass testInstance testArguments |

	numArgs := 120.

	sourceCode :=
		String streamContents: [ :strm |
			strm nextPutAll: 'index: index'.
			1 to: numArgs do: [ :i | strm nextPutAll: ' a: a', i asString].
			strm lf.
			strm nextPutAll: '^ { '.
			1 to: numArgs do: [ :i | strm nextPutAll: 'a', i asString, ' . '].
			strm nextPutAll: '} at: index'].

	testCode :=
		String streamContents: [ :strm |
			strm nextPutAll: 'index: index arguments: args'.
			strm lf.
			strm nextPutAll: '^ self index: index'.
			1 to: numArgs do: [ :i | strm nextPutAll: ' a: (args at: ', i asString, ')']].

	selector :=
		(String streamContents: [ :strm |
			strm nextPutAll: 'index:'.
			1 to: numArgs do: [ :i | strm nextPutAll: 'a:']])
				asSymbol.

	testClass := self createTestClass.
	self
		shouldnt: [testClass compile: sourceCode; compile: testCode]
		raise: SyntaxErrorNotification
		description: 'VM does not support extended argument count'.

	testInstance := testClass new.
	testArguments := (1 to: numArgs) asArray.
	1 to: numArgs do: [ :i |
		self
			assert: (testInstance index: i arguments: testArguments) = i
			description: 'Large arg count message send fails'].

	1 to: numArgs do: [ :i |
		self
			assert: (testInstance perform: selector withArguments: { i }, testArguments) = i
			description: 'Large arg count perform fails'].
	! !

!ExtendedCompilerTest methodsFor: 'tests' stamp: 'sss 18/Jan/2026 19:41:06'!
testLargeNumberOfMethodTemporaries
	| numTemps sourceCode testClass testInstance testArguments |

	numTemps := 120.

	sourceCode :=
		String streamContents: [ :strm |
			strm nextPutAll: 'rotateCollection: aCollection'; lf; tab; nextPutAll: '|'.
			1 to: numTemps do: [ :i | strm nextPutAll: ' a', i asString].
			strm nextPutAll: ' temp |'; lf; lf.
			1 to: numTemps do: [ :i | strm tab; nextPutAll: ' a', i asString, ' := aCollection at: ', i asString, '.'; lf].
			strm lf.
			strm tab; nextPutAll: 'temp := a', numTemps printString, '.'; lf.
			numTemps to: 2 by: -1 do: [ :i | strm tab; nextPutAll: ' a', i asString, ' := a', ((i - 2 mod: numTemps) + 1) asString, '.'; lf].
			strm tab; nextPutAll: 'a1 := temp.'; lf.
			strm lf; nextPutAll: '^ { '.
			1 to: numTemps do: [ :i | strm nextPutAll: 'a', i asString, ' . '].
			strm nextPutAll: '}'].

	testClass := self createTestClass.
	self
		shouldnt: [testClass compile: sourceCode]
		raise: SyntaxErrorNotification
		description: 'VM does not support extended temporaries count'.

	testInstance := testClass new.
	testArguments := (1 to: numTemps) asArray.
	self
		assert: (testInstance rotateCollection: testArguments) = ({ numTemps }, (1 to: numTemps - 1) asArray)
		description: 'Large temp count method fails'.! !

!ExtendedCompilerTest methodsFor: 'tests' stamp: 'sss 18/Jan/2026 19:52:50'!
testLargeNumberOfBlockTemporaries
	| numTemps sourceCode testBlock testArguments |

	numTemps := 120.

	sourceCode :=
		String streamContents: [ :strm |
			strm nextPutAll: '[ :aCollection |'; lf; tab; nextPutAll: '|'.
			1 to: numTemps do: [ :i | strm nextPutAll: ' a', i asString].
			strm nextPutAll: ' temp |'; lf; lf.
			1 to: numTemps do: [ :i | strm tab; nextPutAll: ' a', i asString, ' := aCollection at: ', i asString, '.'; lf].
			strm lf.
			strm tab; nextPutAll: 'temp := a', numTemps printString, '.'; lf.
			numTemps to: 2 by: -1 do: [ :i | strm tab; nextPutAll: ' a', i asString, ' := a', ((i - 2 mod: numTemps) + 1) asString, '.'; lf].
			strm tab; nextPutAll: 'a1 := temp.'; lf.
			strm lf; nextPutAll: '{ '.
			1 to: numTemps do: [ :i | strm nextPutAll: 'a', i asString, ' . '].
			strm nextPutAll: '}]'].

	self
		shouldnt: [testBlock := Compiler evaluate: sourceCode for: nil logged: false]
		raise: SyntaxErrorNotification
		description: 'VM does not support extended block temporaries count'.

	testArguments := (1 to: numTemps) asArray.
	self
		assert: (testBlock value: testArguments) = ({ numTemps }, (1 to: numTemps - 1) asArray)
		description: 'Large temp count block fails'.! !


!ExtendedCompilerTest methodsFor: 'private' stamp: 'sss 15/Jan/2026 17:33:03'!
createTestClass

	^ self createClassNamed: #ExtendedCompilerTestDataClass__! !
