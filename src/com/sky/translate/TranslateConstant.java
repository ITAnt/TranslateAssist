package com.sky.translate;

public class TranslateConstant {
	public static final String REG_LINE = "<string name=[^>]*?>[\\s\\S]*?</string>";
	public static final String REG_VALUE = ">[\\s\\S]*?</string>";
	
	public static final String REG_ARRAY = "<string-array name=[^>]*?>[\\s\\S]*?</string-array>";
	public static final String REG_ARRAY_ITEM = "<item>[\\s\\S]*?</item>";
	
	public static final String REG_ITEM_SPLIT = "`";
	
	public static final String READ_ME = "-------------------翻译工具2.0使用说明-------------------\r\n" + 
			"使用准备工作：\r\n" + 
			"1. Java JDK环境1.7以上\r\n" + 
			"2. 不要更改Excel的结构，即：如果需要增加语言，直接在最后一列的下一列添加，如果需要增加key-value，直接在最后一行的下一行对应添加即可。\r\n" + 
			"------------------------------------------------------------------";
}
