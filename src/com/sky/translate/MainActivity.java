package com.sky.translate;

import java.awt.Color;
import java.awt.TextArea;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class MainActivity {
	private static boolean mIsDoing = false;
	public static void main(String[] args) {
		File file = new File("temp");
		String currentPath = file.getAbsoluteFile().getParentFile().getAbsolutePath();
		StringBuilder outBuilder = new StringBuilder();

		TextArea outLabel = new TextArea("", 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY);
		outLabel.setBackground(Color.black);
	
		PrintTools.INSTANCE.init(outBuilder, outLabel);
		ExcelExtractor.INSTANCE.init(currentPath);
		XmlGenerator.INSTANCE.init(currentPath);
		
		readLocales();
		
		JFrame jFrame = new JFrame();
		jFrame.setTitle("翻译工具2.0");
		jFrame.setLocationRelativeTo(null); 
		jFrame.setResizable(false); 
		jFrame.setSize(800, 600);
		jFrame.setVisible(true);
		jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jFrame.setLocationRelativeTo(jFrame.getOwner()); 
		jFrame.setLayout(null);
		
		int y_level_1 = 20;
		int y_level_2 = y_level_1 + 40;
		int y_level_3 = y_level_2 + 40;
		int y_level_4 = y_level_3 + 40;
		int y_level_5 = y_level_4 + 40;
		int y_level_6 = y_level_5 + 40;
		
		JButton readMeButton = new JButton();
		readMeButton.setSize(200, 25);
		readMeButton.setText("用前必读");
		readMeButton.setLocation(20, y_level_1);
		readMeButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				super.mouseClicked(arg0);
				PrintTools.INSTANCE.printlnNow(TranslateConstant.READ_ME);
			}
		});
		jFrame.add(readMeButton);
		
		JLabel operationLabel = new JLabel();
		operationLabel.setSize(760, 25);
		operationLabel.setText("请拷贝工程的res目录到本应用同一目录下再执行以下操作");
		operationLabel.setOpaque(true);
		operationLabel.setBackground(Color.red);
		operationLabel.setForeground(Color.white);
		operationLabel.setLocation(20, y_level_2);
		jFrame.add(operationLabel);
		
		// 是否检查重复字符串
		JCheckBox checkBox = new JCheckBox("检查重复字符串");
		checkBox.setLocation(450, y_level_3);
		checkBox.setSize(150, 25);
		jFrame.add(checkBox);
		
		// 提取字库到Excel
		JButton extractMMPButton = new JButton();
		extractMMPButton.setSize(200, 25);
		extractMMPButton.setText("提取MMP项目字库到out.xls");
		extractMMPButton.setLocation(20, y_level_3);
		extractMMPButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				super.mouseClicked(arg0);
				if (mIsDoing) {
					PrintTools.INSTANCE.printlnNow("请等待上一任务完成再操作");
					return;
				}
				PrintTools.INSTANCE.clearLog();
				mIsDoing = true;
				
				SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
					 
		            @Override
		            protected Void doInBackground() throws Exception {             
		            	ExcelExtractor.INSTANCE.extractToExcel(true, checkBox.isSelected());
		                return null;
		            }
		            
		            @Override
		            public void done() {
		            	mIsDoing = false;
		            }
		        };
		        worker.execute();
				
			}
		});
		jFrame.add(extractMMPButton);
		
		// 提取其他项目字库到Excel
		JButton extractOtherButton = new JButton();
		extractOtherButton.setSize(200, 25);
		extractOtherButton.setText("提取其他项目字库到out.xls");
		extractOtherButton.setLocation(240, y_level_3);
		extractOtherButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				super.mouseClicked(arg0);
				if (mIsDoing) {
					PrintTools.INSTANCE.printlnNow("请等待上一任务完成再操作");
					return;
				}
				PrintTools.INSTANCE.clearLog();
				mIsDoing = true;
				
				SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
					 
		            @Override
		            protected Void doInBackground() throws Exception {             
		            	ExcelExtractor.INSTANCE.extractToExcel(false, checkBox.isSelected());
		                return null;
		            }
		            
		            @Override
		            public void done() {
		            	mIsDoing = false;
		            }
		        };
		        worker.execute();
			}
		});
		jFrame.add(extractOtherButton);
		
		JLabel operationLabel2 = new JLabel();
		operationLabel2.setSize(760, 25);
		operationLabel2.setText("请拷贝Excel字库到本应用同一目录下再执行以下操作；如果不输入文件名，则默认输出为out.xml");
		operationLabel2.setOpaque(true);
		operationLabel2.setBackground(Color.red);
		operationLabel2.setForeground(Color.white);
		operationLabel2.setLocation(20, y_level_4);
		jFrame.add(operationLabel2);
		
		JTextField textField = new JTextField();
		textField.setSize(200, 25);
		textField.setLocation(20, y_level_5);
		textField.setEditable(true);
		jFrame.add(textField);
		
		// 是否输出空字符串提醒
		JCheckBox checkEmptyBox = new JCheckBox("空字符校验（可能很慢）");
		checkEmptyBox.setLocation(580, y_level_5);
		checkEmptyBox.setSize(180, 25);
		jFrame.add(checkEmptyBox);
		
		// 读取Excel文件中的字库，遍历查找并删除原res对应的字段（整个item都删除），然后把从Excel读取到的字段插入到mmp_final.xml中
		JButton genButton = new JButton();
		genButton.setSize(320, 25);
		genButton.setText("从Excel提取字库并输出到res对应语言文件夹里");
		genButton.setLocation(240, y_level_5);
		genButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				super.mouseClicked(arg0);
				if (mIsDoing) {
					PrintTools.INSTANCE.printlnNow("请等待上一任务完成再操作");
					return;
				}
				PrintTools.INSTANCE.clearLog();
				mIsDoing = true;
				
				SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
					 
		            @Override
		            protected Void doInBackground() throws Exception {             
		            	XmlGenerator.INSTANCE.genXmlFromExcel(textField.getText(), checkEmptyBox.isSelected());
		                return null;
		            }
		            
		            @Override
		            public void done() {
		            	mIsDoing = false;
		            }
		        };
		        worker.execute();
			}
		});
		jFrame.add(genButton);
		
		// 输出的信息
		outLabel.setSize(768, 330);
		outLabel.setLocation(16, y_level_6);
		outLabel.setEditable(false);
		outLabel.setForeground(Color.white);
		jFrame.add(outLabel);
		
		PrintTools.INSTANCE.printlnNow("当前路径：" + currentPath);
		
		operationLabel.repaint();
		extractMMPButton.repaint();
		extractOtherButton.repaint();
		checkBox.repaint();
		operationLabel2.repaint();
		genButton.repaint();
		textField.repaint();
		readMeButton.repaint();
		checkEmptyBox.repaint();
    }
	
	public static Map<String, String> mLocaleMap;
	// 读取locale区域文件
	private static void readLocales() {
		InputStream inputStream = MainActivity.class.getResourceAsStream("/locales.xlsx");
		if (inputStream == null) {
			PrintTools.INSTANCE.println("区域表不存在");
			return;
		}
		
		mLocaleMap = new HashedMap<String, String>();

		XSSFWorkbook workbook = null;
        try {
        	workbook = new XSSFWorkbook(inputStream);
            // 获得工作表
            XSSFSheet sheet = workbook.getSheetAt(0);
            int rows = sheet.getPhysicalNumberOfRows();
            
            // 获取key列表，第三行开始有key
            for (int i = 0; i < rows; i++) {
                // 获取第i行数据
                XSSFRow sheetRow = sheet.getRow(i);
                // 获取每行第1格和第2格数据
                XSSFCell key = sheetRow.getCell(1);
                XSSFCell value = sheetRow.getCell(2);
                mLocaleMap.put(key.toString(), value.toString());
            }
        } catch (Exception e) {
        	e.printStackTrace();
        	PrintTools.INSTANCE.println("区域表不存在");
        	return;
        } finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if (workbook != null) {
				try {
					workbook.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
        PrintTools.INSTANCE.println("区域表读取完毕");
	}
}