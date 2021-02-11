instance creation
with: anObject name: aString model: aModel parent: aParent members: members

	^self new 
		setItem: anObject name: aString model: aModel parent: aParent;
		members: members
