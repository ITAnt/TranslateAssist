package com.sky.translate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Excel文件转XML资源文件
 * @author Jason
 *
 */
public enum XmlGenerator {
	INSTANCE;
	private static final int SECOND_MILLIS_SECONDS = 1000;
	private static final int MINUTE_MILLIS_SECONDS = 60 * SECOND_MILLIS_SECONDS;
	
	private String mCurrentPath;
	private String mOutFileName = "out.xml";
	private boolean mHasEmptyError = false;
	private boolean mCheckEmptyStr = false;
	public void init(String path) {
		this.mCurrentPath = path;
	}
	
	/**
	 * 客户编辑好的Excel字库获取key和各种语言的value，输出到对应的/res/对应的文件夹的out.xml文件里
	 */
	public void genXmlFromExcel(String outFileName, boolean checkEmptyStr) {
		mCheckEmptyStr = checkEmptyStr;
		mHasEmptyError = false;
		if (outFileName != null && !outFileName.equals("")) {
			mOutFileName = outFileName;
		} else {
			mOutFileName = "out.xml";
		}
		long startTimeMillis = System.currentTimeMillis();
		PrintTools.INSTANCE.printlnNow("开始从Excel读取文件到/res/对应的文件夹的" + mOutFileName + "文件里...");
		File currentFile = new File(mCurrentPath);
		File[] currentFileList = currentFile.listFiles();
		if (currentFileList == null || currentFileList.length == 0) {
			return;
		}
		
		int excelFileNum = 0;
		File inputExcelFile = null;
		for (File file : currentFileList) {
			String fileName = file.getName();
			if (fileName.endsWith(".xls") || fileName.endsWith("xlsx")) {
				inputExcelFile = file; 
				excelFileNum++;
			}
		}
		
		if (excelFileNum == 0) {
			PrintTools.INSTANCE.printlnNow("错误：当前文件夹没有.xls或.xlsx格式的Excel文件：" + mCurrentPath);
			return;
		}
		
		if (excelFileNum > 1) {
			PrintTools.INSTANCE.printlnNow("错误：当前文件夹有多个.xls或.xlsx格式的Excel文件：" + mCurrentPath);
			return;
		}
		
		XSSFWorkbook workbook = null;
        try {
        	workbook = new XSSFWorkbook(new FileInputStream(inputExcelFile));
            // 获得工作表
            XSSFSheet sheet = workbook.getSheetAt(0);
            int rows = sheet.getPhysicalNumberOfRows();
            
            if (rows <3) {
            	PrintTools.INSTANCE.printlnNow("Excel文件内容有误");
            	return;
            }
            
            // 获取key列表，第三行(即第2行)开始有key
            List<String> keyList = new ArrayList<>();
            for (int i = 2; i < rows; i++) {
                // 获取第i行数据
                XSSFRow sheetRow = sheet.getRow(i);
                // 获取第0格数据
                XSSFCell cell = sheetRow.getCell(0);
                // 调用toString方法获取内容
                keyList.add(cell.toString());
            }
            
            // [语言，该语言下的字段集合]
            Map<String, StringBuilder> lanLineMap = new HashMap<>();
            List<String> lanList = new ArrayList<>();
            // 获取第二行数据，即语言文件夹
            XSSFRow lanRow = sheet.getRow(1);
            // 语言列数
            int lanColumnNum = lanRow.getLastCellNum();
            // 第二行，第二列开始是语言文件
            for (int i = 1; i < lanColumnNum; i++) {
            	XSSFCell cell = lanRow.getCell(i);
            	String lan = cell.toString();
            	lanList.add(lan);
            	StringBuilder lineBuilder = new StringBuilder();
            	// XML文件前面部分
            	lineBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n");
            	lanLineMap.put(lan, lineBuilder);
            }
            
            //[语言], [[字符数组名], 字符数组值]
            Map<String, Map<String, StringBuilder>> lanArrayMap = new HashMap<>();
            for (int i = 2; i < rows; i++) {
                // 获取第i行数据
                XSSFRow keyValueRow = sheet.getRow(i);
                int valueNum = keyValueRow.getLastCellNum();
                String key = keyList.get(i-2);
                if (key == null || key.equals("")) {
            		PrintTools.INSTANCE.println("错误：有空的key，无法生成正确的字符串");
            		continue;
            	}
                
                if (key.contains(TranslateConstant.REG_ITEM_SPLIT)) {
                	for (int j = 1; j < valueNum; j++) {
	                	String lan = lanList.get(j-1);
	                	String value = keyValueRow.getCell(j).toString();
	                	
	                	Map<String, StringBuilder> arrayMap = lanArrayMap.get(lan);
	                	if (arrayMap == null) {
	                		arrayMap = new HashMap<>();
	                	}
	                	
						// 这是一个字符数组
	                	String arrayKey = key.split(TranslateConstant.REG_ITEM_SPLIT)[0];
	                	StringBuilder arrayValueBuilder = arrayMap.get(arrayKey);
	                	if (arrayValueBuilder == null) {
	                		arrayValueBuilder = new StringBuilder();
	                	}
	                	
	                	arrayValueBuilder.append(TranslateConstant.SPACE_BIG)
	                	.append(TranslateConstant.SPACE_MIDDLE)
	                	.append("<item>")
	                	.append(value)
	                	.append("</item>\n");
						arrayMap.put(arrayKey, arrayValueBuilder);
						lanArrayMap.put(lan, arrayMap);
	                	
	                	if (value == null || value.equals("")) {
	                		mHasEmptyError = true;
	                		if (mCheckEmptyStr) {
	                			PrintTools.INSTANCE.print("警告：语言");
		                		PrintTools.INSTANCE.print(lan);
		                		PrintTools.INSTANCE.print("的");
		                		PrintTools.INSTANCE.print(key);
		                		PrintTools.INSTANCE.println("字段没有内容");
	                		}
	                	} else {
							if (value.contains("\n")) {
								PrintTools.INSTANCE.println(lan + "的字符串数组" + arrayKey + "有换行符，请手动处理");
							}
						}
	                }
				} else {
					// 普通的字符串
					for (int j = 1; j < valueNum; j++) {
	                	String lan = lanList.get(j-1);
	                	String value = keyValueRow.getCell(j).toString();
	                	
	                	if (value == null || value.equals("")) {
	                		mHasEmptyError = true;
	                		if (mCheckEmptyStr) {
	                			PrintTools.INSTANCE.print("警告：语言");
		                		PrintTools.INSTANCE.print(lan);
		                		PrintTools.INSTANCE.print("的");
		                		PrintTools.INSTANCE.print(key);
		                		PrintTools.INSTANCE.println("字段没有内容");
	                		}
	                	} else {
	                		PrintTools.INSTANCE.println(lan + "的字符串" + key + "有换行符，请手动处理");
						}
	                	
	                	StringBuilder lineBuilder = lanLineMap.get(lan);
	                	if (lineBuilder != null) {
	                		lineBuilder.append(TranslateConstant.SPACE_BIG)
	                		.append("<string name=\"")
	                		.append(key)
	                		.append("\">")
	                		.append(value)
	                		.append("</string>\n");
	                	}
	                }
				}
            }
            
            // 这个时候要把字符数组拼接成整体进行输出
            for (String lan : lanList) {
        		StringBuilder lineBuilder = lanLineMap.get(lan);
            	if (lineBuilder != null) {
            		Map<String, StringBuilder> arrayMap = lanArrayMap.get(lan);
            		if (arrayMap != null) {
            			for (String arrayKey : arrayMap.keySet()) {
            				StringBuilder itemValues = arrayMap.get(arrayKey);
                			if (itemValues != null) {
                				lineBuilder.append(TranslateConstant.SPACE_BIG)
                				.append("<string-array name=")
                				.append(arrayKey)
                				.append("\">")
                				.append("\n")
                				.append(itemValues.toString())
                				.append(TranslateConstant.SPACE_BIG)
                				.append("</string-array>\n\n");
                			}
            			}
            		}
            	}
        	}
            
            // 此时lanLineMap已经获取到所有的数据了，写入到文件中
            for (String lan : lanList) {
            	StringBuilder lineBuilder = lanLineMap.get(lan);
            	if (lineBuilder != null) {
            		// XML资源文件后面部分
            		lineBuilder.append("</resources>");
            		String lines = lineBuilder.toString();
            		writeToXml(lan, lines);
            	} else {
            		mHasEmptyError = true;
            		if (mCheckEmptyStr) {
            			PrintTools.INSTANCE.print("警告：语言");
	            		PrintTools.INSTANCE.print(lan);
	            		PrintTools.INSTANCE.println("没有内容");
            		}
				}
            }
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	if (workbook != null) {
        		try {
        			workbook.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
        	}
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
		if (mHasEmptyError) {
			PrintTools.INSTANCE.println("错误：您的表格有一些key没有对应的值，请填写完毕再导出");
		}
	}

	private static final String PATH_RES = "/res/";
	private void writeToXml(String lan, String lines) {
		// TODO Auto-generated method stub
		FileOutputStream writerStream;
		BufferedWriter tempWriter = null;
		File file = new File(mCurrentPath, PATH_RES+lan+"/" + mOutFileName);
		file.getParentFile().mkdirs();
		if (file.exists()) {
			file.delete();
		}
		try {
			writerStream = new FileOutputStream(file, false);
			tempWriter = new BufferedWriter(new OutputStreamWriter(writerStream, "UTF-8"));
			tempWriter.write(lines);
			tempWriter.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (tempWriter != null) {
				try {
					tempWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}