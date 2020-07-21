selecting
calculateOptionalFields
	optionalFields := #().
	(self hasMembers: object) ifTrue: [ optionalFields := optionalFields, #('members') ].
	(Interop hasSourceLocation: object) ifTrue: [ optionalFields := optionalFields, #('source') ]