package com.sky.translate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.metadata.Sheet;
import com.alibaba.excel.support.ExcelTypeEnum;

/**
 * 由XML资源文件生成Excel文件
 * @author Jason
 */
public enum ExcelExtractor {
	INSTANCE;
	// 默认资源文件的相对路径
	private static final String PATH_RES = "res";
	private static final String PATH_DEFAULT_KEY_FILE_COMMON = PATH_RES + "/values/";
	private static final String PATH_DEFAULT_KEY_FILE_MMP = PATH_DEFAULT_KEY_FILE_COMMON + "mmp_new.xml";
	
	private static final int SECOND_MILLIS_SECONDS = 1000;
	private static final int MINUTE_MILLIS_SECONDS = 60 * SECOND_MILLIS_SECONDS;
	
	private String mCurrentPath;
	private Pattern mLinePattern;
	private Pattern mArrayPattern;
	private Pattern mValuePattern;
	private Pattern mItemPattern;
	private boolean mIsExtractingMMP;
	private boolean mCheckMultiple;
	
	private List<String> mKeyList;
	private Map<String, Map<String, String>> mAllLanKeyValueMap;
	private int mFileNumbers;
	private int mCurrentFileNumber;
	public void init(String path) {
		this.mCurrentPath = path;
	}
	
	/**
	 * 从默认的语言文件/res/values/mmp_string.xml获取字段，遍历获取其他语言的字段，一并输出到同一个Excel文件中
	 */
	public void extractToExcel(boolean isExtractingMMP, boolean checkMultiple) {
		this.mIsExtractingMMP = isExtractingMMP;
		this.mCheckMultiple = checkMultiple;
		long startTimeMillis = System.currentTimeMillis();
		
		// 获取所有默认的key-value文件字符串，MMP只从res/values/mmp_new.xml读取，其他项目则获取res/values/文件夹下所有的文件内容
		File defaultDictionaryFile;
		if (mIsExtractingMMP) {
			defaultDictionaryFile = new File(mCurrentPath, PATH_DEFAULT_KEY_FILE_MMP);
			PrintTools.INSTANCE.printlnNow("开始从默认的" + defaultDictionaryFile.getAbsolutePath() + "文件读取有效字段的key......");
			if (!defaultDictionaryFile.exists()) {
				PrintTools.INSTANCE.printlnNow("操作中断，未找到有效字库文件：" + PATH_DEFAULT_KEY_FILE_MMP);
				return;
			}
		} else {
			PrintTools.INSTANCE.printlnNow("开始从默认的" + PATH_DEFAULT_KEY_FILE_COMMON + "文件夹读取所有字段的key......");
			defaultDictionaryFile = new File(mCurrentPath, PATH_DEFAULT_KEY_FILE_COMMON);
			if (!defaultDictionaryFile.exists()) {
				PrintTools.INSTANCE.printlnNow("操作中断，未能在" + PATH_DEFAULT_KEY_FILE_COMMON + "文件夹找到有效字库文件");
				return;
			}
			File[] defaultXmlFiles = defaultDictionaryFile.listFiles();
			if (defaultXmlFiles == null || defaultXmlFiles.length == 0) {
				PrintTools.INSTANCE.printlnNow("操作中断，未能在" + PATH_DEFAULT_KEY_FILE_COMMON + "文件夹找到有效字库文件");
				return;
			}
		}
		
		// 查找行
		mLinePattern = Pattern.compile(TranslateConstant.REG_LINE);
		// 查找字符数组
		mArrayPattern = Pattern.compile(TranslateConstant.REG_ARRAY);
		// 查找value
		mValuePattern = Pattern.compile(TranslateConstant.REG_VALUE);
		// 查找字符数组的item
		mItemPattern = Pattern.compile(TranslateConstant.REG_ARRAY_ITEM);
		
		// 这些key是所有语言共有的
		mKeyList = new ArrayList<>();
		// 所有语言的key-value集合，其形式为：[语言文件夹， [键， 值]]
		mAllLanKeyValueMap = new HashMap<>();
		// 获取默认语言的key-value对和key集合
		Map<String, String> defaultKeyValue = getOneLanKeyValueFromRom(defaultDictionaryFile, mKeyList);
		if (mKeyList.size() == 0) {
			// 说明内容为空
			PrintTools.INSTANCE.printlnNow("没找到字库文件");
			return;
		}
		if (defaultDictionaryFile.isDirectory()) {
			mAllLanKeyValueMap.put(defaultDictionaryFile.getName(), defaultKeyValue);
		} else {
			mAllLanKeyValueMap.put(defaultDictionaryFile.getParentFile().getName(), defaultKeyValue);
		}
		
		// 计算要解析的文件总数
		File resFile = new File(mCurrentPath, PATH_RES);
		File[] valueFolders = resFile.listFiles();
		mFileNumbers = 0;
		mCurrentFileNumber = 0;
		List<File> lanFolderList = new ArrayList<>();
		for (File folder : valueFolders) {
			if (folder.isDirectory() && folder.getName().startsWith("values")) {
				lanFolderList.add(folder);
				File[] xmlFiles = folder.listFiles();
				if (xmlFiles != null) {
					mFileNumbers += xmlFiles.length;
				}
			}
		}
		PrintTools.INSTANCE.printlnNow("共有" + mFileNumbers + "个文件");
		
		for (File folder : lanFolderList) {
			// 依次进入各个语言的文件夹查找key对应的value并输出到Excel
			if (folder.getName().equals("values")) {
				mCurrentFileNumber += folder.listFiles().length;
				continue;
			}
			parseLanKeyValue(folder);
		}
		
		PrintTools.INSTANCE.printlnNow("解析完成，正在输出到Excel文件...");
		// 这里获取到了key和所有语言对应的value，现在把它们拼接输出为Excel
		File excelFile = new File(mCurrentPath, "out.xls");
		if (excelFile.exists()) {
			excelFile.delete();
		}
		
		// 所有行
		List<List<String>> excelLineList = new ArrayList<>();
		
		List<String> localeLine = new ArrayList<String>();
		// 第0行，第0列的格子为空，第0行，第1个开始有区域信息
		localeLine.add("");
		
		// 第一行(第0行)为空行，第二行（第1行）为语言对应的文件夹
		List<String> lanLine = new ArrayList<>();
		lanLine.add("");
		for (File folder : lanFolderList) {
			lanLine.add(folder.getName());
			if (MainActivity.mLocaleMap != null) {
				String localeName = MainActivity.mLocaleMap.get(folder.getName());
				if (localeName != null && !"".equals(localeName)) {
					localeLine.add(localeName);
				} else {
					localeLine.add("未知");
				}
			}
		}
		
		if (localeLine.size() > 0) {
			excelLineList.add(localeLine);
		}
		excelLineList.add(lanLine);
		
		// 逐行输出
		for (int i = 0, j = mKeyList.size(); i < j; i++) {
			List<String> excelLine = new ArrayList<>();
			String key = mKeyList.get(i);
			excelLine.add(key);
			for (File folder : lanFolderList) {
				Map<String, String> oneLanKeyValue = getOneLanKeyValueFromRam(folder.getName());
				if (oneLanKeyValue != null) {
					excelLine.add(oneLanKeyValue.get(key));
				} else {
					excelLine.add("");
				}
			}
			excelLineList.add(excelLine);
		}
		
		try {
			ExcelWriter writer = new ExcelWriter(new FileOutputStream(excelFile), ExcelTypeEnum.XLSX, false);
			Sheet sheet1 = new Sheet(1, 0);
			sheet1.setStartRow(-1); 
	        sheet1.setSheetName("全语言字库");
			
	        writer.write0(excelLineList, sheet1);
	        writer.finish();
		} catch (FileNotFoundException e) {
			PrintTools.INSTANCE.printlnNow("导出过程出错了" + e.toString());
			e.printStackTrace();
			return;
		}
		
		// 计算导出的耗时
		long time = System.currentTimeMillis() - startTimeMillis;
		int minute = 0;
		int second = 0;
		if (time > MINUTE_MILLIS_SECONDS) {
			minute = (int)(time / MINUTE_MILLIS_SECONDS);
			second = (int)(time % MINUTE_MILLIS_SECONDS);
		}

		if (time > SECOND_MILLIS_SECONDS) {
			second =  (int)((time) / SECOND_MILLIS_SECONDS);
		}
		
		PrintTools.INSTANCE.println("导出完成，共耗时：" + time + "毫秒，即约：" + minute + "分" + second + "秒");
		PrintTools.INSTANCE.println("输出路径为：" + excelFile.getAbsolutePath());
	}
	
	/**
	 * 根据语言的文件夹用正则从原生字符串获取相应语言的key-value对（用于解析获取默认key-value）
	 * @param lanFolder
	 * @param defaultKeyList
	 * @return
	 */
	private Map<String, String> getOneLanKeyValueFromRom(File lanFolder, List<String> defaultKeyList) {
		String rawContent = FileUtils.getStringFromFile(lanFolder);
		// 一种语言的value集合
		Map<String, String> oneLanKeyValue = new HashMap<>();
		
		// 查找字符串
		Matcher lineMatcher = mLinePattern.matcher(rawContent);
		while (lineMatcher.find()) {
			String lineStr = lineMatcher.group();
			
			// 得到key
			int keyStartIndex = lineStr.indexOf("\"") + 1;
			int keyEndIndex = lineStr.indexOf(">") - 1;
			String key = lineStr.substring(keyStartIndex, keyEndIndex);
			
			Matcher valueMatcher = mValuePattern.matcher(lineStr);
			if (valueMatcher.find()) {
				String tempValue = valueMatcher.group();
				// 去掉>和</string>，得到真正的value
				String realValue = tempValue.substring(1, tempValue.length()-9);
				
				if (mCheckMultiple && oneLanKeyValue.containsKey(key)) {
					PrintTools.INSTANCE.println("警告：语言" + lanFolder + "有重复的key：" + key);
				}
				oneLanKeyValue.put(key, realValue);
				
				// 这些key是所有语言共有的，暂存起来
				if (defaultKeyList != null) {
					defaultKeyList.add(key);
				}
			}
		}
		
		// 查找字符数组
		Matcher arrayMatcher = mArrayPattern.matcher(rawContent);
		while (arrayMatcher.find()) {
			String arrayStr = arrayMatcher.group();
			
			// 得到字符数组的key
			int arrayKeyStartIndex = arrayStr.indexOf("\"") + 1;
			int arrayKeyEndIndex = arrayStr.indexOf(">") - 1;
			String arrayKey = arrayStr.substring(arrayKeyStartIndex, arrayKeyEndIndex);
			
			if (arrayStr.contains("<!--")) {
				PrintTools.INSTANCE.println("错误：字符数组" + arrayKey + "包含注释，请去掉");
			}
			
			// 查找item
			Matcher itemMatcher = mItemPattern.matcher(arrayStr);
			
			while (itemMatcher.find()) {
				String itemStr = itemMatcher.group();
				int itemStartIndex = itemStr.indexOf(">") + 1;
				int itemEndIndex = itemStr.indexOf("</");
				String itemValue = itemStr.substring(itemStartIndex, itemEndIndex);
				// arraykey`[itemValue]作为唯一的key
				String specialArrayKey = arrayKey + TranslateConstant.REG_ITEM_SPLIT + itemValue;
				
				if (mCheckMultiple && oneLanKeyValue.containsKey(specialArrayKey)) {
					PrintTools.INSTANCE.println("警告：语言" + lanFolder + "有重复的字符串数组key：" + arrayKey);
				}
				oneLanKeyValue.put(specialArrayKey, itemValue);
				// 这些key是所有语言共有的，暂存起来
				if (defaultKeyList != null) {
					defaultKeyList.add(specialArrayKey);
				}
			}
		}
		
		return oneLanKeyValue;
	}
	
	/**
	 * 根据语言的文件夹从内存获取相应语言的key-value对
	 * @return
	 */
	private Map<String, String> getOneLanKeyValueFromRam(String lanFolderName) {
		if (mAllLanKeyValueMap != null) {
			return mAllLanKeyValueMap.get(lanFolderName);
		}
		return null;
	}

	private void parseLanKeyValue(File folder) {
		// 某种语言所有XML文件
		File[] lanXmlList = folder.listFiles();
		if (lanXmlList == null || lanXmlList.length == 0) {
			PrintTools.INSTANCE.println("警告：" + folder.getName() + "文件夹没有文件");
			return;
		}
		PrintTools.INSTANCE.printlnNow("已解析的文件数：" + mCurrentFileNumber + "，总：" + mFileNumbers);
		mCurrentFileNumber += lanXmlList.length;
		
		Map<String, String> oneLanKeyValue = getOneLanKeyValueFromRom(folder, null);
		mAllLanKeyValueMap.put(folder.getName(), oneLanKeyValue);
	}
}
