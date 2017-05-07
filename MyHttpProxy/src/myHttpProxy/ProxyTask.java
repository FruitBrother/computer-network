package myHttpProxy;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 将客户端发送过来的数据转发给请求的服务器端，并将服务器返回的数据转发给客户端
 *
 */
public class ProxyTask implements Runnable {
	private Socket socketIn;
	private Socket socketOut;

	private long totalUpload = 0l;// 总计上行比特数
	private long totalDownload = 0l;// 总计下行比特数

	public ProxyTask(Socket socket) {
		this.socketIn = socket;
	}

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	/** 已连接到请求的服务器 */
	private static final String AUTHORED = "HTTP/1.1 200 Connection established\r\n\r\n";
	/** 本代理登陆失败(此应用暂时不涉及登陆操作) */
	// private static final String UNAUTHORED="HTTP/1.1 407
	// Unauthorized\r\n\r\n";
	/** 内部错误 */
	private static final String SERVERERROR = "HTTP/1.1 500 Connection FAILED\r\n\r\n";
	/** 屏蔽地址列表 */
	private static final String[] blockurl = { "www.hao123.com" };
	private static final String[] reurl = { "news.baidu.com" };

	@Override
	public void run() {

		StringBuilder builder = new StringBuilder();
		try {
			builder.append("\r\n").append("Request Time  ：" + sdf.format(new Date()));

			InputStream CtoP = socketIn.getInputStream();
			OutputStream PtoC = socketIn.getOutputStream();
			// 从客户端流数据中读取头部，获得请求主机和端口
			HttpHeader header = HttpHeader.readHeader(CtoP);

			// 添加请求日志信息
			builder.append("\r\n").append("From    Host  ：" + socketIn.getInetAddress());
			builder.append("\r\n").append("From    Port  ：" + socketIn.getPort());
			builder.append("\r\n").append("Proxy   Method：" + header.getMethod());
			builder.append("\r\n").append("Request Host  ：" + header.getHost());
			builder.append("\r\n").append("Request Port  ：" + header.getPort());

			// 如果没解析出请求请求地址和端口，则返回错误信息
			if (header.getHost() == null || header.getPort() == null) {
				PtoC.write(SERVERERROR.getBytes());
				PtoC.flush();
				return;
			}

			// 查找主机和端口
			socketOut = new Socket(header.getHost(), Integer.parseInt(header.getPort()));
			socketOut.setKeepAlive(true);
			InputStream StoP = socketOut.getInputStream();
			OutputStream PtoS = socketOut.getOutputStream();
			// 新开一个线程将返回的数据转发给客户端,串行会出问题，尚没搞明白原因
			Thread ot = new DataSendThread(StoP, PtoC);
			ot.start();
			boolean flagblock = false;
			boolean reblock = false;
			if (header.getMethod().equals(HttpHeader.METHOD_CONNECT)) {
				// 将已联通信号返回给请求页面
				PtoC.write(AUTHORED.getBytes());
				PtoC.flush();
			} else if (header.getMethod().equals(HttpHeader.METHOD_GET)) {
				for (int i = 0; i < blockurl.length; i++)
					if (header.getHost().equals(blockurl[i])) {
						flagblock = true;
						break;
					}
				if (flagblock) {
					System.out.println("block " + header.getHost());
					PtoC.write(SERVERERROR.getBytes());
					PtoC.flush();
				}

				for (int i = 0; i < reurl.length; i++)
					if (header.getHost().equals(reurl[i])) {
						reblock = true;
						break;
					}
				if (reblock) {
					System.out.println("redirect " + header.getHost());
					header.fish();
					
					URL url = new URL("http://www.sina.com.cn");
			        URLConnection urlcon = url.openConnection();
			        InputStream is = urlcon.getInputStream();
			        Thread op = new DataSendThread(is, PtoC);
					op.start();
				}
				
				if (!flagblock && !reblock) {
					// http请求需要将请求头部也转发出去
					System.out.println(header.toString());
					byte[] headerData = header.toString().getBytes();
					totalUpload += headerData.length;
					PtoS.write(headerData);
					PtoS.flush();
				}

			} else {
				// http请求需要将请求头部也转发出去
				System.out.println(header.toString());
				byte[] headerData = header.toString().getBytes();
				totalUpload += headerData.length;
				PtoS.write(headerData);
				PtoS.flush();
			}
			// 读取客户端请求过来的数据转发给服务器
			readForwardDate(CtoP, PtoS);
			// 等待向客户端转发的线程结束
			ot.join();
			//op.join();
		} catch (Exception e) {
			if (!e.toString().equals("java.net.ConnectException: Connection timed out: connect"))
				e.printStackTrace();
			if (!socketIn.isOutputShutdown()) {
				// 如果还可以返回错误状态的话，返回内部错误
				try {
					socketIn.getOutputStream().write(SERVERERROR.getBytes());
				} catch (IOException e1) {
				}
			}
		} finally {
			try {
				if (socketIn != null) {
					socketIn.close();
				}
			} catch (IOException e) {
			}
			if (socketOut != null) {
				try {
					socketOut.close();
				} catch (IOException e) {
				}
			}
			// 纪录上下行数据量和最后结束时间并打印
			builder.append("\r\n").append("Up    Bytes  ：" + totalUpload);
			builder.append("\r\n").append("Down  Bytes  ：" + totalDownload);
			builder.append("\r\n").append("Closed Time  ：" + sdf.format(new Date()));
			builder.append("\r\n");
			// logRequestMsg(builder.toString());
		}
	}

	/**
	 * 避免多线程竞争把日志打串行了
	 * 
	 * @param msg
	 */
	private synchronized void logRequestMsg(String msg) {
		System.out.println(msg);
	}

	/**
	 * 读取客户端发送过来的数据，发送给服务器端
	 * 
	 * @param CtoP
	 * @param PtoS
	 */
	private void readForwardDate(InputStream CtoP, OutputStream PtoS) {
		byte[] buffer = new byte[4096];
		try {
			int len;
			while ((len = CtoP.read(buffer)) != -1) {
				if (len > 0) {
					PtoS.write(buffer, 0, len);
					PtoS.flush();
				}
				totalUpload += len;
				if (socketIn.isClosed() || socketOut.isClosed()) {
					break;
				}
			}
		} catch (Exception e) {
			try {
				socketOut.close();// 尝试关闭远程服务器连接，中断转发线程的读阻塞状态
			} catch (IOException e1) {
			}
		}
	}

	/**
	 * 将服务器端返回的数据转发给客户端
	 * 
	 * @param StoP
	 * @param PtoC
	 */
	class DataSendThread extends Thread {
		private InputStream StoP;
		private OutputStream PtoC;
		private HttpHeader header;

		DataSendThread(InputStream StoP, OutputStream PtoC) {
			this.StoP = StoP;
			this.PtoC = PtoC;
		}

		@Override
		public void run() {
			byte[] buffer = new byte[4096];
			try {
				int len;
				while ((len = StoP.read(buffer)) != -1) {
					if (len > 0) {
						// logData(buffer, 0, len);
						PtoC.write(buffer, 0, len);
						PtoC.flush();
						totalDownload += len;
					}
					if (socketIn.isOutputShutdown() || socketOut.isClosed()) {
						break;
					}
				}
			} catch (Exception e) {
			}
		}
		
		public void run1() {
			char c = 0;
			StringBuilder sb = new StringBuilder();
			try {
				while (true) {
					while ((c = (char) StoP.read()) != '\n') {
						sb.append(c);
						if (sb.length() == 4096) {// 不接受过长的头部字段
							break;
						}
					}
					System.out.println(sb.toString());
					if (socketIn.isOutputShutdown() || socketOut.isClosed()) {
						break;
					}
				}
			} catch (Exception e) {
			}
		}

	}

}