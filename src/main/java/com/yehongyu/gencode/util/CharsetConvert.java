package com.yehongyu.gencode.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.apache.log4j.Logger;

/**
 * 给定一个源文件路径或者File和目标路径
 * 把源文件路径下的的所有txt文件和文件夹拷贝到目标路径下 并且可以按照给定的编码输出txt文件
 * @author yingyang
 * @since 2011-11-17
 */
public class CharsetConvert {
	/** 日志记录 */
	private final static Logger logger = Logger.getLogger(CharsetConvert.class);
	
	private File inFilePath; // 输入路径
	private File outFilePath; // 输出路径
    private String inputencoding; // 输入文件编码
	private String outputencoding; // 输出文件编码
    private static String DEFAULT_INPUT_ENCODING = "GBK";    // 默认输入文件编码
	private static String DEFAULT_OUTPUT_ENCODING = "UTF-8";   // 默认输出文件编码
	
	public CharsetConvert(String inPath, String outPath){
		this(new File(inPath), new File(outPath), DEFAULT_INPUT_ENCODING , DEFAULT_OUTPUT_ENCODING);
	}

	public CharsetConvert(String inPath, String outPath, String inputencoding,String outputencoding){
		this(new File(inPath), new File(outPath),inputencoding, outputencoding);
	}

	public CharsetConvert(File inFilePath, File outFilePath){
		this(inFilePath, outFilePath,DEFAULT_INPUT_ENCODING, DEFAULT_OUTPUT_ENCODING);
	}

	public CharsetConvert(File inFilePath, File outFilePath, String inputencoding, String outputencoding){
		this.inFilePath = inFilePath;
		this.outFilePath = outFilePath;
		if (!outFilePath.exists()) {
			outFilePath.mkdir();
		}
        this.inputencoding = inputencoding;
		this.outputencoding = outputencoding;
	}
	
	public static void main(String[] args){
		String inpath = "D:\\IdeaWork\\codeutil\\src\\main\\java";
		String outpath = "D:\\IdeaWork\\codeutil\\src\\main\\java2";
		CharsetConvert convert = new CharsetConvert(inpath, outpath,"GBK","UTF-8");
		logger.error("进行编码转换！inpath[" + inpath + "]，outpath[" + outpath +"]。");
		logger.info(convert.convert());
	}

	public boolean convert(){
		return copyFiles(inFilePath, outFilePath);
	}

	/**
	 * 拷贝目录和文件
	 * @param inFile 需拷贝的文件或目录
	 * @param outDirectory 拷贝到的目录
	 * @throws Exception
	 */
	private boolean copyFiles(File inFile, File outDirectory){
		if(inFile.isFile()){	//拷贝文件
			return copySingleFile(inFile, new File(outDirectory.getAbsolutePath(),inFile.getName()));
		}else if(inFile.isDirectory()){	//拷贝目录及目录下的所有目录和文件
			outDirectory = new File(outDirectory.getAbsolutePath(),inFile.getName());
			if (!outDirectory.exists()) outDirectory.mkdirs();
			File[] files = inFile.listFiles();
			for (int i = 0; i < files.length; i++) {
				if(!copyFiles(files[i],outDirectory)){
					return false;
				}
			}
			return true;
		}
		return true;
	}

	/**
	 * 拷贝单个文件
	 * @param src 输入文件
	 * @param des 输出文件
	 * @return true or false
	 */
	private boolean copySingleFile(File src, File des){
		try {
			//创建目标文件
			if (!des.exists()) des.createNewFile();
            //设定输入文件的编码
            String encodingInput = ((inputencoding == null) ? DEFAULT_INPUT_ENCODING : inputencoding);
			//获取输入文件
			Reader in = new BufferedReader(new InputStreamReader(new FileInputStream(src),encodingInput));
			//设定输出文件的编码
			String encodingUse = ((outputencoding == null) ? DEFAULT_OUTPUT_ENCODING : outputencoding);
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(des), encodingUse));
			//设置读取缓冲区，并进行读取写入
			char[] buffer = new char[4096];
			int readBytes = -1;
			while ((readBytes = in.read(buffer)) != -1) {
				out.write(buffer, 0, readBytes);
			}
			//关闭输入输出流
			out.flush();
			out.close();
			in.close();
		} catch (IOException e) {
			logger.error("拷贝文本文件是发生错误，异常是:", e);
			return false;
		}
		return true;
	}

}