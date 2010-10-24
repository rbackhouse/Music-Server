/*******************************************************************************
* Copyright (c) 2010 Richard Backhouse
* 
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
*******************************************************************************/
package org.potpie.musicserver.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public class WARModifier {
	
	private static final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
	private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
	static {
		docBuilderFactory.setNamespaceAware(true);
		try {
			transformerFactory.setAttribute("indent-number", "4");
		} catch (IllegalArgumentException e) {}
	}
	
	public WARModifier(String warFilePath, String musicPath, String dbPath) {
		File warFile = new File(warFilePath);
		if (!warFile.exists()) {
			System.out.println("Unable to find ["+warFilePath+"]");
			return;
		} 
		try {
			modifyWAR(warFile, musicPath, dbPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void modifyWAR(File warFile, String musicPath, String dbPath) throws IOException {
		JarInputStream war = null;
		JarOutputStream os = null;
		OutputStream webXml =null;
		byte[] buffer = new byte[4096]; 
		try {
			war = new JarInputStream(new FileInputStream(warFile));
			os = new JarOutputStream(new FileOutputStream("modified-"+warFile.getName()));
			JarEntry entry = null;
			while ((entry = war.getNextJarEntry()) != null) {
				if (entry.getName().equals("WEB-INF/web.xml")) {
					File webXmlFile = new File("web.xml");
					webXml = new BufferedOutputStream(new FileOutputStream(webXmlFile)); 
					int len = 0; 
					while ((len = war.read(buffer, 0, 4096)) > 0) { 
						webXml.write(buffer, 0, len); 
					}
					webXml.close();
					Document webXmlDoc = modifyWebXml(webXmlFile, musicPath, dbPath);
					webXmlFile.deleteOnExit();
					os.putNextEntry(new JarEntry(entry.getName()));
					Source source = new DOMSource(webXmlDoc);
					Result result = new StreamResult(new OutputStreamWriter(os));
					
					try {
						Transformer transformer = transformerFactory.newTransformer();
						transformer.setOutputProperty(OutputKeys.INDENT, "yes");
						transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
						transformer.transform(source, result);
					} catch (TransformerConfigurationException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (TransformerException e) {
						e.printStackTrace();
					}
				} else {
					os.putNextEntry(new JarEntry(entry.getName()));
					int len = 0; 
					while ((len = war.read(buffer, 0, 4096)) > 0) { 
						os.write(buffer, 0, len); 
					}
				}
				
				os.closeEntry();
			}
		} finally {
			if (war != null) { try {war.close();}catch(IOException e) {}}
			if (webXml != null) { try {webXml.close();}catch(IOException e) {}}
			if (os != null) { try {os.close();}catch(IOException e) {}}
		}
	}
	
	private Document modifyWebXml(File webXml, String musicPath, String dbPath) {
		Document webXmlDoc = null;
		
		try {
			webXmlDoc = docBuilderFactory.newDocumentBuilder().parse(webXml);
			Element element = findContextParamValueElement(webXmlDoc.getDocumentElement(), "root");
			if (element != null) {
				System.out.println("Setting musicPath to ["+musicPath+"]");
				element.replaceChild(webXmlDoc.createTextNode(musicPath), element.getFirstChild());
			} else {
				System.out.println("Unable to set musicPath to ["+musicPath+"]");
			}
			element = findContextParamValueElement(webXmlDoc.getDocumentElement(), "storageDir");
			if (element != null) {
				System.out.println("Setting dbPath to ["+dbPath+"]");
				element.replaceChild(webXmlDoc.createTextNode(dbPath), element.getFirstChild());
			} else {
				System.out.println("Unable to set dbPath to ["+dbPath+"]");
			}
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		return webXmlDoc;
	}
	
	private Element findContextParamValueElement(Element element, String paramName) {
		NodeList nl = element.getElementsByTagName("context-param");
		for (int i = 0; i < nl.getLength(); i++) {
			Element e = (Element)nl.item(i);
			Element paramNameElement = (Element)e.getElementsByTagName("param-name").item(0);
			Element paramValueElement = (Element)e.getElementsByTagName("param-value").item(0);
			
			Text t = (Text)paramNameElement.getFirstChild();
			if (t.getData().equals(paramName)) {
				return paramValueElement;
			}
		}
		return null;
	}

	public static void main(String[] args) {
		if (args.length > 2) {
			new WARModifier(args[0], args[1], args[2]);
		} else {
			System.out.println("usage: WARModifier <warFile> <musicPath> <dbPath>");
		}
	}
}
