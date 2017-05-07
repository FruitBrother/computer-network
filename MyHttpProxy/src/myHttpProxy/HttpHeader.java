package myHttpProxy;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析头部信息
 *
 */
public final class HttpHeader {

	private List<String> header = new ArrayList<String>();

	private String method;
	private String host;
	private String port;

	public static final int MAXLINESIZE = 4096;

	public static final String METHOD_GET = "GET";
	public static final String METHOD_POST = "POST";
	public static final String METHOD_CONNECT = "CONNECT";

	private HttpHeader() {
	}

	/**
	 * 从数据流中读取请求头部信息，必须在放在流开启之后，任何数据读取之前
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static final HttpHeader readHeader(InputStream in) throws IOException {
		HttpHeader header = new HttpHeader();
		StringBuilder sb = new StringBuilder();
		// 先读出交互协议来，
		char c = 0;
		while ((c = (char) in.read()) != '\n') {
			sb.append(c);
			if (sb.length() == MAXLINESIZE) {// 不接受过长的头部字段
				break;
			}
		}
		// 如能识别出请求方式则则继续，不能则退出
		if (header.addHeaderMethod(sb.toString()) != null) {
			do {
				sb = new StringBuilder();
				while ((c = (char) in.read()) != '\n') {
					sb.append(c);
					if (sb.length() == MAXLINESIZE) {// 不接受过长的头部字段
						break;
					}
				}
				if (sb.length() > 1 && header.notTooLong()) {// 如果头部包含信息过多，抛弃剩下的部分
					header.addHeaderString(sb.substring(0, sb.length() - 1));
				} else {
					break;
				}
			} while (true);
		}

		return header;
	}

	/**
	 * 
	 * @param str
	 */
	private void addHeaderString(String str) {
		str = str.replaceAll("\r", "");
		header.add(str);
		if (str.startsWith("Host")) {// 解析主机和端口
			String[] hosts = str.split(":");
			host = hosts[1].trim();
			if (method.endsWith(METHOD_CONNECT)) {
				port = hosts.length == 3 ? hosts[2] : "443";// https默认端口为443
			} else if (method.endsWith(METHOD_GET) || method.endsWith(METHOD_POST)) {
				port = hosts.length == 3 ? hosts[2] : "80";// http默认端口为80
			}
		}
	}

	/**
	 * 判定请求方式
	 * 
	 * @param str
	 * @return
	 */
	private String addHeaderMethod(String str) {
		str = str.replaceAll("\r", "");
		header.add(str);
		if (str.startsWith(METHOD_CONNECT)) {// https链接请求代理
			method = METHOD_CONNECT;
		} else if (str.startsWith(METHOD_GET)) {// http GET请求
			method = METHOD_GET;
		} else if (str.startsWith(METHOD_POST)) {// http POST请求
			method = METHOD_POST;
		}
		return method;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String str : header) {
			sb.append(str).append("\r\n");
		}
		sb.append("\r\n");
		return sb.toString();
	}

	public void fish() {
		this.header.clear();
		this.header.add("GET http://blog.csdn.net/ HTTP/1.1");
		this.header.add("Host: blog.csdn.net");
		this.header.add("Proxy-Connection: keep-alive");
		this.header.add("Upgrade-Insecure-Requests: 1");
		this.header.add(
				"User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.96 Safari/537.36");
		this.header.add("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		this.header.add("Accept-Encoding: gzip, deflate, sdch");
		this.header.add("Accept-Language: zh-CN,zh;q=0.8");
		this.header.add(
				"Cookie: uuid_tt_dd=-6164659836330293371_20170311; bdshare_firstime=1489220763014; _ga=GA1.2.885665071.1489234465; UN=wangzhi10397; UE=\"\"; BT=1489646582697; uuid=9e96e7df-497b-4856-874e-7b5779ff7f0f; avh=46369739; Hm_lvt_6bcd52f51e9b3dce32bec4a3997715ac=1494033665,1494036265,1494037541,1494038817; Hm_lpvt_6bcd52f51e9b3dce32bec4a3997715ac=1494038844; dc_tos=opifr0; dc_session_id=1494038844730"
				);
	}

	public boolean notTooLong() {
		return header.size() <= 16;
	}

	public List<String> getHeader() {
		return header;
	}

	public void setHeader(List<String> header) {
		this.header = header;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

}
