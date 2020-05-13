updating
adjustBackgroundColor

	"Make my background a little transparent to that you can see the editor color"
	self color: ((self userInterfaceTheme color ifNil: [Color white]) alpha: 0.8)