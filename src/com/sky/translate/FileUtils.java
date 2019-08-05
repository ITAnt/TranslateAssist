package com.sky.translate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class FileUtils {
	public static String getStringFromFile(File targetFile) {
		if (targetFile.isDirectory()) {
			File[] files = targetFile.listFiles();
			StringBuilder defaultBuilder = new StringBuilder();
			if (files != null && files.length > 0) {
				for (File file : files) {
					defaultBuilder.append(FileUtils.getStringFromFile(file)).append("\n");
				}
			}
			return defaultBuilder.toString();
			
		} else {
			StringBuilder inputBuilder = new StringBuilder();
			try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(targetFile), "UTF-8")))  {
				String line = null;
				while ((line = bufferedReader.readLine()) != null) {
					inputBuilder.append(line);
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			return inputBuilder.toString();
		}
	}
}