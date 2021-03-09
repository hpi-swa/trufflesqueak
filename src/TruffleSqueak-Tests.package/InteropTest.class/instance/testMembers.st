testing
testMembers
	{nil. #(). 'foo'. Object new} do: [ :ea |
		self assert: (Interop hasMembers: ea).
		(Interop getMembers: ea) do: [ :member |
			self assert: (Interop isMemberInvocable: ea member: member) ] ].
	
	self assert: (Interop invokeMember: #(Smalltalk) member: 'includes' arguments: #(Smalltalk))
