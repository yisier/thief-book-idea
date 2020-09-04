package com.thief.idea;

import java.io.*;

/**
 * @description:
 * @program: com.thief
 * @author: youvy
 * @create: 2019-12-29 10:47
 **/
public class RemoveBlank {
	/*
	 *  删除txt文档空白行
	 * @author WTCLAB_yd
	 */
		public static void main(String[] args)
		{
			String line=null;
			int i=0;
			File file=new File("C:\\Users\\BestN\\Downloads\\九星毒奶.txt");//用命令行参数直接写入待处理文件
			File file1=new File("C:\\Users\\BestN\\Downloads\\九星毒奶DBLine.txt");

				//判断输入的文档是否存在，不存在则提示退出
				if(!file.exists())
				{
					System.out.println("文件不存在！");
					System.exit(0);
				}
				//输入的是TXT文档则继续往下执行
				try
				{
					//读出文档数据流方式
					InputStreamReader Stream=new InputStreamReader(new FileInputStream(file),"gbk");//读入数据流方式设为‘UTF-8’，避免乱码
					//构造一个字符流的缓存器，存放在控制台输入的字节转换后成的字符
					BufferedReader reader=new BufferedReader(Stream);
					//写入数据流方式
					OutputStreamWriter outStream=new OutputStreamWriter(new FileOutputStream(file1),"gbk");
					BufferedWriter writer=new BufferedWriter(outStream);
					//以行读出文档内容至结束
					while((line=reader.readLine())!=null)
					{
						if(line.equals(""))//判断是否是空行
						{
							continue;//是空行则不进行操作
						}
						else
						{
							i++;
							writer.write("["+i+"]");//可在文档中标出行号
							writer.write(line+"\r\n");//将非空白行写入新文档
						}
					}
					//关闭数据流
					writer.close();
					reader.close();
					System.out.println("修改完成！");
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}

//	public static void main(String[] args) {
//		System.out.println("\uD83D\uDD04");
//	}
}
