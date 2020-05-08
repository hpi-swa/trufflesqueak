testing
testJavaString
	{65. "$A" 16r1f43b "Bear Emoji"} do: [ :ea | 
		self assert: (String with: (Character value: ea))
			equals: ((Java type: 'java.lang.String') new: ((Java type: 'java.lang.Character') toChars: ea)) ]