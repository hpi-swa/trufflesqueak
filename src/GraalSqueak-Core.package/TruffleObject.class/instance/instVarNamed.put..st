system primitives
instVarNamed: aName put: aValue
	^ Interop writeMember: self member: aName value: aValue