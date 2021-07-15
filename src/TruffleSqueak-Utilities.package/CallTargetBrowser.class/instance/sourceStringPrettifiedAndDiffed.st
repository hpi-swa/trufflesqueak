message list
sourceStringPrettifiedAndDiffed
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