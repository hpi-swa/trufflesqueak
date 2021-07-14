message list
selectedBytecodes
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